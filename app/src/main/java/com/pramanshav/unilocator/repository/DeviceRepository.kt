package com.pramanshav.unilocator.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.Timestamp
import com.pramanshav.unilocator.data.Device
import com.pramanshav.unilocator.data.DeviceConnection
import com.pramanshav.unilocator.data.UserDeviceCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random
import com.pramanshav.unilocator.data.ApiService
import com.pramanshav.unilocator.data.VerifyCodeRequest
import com.pramanshav.unilocator.utils.ApiClient

sealed class ConnectionResult {
    data class Success(val deviceId: String, val ownerEmail: String) : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
    object Timeout : ConnectionResult()
}

sealed class CodeFetchResult {
    data class Success(val code: UserDeviceCode) : CodeFetchResult()
    data class Error(val message: String) : CodeFetchResult()
    object NoValidCode : CodeFetchResult()
    object Timeout : CodeFetchResult()
}

class DeviceRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val apiService = ApiClient.apiService
    
    private val devicesCollection = firestore.collection("devices")
    private val connectionsCollection = firestore.collection("device_connections")
    private val userCodesCollection = firestore.collection("user_device_codes")
    
    companion object {
        private const val TAG = "DeviceRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    /**
     * 🎯 PRODUCTION-READY CODE FETCHER
     * Fetches the latest active & valid pairing code from Firestore
     * Handles both user-generated and other-user codes
     */
    suspend fun fetchLatestValidCode(targetCode: String? = null): CodeFetchResult {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.util.Log.e(TAG, "❌ User not authenticated")
            return CodeFetchResult.Error("User not authenticated")
        }

        android.util.Log.d(TAG, "🔍 Fetching latest valid code for user: ${currentUser.uid}")

        return try {
            withTimeoutOrNull(25000L) { // 25 second timeout
                fetchCodeWithRetry(targetCode, currentUser.uid)
            } ?: run {
                android.util.Log.e(TAG, "❌ Code fetch timeout after 25 seconds")
                CodeFetchResult.Timeout
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "💥 Code fetch failed", e)
            CodeFetchResult.Error("Fetch failed: ${e.message}")
        }
    }

    /**
     * 🔄 RETRY LOGIC for network resilience
     */
    private suspend fun fetchCodeWithRetry(targetCode: String?, userId: String): CodeFetchResult {
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                android.util.Log.d(TAG, "🔄 Attempt ${attempt + 1}/$MAX_RETRY_ATTEMPTS")
                
                val result = if (targetCode != null) {
                    fetchSpecificCode(targetCode, userId)
                } else {
                    fetchLatestActiveCode(userId)
                }
                
                // If successful or definitively failed, return
                if (result !is CodeFetchResult.Error || attempt == MAX_RETRY_ATTEMPTS - 1) {
                    return result
                }
                
                // Wait before retry
                android.util.Log.w(TAG, "⏳ Retrying in ${RETRY_DELAY_MS}ms...")
                delay(RETRY_DELAY_MS * (attempt + 1)) // Exponential backoff
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "🔥 Attempt ${attempt + 1} failed", e)
                if (attempt == MAX_RETRY_ATTEMPTS - 1) {
                    return CodeFetchResult.Error("All retry attempts failed: ${e.message}")
                }
            }
        }
        
        return CodeFetchResult.Error("Unexpected retry loop exit")
    }

    /**
     * 🎯 FETCH SPECIFIC CODE by device code string
     */
    private suspend fun fetchSpecificCode(deviceCode: String, userId: String): CodeFetchResult = withContext(Dispatchers.IO) {
        android.util.Log.d(TAG, "🔍 Searching for specific code: $deviceCode")
        
        val now = Timestamp.now()
        
        val queryResult = withTimeoutOrNull(15000L) {
            userCodesCollection
                .whereEqualTo("deviceCode", deviceCode)
                .whereEqualTo("isActive", true)
                .whereGreaterThan("expiresAt", now)
                .get()
                .await()
        }
        
        if (queryResult == null) {
            android.util.Log.e(TAG, "❌ Specific code query timeout")
            return@withContext CodeFetchResult.Timeout
        }
        
        if (queryResult.isEmpty) {
            android.util.Log.w(TAG, "❌ Specific code not found or expired: $deviceCode")
            return@withContext CodeFetchResult.NoValidCode
        }
        
        val doc = queryResult.documents.first()
        val code = doc.toObject<UserDeviceCode>()
        
        if (code == null) {
            android.util.Log.e(TAG, "❌ Failed to parse code document")
            return@withContext CodeFetchResult.Error("Failed to parse code")
        }
        
        android.util.Log.d(TAG, "✅ Found specific code: ${code.deviceCode} by user: ${code.userId}")
        
        // Check if it's the user's own code vs another user's code
        if (code.userId == userId) {
            android.util.Log.d(TAG, "📱 This is user's own code")
        } else {
            android.util.Log.d(TAG, "🤝 This is another user's code - can be used for pairing")
        }
        
        return@withContext CodeFetchResult.Success(code)
    }

    /**
     * 📊 FETCH LATEST ACTIVE CODE (any user)
     */
    private suspend fun fetchLatestActiveCode(userId: String): CodeFetchResult = withContext(Dispatchers.IO) {
        android.util.Log.d(TAG, "📊 Fetching latest active code from any user")
        
        val now = Timestamp.now()
        
        val queryResult = withTimeoutOrNull(15000L) {
            userCodesCollection
                .whereEqualTo("isActive", true)
                .whereGreaterThan("expiresAt", now)
                .orderBy("expiresAt") // Required for whereGreaterThan
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(10) // Get more to filter
                .get()
                .await()
        }
        
        if (queryResult == null) {
            android.util.Log.e(TAG, "❌ Latest code query timeout")
            return@withContext CodeFetchResult.Timeout
        }
        
        if (queryResult.isEmpty) {
            android.util.Log.w(TAG, "❌ No valid codes found")
            return@withContext CodeFetchResult.NoValidCode
        }
        
        // Filter and prioritize codes from other users (for pairing)
        val otherUserCodes = queryResult.documents.mapNotNull { doc ->
            doc.toObject<UserDeviceCode>()?.takeIf { it.userId != userId }
        }
        
        val ownCodes = queryResult.documents.mapNotNull { doc ->
            doc.toObject<UserDeviceCode>()?.takeIf { it.userId == userId }
        }
        
        val selectedCode = otherUserCodes.firstOrNull() ?: ownCodes.firstOrNull()
        
        if (selectedCode == null) {
            android.util.Log.w(TAG, "❌ No parseable codes found")
            return@withContext CodeFetchResult.NoValidCode
        }
        
        android.util.Log.d(TAG, "✅ Selected code: ${selectedCode.deviceCode}")
        android.util.Log.d(TAG, "📧 Code owner: ${selectedCode.userEmail}")
        android.util.Log.d(TAG, "⏰ Expires at: ${selectedCode.expiresAt}")
        
        if (selectedCode.userId == userId) {
            android.util.Log.d(TAG, "📱 Selected user's own code")
        } else {
            android.util.Log.d(TAG, "🤝 Selected another user's code - ready for pairing")
        }
        
        return@withContext CodeFetchResult.Success(selectedCode)
    }

    /**
     * 🚨 EMERGENCY TEST - Direct Firebase read without timeouts
     * This will tell us if Firebase works at all in your environment
     */
    suspend fun emergencyFirebaseTest(deviceCode: String): String {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return "❌ ERROR: User not authenticated"
            }
            
            android.util.Log.d(TAG, "🚨 EMERGENCY TEST: Starting direct Firebase read")
            android.util.Log.d(TAG, "🔍 Looking for code: $deviceCode")
            android.util.Log.d(TAG, "👤 Current user: ${currentUser.uid}")
            android.util.Log.d(TAG, "📧 User email: ${currentUser.email}")
            
            // Step 1: Test basic Firebase connection
            android.util.Log.d(TAG, "📡 Testing Firebase connection...")
            val testQuery = userCodesCollection.limit(1).get().await()
            android.util.Log.d(TAG, "✅ Firebase connection works - found ${testQuery.size()} documents")
            
            // Step 2: Test specific code query
            android.util.Log.d(TAG, "🔍 Searching for specific code: $deviceCode")
            val codeQuery = userCodesCollection
                .whereEqualTo("deviceCode", deviceCode)
                .get()
                .await()
            
            android.util.Log.d(TAG, "📋 Code query returned ${codeQuery.size()} documents")
            
            if (codeQuery.isEmpty) {
                android.util.Log.w(TAG, "⚠️ No documents found for code: $deviceCode")
                return "⚠️ Code not found: $deviceCode"
            }
            
            val doc = codeQuery.documents.first()
            val data = doc.data
            
            android.util.Log.d(TAG, "📄 Document data: $data")
            
            val isActive = data?.get("isActive") as? Boolean ?: false
            val userId = data?.get("userId") as? String ?: "unknown"
            val userEmail = data?.get("userEmail") as? String ?: "unknown"
            
            android.util.Log.d(TAG, "✅ SUCCESS: Code found!")
            android.util.Log.d(TAG, "🔹 isActive: $isActive")
            android.util.Log.d(TAG, "🔹 userId: $userId")
            android.util.Log.d(TAG, "🔹 userEmail: $userEmail")
            
            return "✅ SUCCESS: Found code $deviceCode from $userEmail (active: $isActive)"
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "💥 EMERGENCY TEST FAILED", e)
            return "❌ FAILED: ${e.message}"
        }
    }
    
    /**
     * 🧪 Simple test you can call from anywhere
     */
    suspend fun quickFirebaseTest(): String {
        return try {
            android.util.Log.d(TAG, "🧪 Quick Firebase test starting...")
            
            val user = auth.currentUser
            if (user == null) {
                return "❌ Not authenticated"
            }
            
            // Just try to read any document
            val result = userCodesCollection.limit(1).get().await()
            
            android.util.Log.d(TAG, "📊 Found ${result.size()} documents")
            
            if (result.isEmpty) {
                return "⚠️ No documents in collection"
            }
            
            val doc = result.documents.first()
            val deviceCode = doc.data?.get("deviceCode") as? String ?: "unknown"
            
            return "✅ Firebase works! Sample code: $deviceCode"
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "💥 Quick test failed", e)
            return "❌ Firebase error: ${e.message}"
        }
    }
    
    suspend fun getUserDevices(): Flow<List<Device>> = flow {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                // Get devices owned by user
                val ownedDevices = devicesCollection
                    .whereEqualTo("ownerId", currentUser.uid)
                    .get()
                    .await()
                    .toObjects(Device::class.java)
                
                // Get devices connected to user
                val connections = connectionsCollection
                    .whereEqualTo("connectedUserId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                
                val connectedDevices = mutableListOf<Device>()
                for (connection in connections) {
                    val connectionData = connection.toObject<DeviceConnection>()
                    val device = devicesCollection
                        .whereEqualTo("deviceCode", connectionData.deviceCode)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                        ?.toObject<Device>()
                    
                    device?.let { connectedDevices.add(it) }
                }
                
                emit(ownedDevices + connectedDevices)
            } catch (e: Exception) {
                emit(emptyList())
            }
        } else {
            emit(emptyList())
        }
    }
    
    suspend fun generateUserDeviceCode(): UserDeviceCode? {
        val currentUser = auth.currentUser ?: return null
        
        try {
            return withTimeoutOrNull(20000L) { // 20 second timeout
                generateUserDeviceCodeDirect(currentUser)
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepository", "Code generation failed: ${e.message}", e)
            return null
        }
    }
    
    private suspend fun generateUserDeviceCodeDirect(currentUser: com.google.firebase.auth.FirebaseUser): UserDeviceCode? {
        // Generate a unique 8-digit code in XXXX-XXXX format
        val deviceCode = generateUniqueDeviceCode()
        val qrCodeData = "unilocator://connect?code=$deviceCode&user=${currentUser.uid}"
        
        val userDeviceCode = UserDeviceCode(
            userId = currentUser.uid,
            userEmail = currentUser.email ?: "",
            deviceCode = deviceCode,
            qrCodeData = qrCodeData,
            generatedAt = Date(),
            expiresAt = Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000), // 24 hours
            isActive = true
        )
        
        // Deactivate previous codes with timeout
        withTimeoutOrNull(8000L) {
            deactivatePreviousUserCodes(currentUser.uid)
        }
        
        // Save new code with timeout
        val docRef = withTimeoutOrNull(10000L) {
            userCodesCollection.add(userDeviceCode).await()
        } ?: return null
        
        return userDeviceCode.copy(id = docRef.id)
    }
    
    suspend fun connectByCode(deviceCode: String): ConnectionResult {
        val currentUser = auth.currentUser ?: return ConnectionResult.Error("User not authenticated")
        
        android.util.Log.d(TAG, "🔗 Starting connection process for code: $deviceCode")
        
        try {
            // Step 1: Fetch and validate the code using our robust fetcher
            val codeResult = fetchLatestValidCode(deviceCode)
            
            val userDeviceCode = when (codeResult) {
                is CodeFetchResult.Success -> codeResult.code
                is CodeFetchResult.Error -> return ConnectionResult.Error("Code validation failed: ${codeResult.message}")
                is CodeFetchResult.NoValidCode -> return ConnectionResult.Error("Invalid or expired device code")
                is CodeFetchResult.Timeout -> return ConnectionResult.Timeout
            }
            
            // Step 2: Prevent self-connection
            if (userDeviceCode.userId == currentUser.uid) {
                android.util.Log.e(TAG, "❌ Cannot connect to your own device")
                return ConnectionResult.Error("Cannot connect to your own device")
            }
            
            android.util.Log.d(TAG, "✅ Code validation successful - proceeding with connection")
            
            // Step 3: Check for existing connection
            val existingConnection = withTimeoutOrNull(10000L) {
                connectionsCollection
                    .whereEqualTo("deviceCode", deviceCode)
                    .whereEqualTo("connectedUserId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
            }
            
            if (existingConnection == null) {
                android.util.Log.e(TAG, "❌ Existing connection check timeout")
                return ConnectionResult.Timeout
            }
            
            if (!existingConnection.isEmpty) {
                android.util.Log.w(TAG, "⚠️ Already connected to this device")
                return ConnectionResult.Error("Already connected to this device")
            }
            
            // Step 4: Create the connection
            android.util.Log.d(TAG, "🔗 Creating new connection...")
            
            val connection = DeviceConnection(
                deviceCode = deviceCode,
                ownerId = userDeviceCode.userId,
                connectedUserId = currentUser.uid,
                connectedUserEmail = currentUser.email ?: "",
                deviceName = "Connected Device",
                connectionType = "MANUAL_CODE",
                connectedAt = Date(),
                isActive = true
            )
            
            val connectionResult = withTimeoutOrNull(12000L) {
                connectionsCollection.add(connection).await()
            }
            
            if (connectionResult == null) {
                android.util.Log.e(TAG, "❌ Connection creation timeout")
                return ConnectionResult.Timeout
            }
            
            android.util.Log.d(TAG, "🎉 Connection created successfully!")
            return ConnectionResult.Success("device_connected", userDeviceCode.userEmail)
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "💥 Connection failed", e)
            return ConnectionResult.Error("Connection error: ${e.message}")
        }
    }
    
    private suspend fun connectByCodeDirect(deviceCode: String): Boolean = withContext(Dispatchers.IO) {
        val currentUser = auth.currentUser ?: return@withContext false
        
        try {
            android.util.Log.d("DeviceRepo", "🔍 Looking for device code: $deviceCode")
            
            // Step 1: Find the device code with timeout protection
            val userCodeQuery = withTimeoutOrNull(10000L) { // 10 second timeout for this query
                userCodesCollection
                    .whereEqualTo("deviceCode", deviceCode)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
            }
            
            if (userCodeQuery == null) {
                android.util.Log.e("DeviceRepo", "❌ Query timeout - Firebase read took too long")
                return@withContext false
            }
            
            android.util.Log.d("DeviceRepo", "📋 Query returned ${userCodeQuery.size()} documents")
            
            if (userCodeQuery.isEmpty) {
                android.util.Log.e("DeviceRepo", "❌ Device code not found or inactive")
                return@withContext false
            }
            
            val userCodeDoc = userCodeQuery.documents.first()
            val userDeviceCode = userCodeDoc.toObject<UserDeviceCode>()
            
            if (userDeviceCode == null) {
                android.util.Log.e("DeviceRepo", "❌ Failed to parse user device code")
                return@withContext false
            }
            
            android.util.Log.d("DeviceRepo", "✅ Found device code owned by: ${userDeviceCode.userId}")
            
            // Step 2: Validate the code
            val now = Date()
            if (userDeviceCode.expiresAt?.before(now) == true) {
                android.util.Log.e("DeviceRepo", "❌ Device code has expired")
                return@withContext false
            }
            
            // Step 3: Prevent self-connection
            if (userDeviceCode.userId == currentUser.uid) {
                android.util.Log.e("DeviceRepo", "❌ Cannot connect to your own device")
                return@withContext false
            }
            
            // Step 4: Check for existing connection with timeout
            val existingConnection = withTimeoutOrNull(8000L) { // 8 second timeout
                connectionsCollection
                    .whereEqualTo("deviceCode", deviceCode)
                    .whereEqualTo("connectedUserId", currentUser.uid)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
            }
            
            if (existingConnection == null) {
                android.util.Log.e("DeviceRepo", "❌ Existing connection check timeout")
                return@withContext false
            }
            
            if (!existingConnection.isEmpty) {
                android.util.Log.w("DeviceRepo", "⚠️ Already connected to this device")
                return@withContext false
            }
            
            // Step 5: Create the connection with timeout
            android.util.Log.d("DeviceRepo", "🔗 Creating connection...")
            
            val connection = DeviceConnection(
                deviceCode = deviceCode,
                ownerId = userDeviceCode.userId,
                connectedUserId = currentUser.uid,
                connectedUserEmail = currentUser.email ?: "",
                deviceName = "Connected Device",
                connectionType = "MANUAL_CODE",
                connectedAt = Date(),
                isActive = true
            )
            
            val connectionResult = withTimeoutOrNull(10000L) { // 10 second timeout for creation
                connectionsCollection.add(connection).await()
            }
            
            if (connectionResult == null) {
                android.util.Log.e("DeviceRepo", "❌ Connection creation timeout")
                return@withContext false
            }
            
            android.util.Log.d("DeviceRepo", "🎉 Connection created successfully!")
            return@withContext true
            
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepo", "💥 Connection failed with error", e)
            return@withContext false
        }
    }
    
    suspend fun connectByQR(qrData: String): Boolean {
        val currentUser = auth.currentUser ?: return false
        
        try {
            return withTimeoutOrNull(25000L) { // 25 second total timeout
                connectByQRDirect(qrData, currentUser)
            } ?: run {
                android.util.Log.e("DeviceRepository", "❌ QR connection timeout")
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceRepository", "QR connection failed: ${e.message}", e)
            return false
        }
    }
    
    private suspend fun connectByQRDirect(qrData: String, currentUser: com.google.firebase.auth.FirebaseUser): Boolean {
        // Parse QR code data
        if (!qrData.startsWith("unilocator://connect?")) return false
        
        val uri = android.net.Uri.parse(qrData)
        val deviceCode = uri.getQueryParameter("code") ?: return false
        val ownerId = uri.getQueryParameter("user") ?: return false
        
        // Don't allow self-connection
        if (ownerId == currentUser.uid) return false
        
        // Find the user device code with timeout
        val userCodeQuery = withTimeoutOrNull(10000L) {
            userCodesCollection
                .whereEqualTo("deviceCode", deviceCode)
                .whereEqualTo("userId", ownerId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
        } ?: return false
        
        if (userCodeQuery.isEmpty) return false
        
        val userDeviceCode = userCodeQuery.documents.first().toObject<UserDeviceCode>() ?: return false
        
        // Check if code is not expired
        val now = Date()
        if (userDeviceCode.expiresAt?.before(now) == true) return false
        
        // Check if already connected with timeout
        val existingConnection = withTimeoutOrNull(8000L) {
            connectionsCollection
                .whereEqualTo("deviceCode", deviceCode)
                .whereEqualTo("connectedUserId", currentUser.uid)
                .whereEqualTo("isActive", true)
                .get()
                .await()
        } ?: return false
        
        if (!existingConnection.isEmpty) return false
        
        // Create connection with timeout
        val connection = DeviceConnection(
            deviceCode = deviceCode,
            ownerId = ownerId,
            connectedUserId = currentUser.uid,
            connectedUserEmail = currentUser.email ?: "",
            deviceName = "Connected Device",
            connectionType = "QR_CODE",
            connectedAt = Date(),
            isActive = true
        )
        
        return withTimeoutOrNull(10000L) {
            connectionsCollection.add(connection).await()
            true
        } ?: false
    }
    
    private suspend fun generateUniqueDeviceCode(): String {
        var code: String
        do {
            // Generate web app format code: ABC4-5EF7 (letters + numbers with hyphen)
            code = generateWebAppFormatCode()
            
            // Check uniqueness with timeout
            val existing = withTimeoutOrNull(8000L) {
                userCodesCollection
                    .whereEqualTo("deviceCode", code)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
            }
            
            // If timeout, generate a new code to be safe
            if (existing == null) {
                android.util.Log.w("DeviceRepo", "Uniqueness check timeout for code: $code, generating new one")
                continue
            }
            
            // If unique, break the loop
            if (existing.isEmpty) break
            
        } while (true)
        
        return code
    }
    
    private suspend fun deactivatePreviousUserCodes(userId: String) {
        try {
            val previousCodes = withTimeoutOrNull(10000L) {
                userCodesCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
            }
            
            if (previousCodes != null) {
                for (doc in previousCodes.documents) {
                    withTimeoutOrNull(5000L) {
                        doc.reference.update("isActive", false).await()
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error silently but log it
            android.util.Log.w("DeviceRepo", "Failed to deactivate previous codes: ${e.message}")
        }
    }
    
    /**
     * 🎲 GENERATE WEB APP FORMAT CODE
     * Creates codes like: ABC4-5EF7 (capital letters + numbers with hyphen)
     */
    private fun generateWebAppFormatCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val firstPart = StringBuilder()
        val secondPart = StringBuilder()
        
        // Generate first part (4 characters)
        repeat(4) {
            firstPart.append(chars[Random.nextInt(chars.length)])
        }
        
        // Generate second part (4 characters)
        repeat(4) {
            secondPart.append(chars[Random.nextInt(chars.length)])
        }
        
        return "$firstPart-$secondPart"
    }
}

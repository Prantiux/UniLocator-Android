package com.pramanshav.unilocator.utils

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * 📱 DEVICE REGISTRATION MANAGER
 * 
 * Handles automatic device registration with Firebase when users log in
 * Enhanced with security features and validation
 */
class DeviceRegistrationManager private constructor() {
    
    companion object {
        private const val TAG = "DeviceRegistration"
        private const val COLLECTION_USER_DEVICES = "user_devices"
        
        @Volatile
        private var INSTANCE: DeviceRegistrationManager? = null
        
        fun getInstance(): DeviceRegistrationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceRegistrationManager().also { INSTANCE = it }
            }
        }
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * 🚀 REGISTER CURRENT DEVICE
     * 
     * Call this when user logs in successfully
     * Enhanced with security validation
     */
    suspend fun registerCurrentDevice(context: Context): RegisterResult {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                SecureLogger.e(TAG, "No authenticated user found")
                return RegisterResult.Error("User not authenticated")
            }
            
            SecureLogger.secureSuccess(TAG, "Device registration initiated", currentUser.uid)
            
            // Generate device information
            val deviceIdGenerator = DeviceIdGenerator.getInstance()
            val deviceInfo = deviceIdGenerator.getDeviceInfo(context)
            val deviceData = createDeviceData(context, deviceInfo, currentUser.uid)
            
            // Validate data before sending to Firebase
            if (!SecurityValidator.validateFirebaseOperation("device_registration", deviceData)) {
                SecureLogger.secureError(TAG, "Device registration", "Security validation failed")
                return RegisterResult.Error("Security validation failed")
            }
            
            // Store in Firebase
            firestore.collection(COLLECTION_USER_DEVICES)
                .document(deviceInfo.primaryDeviceId)
                .set(deviceData, SetOptions.merge())
                .await()
            
            SecureLogger.secureSuccess(TAG, "Device registered", deviceInfo.primaryDeviceId)
            RegisterResult.Success(deviceInfo.primaryDeviceId)
            
        } catch (e: Exception) {
            SecureLogger.secureError(TAG, "Device registration", e.message ?: "Unknown error", e)
            RegisterResult.Error("Registration failed: ${e.message}")
        }
    }
    
    /**
     * 📋 CREATE DEVICE DATA FOR FIREBASE
     */
    private fun createDeviceData(
        context: Context, 
        deviceInfo: DeviceIdGenerator.DeviceInfo, 
        userId: String
    ): Map<String, Any> {
        return mapOf(
            "deviceId" to deviceInfo.primaryDeviceId,
            "userId" to userId,
            "deviceType" to "android",
            "deviceName" to getDeviceName(),
            "deviceModel" to Build.MODEL,
            "androidVersion" to Build.VERSION.RELEASE,
            "appVersion" to getAppVersion(context),
            "lastSeenAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "registeredAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "isActive" to true,
            "deviceInfo" to mapOf(
                "androidId" to deviceInfo.androidId,
                "customUuid" to deviceInfo.customUuid,
                "manufacturer" to Build.MANUFACTURER,
                "brand" to Build.BRAND,
                "product" to Build.PRODUCT
            )
        )
    }
    
    /**
     * 📱 GET DEVICE NAME
     */
    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
        }
        val model = Build.MODEL
        
        return if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }
    
    /**
     * 📦 GET APP VERSION
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * 🔄 UPDATE LAST SEEN
     * 
     * Call this periodically to show device is active
     */
    suspend fun updateLastSeen(context: Context): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val deviceId = DeviceIdGenerator.getInstance().getPrimaryDeviceId(context)
            
            firestore.collection(COLLECTION_USER_DEVICES)
                .document(deviceId)
                .update("lastSeenAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
                .await()
            
            SecureLogger.secureSuccess(TAG, "update last seen", deviceId)
            true
        } catch (e: Exception) {
            SecureLogger.e(TAG, "❌ Failed to update last seen: ${e.message}")
            false
        }
    }
    
    /**
     * 📱 GET USER'S DEVICES
     * 
     * Retrieve all devices for the current user
     */
    suspend fun getUserDevices(context: Context): List<UserDevice> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                SecureLogger.e(TAG, "❌ No authenticated user found")
                return emptyList()
            }
            
            val snapshot = firestore.collection(COLLECTION_USER_DEVICES)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val currentDeviceId = DeviceIdGenerator.getInstance().getPrimaryDeviceId(context)
            
            val devices = snapshot.documents.mapNotNull { doc ->
                try {
                    UserDevice(
                        deviceId = doc.getString("deviceId") ?: "",
                        deviceName = doc.getString("deviceName") ?: "Unknown Device",
                        deviceModel = doc.getString("deviceModel") ?: "Unknown",
                        lastSeenAt = doc.getTimestamp("lastSeenAt"),
                        registeredAt = doc.getTimestamp("registeredAt"),
                        androidVersion = doc.getString("androidVersion") ?: "Unknown",
                        isCurrentDevice = doc.getString("deviceId") == currentDeviceId
                    )
                } catch (e: Exception) {
                    SecureLogger.e(TAG, "❌ Error parsing device document: ${e.message}")
                    null
                }
            }
            
            SecureLogger.d(TAG, "📱 Found ${devices.size} devices for user")
            devices
            
        } catch (e: Exception) {
            SecureLogger.e(TAG, "❌ Failed to get user devices: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * ✏️ UPDATE DEVICE NAME
     * 
     * Updates the display name of a device in Firebase
     * Enhanced with security validation
     */
    suspend fun updateDeviceName(deviceId: String, newName: String): Boolean {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                SecureLogger.e(TAG, "No authenticated user found")
                return false
            }
            
            // Validate device ID and name
            if (!SecurityValidator.isValidDeviceId(deviceId)) {
                SecureLogger.secureError(TAG, "Device name update", "Invalid device ID format")
                return false
            }
            
            if (!SecurityValidator.isValidDeviceName(newName)) {
                SecureLogger.secureError(TAG, "Device name update", "Invalid device name format")
                return false
            }
            
            val sanitizedName = SecurityValidator.sanitizeInput(newName)
            if (sanitizedName.isBlank()) {
                SecureLogger.secureError(TAG, "Device name update", "Device name cannot be empty after sanitization")
                return false
            }
            
            SecureLogger.secureSuccess(TAG, "Device name update initiated", deviceId)
            
            val updateData = mapOf(
                "deviceName" to sanitizedName,
                "lastSeenAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            // Validate update data
            if (!SecurityValidator.validateFirebaseOperation("device_name_update", updateData)) {
                SecureLogger.secureError(TAG, "Device name update", "Security validation failed")
                return false
            }
            
            firestore.collection(COLLECTION_USER_DEVICES)
                .document(deviceId)
                .update(updateData)
                .await()
            
            SecureLogger.secureSuccess(TAG, "Device name updated", deviceId)
            true
            
        } catch (e: Exception) {
            SecureLogger.secureError(TAG, "Device name update", e.message ?: "Unknown error", e)
            false
        }
    }

    /**
     * 🧪 EMERGENCY DEVICE TEST
     * 
     * Test device registration functionality
     */
    suspend fun emergencyDeviceTest(context: Context): String {
        return try {
            SecureLogger.d(TAG, "🧪 Starting emergency device test...")
            
            // Test device ID generation
            val deviceIdGenerator = DeviceIdGenerator.getInstance()
            val deviceInfo = deviceIdGenerator.getDeviceInfo(context)
            SecureLogger.secureSuccess(TAG, "device ID generation", deviceInfo.primaryDeviceId)
            
            // Test user authentication
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return "❌ FAILED: No authenticated user found"
            }
            SecureLogger.d(TAG, "✅ User authenticated: ${currentUser.uid}")
            
            // Test device registration
            val registerResult = registerCurrentDevice(context)
            when (registerResult) {
                is RegisterResult.Success -> {
                    SecureLogger.d(TAG, "✅ Device registration successful")
                }
                is RegisterResult.Error -> {
                    SecureLogger.e(TAG, "❌ Device registration failed: ${registerResult.message}")
                    return "❌ FAILED: ${registerResult.message}"
                }
            }
            
            // Test fetching user devices
            val devices = getUserDevices(context)
            SecureLogger.d(TAG, "✅ Found ${devices.size} user devices")
            
            // Prepare result summary
            val devicesList = devices.joinToString("\n") { device ->
                val current = if (device.isCurrentDevice) " (THIS DEVICE)" else ""
                "- ${device.deviceName}$current"
            }
            
            """✅ SUCCESS: Device registration test passed!
            
📱 Current Device: ${deviceInfo.primaryDeviceId.take(15)}...
👤 User: ${currentUser.email}
🔢 Total Devices: ${devices.size}

📋 Your Devices:
$devicesList

✅ All device registration functionality working!"""
            
        } catch (e: Exception) {
            SecureLogger.e(TAG, "❌ Emergency device test failed: ${e.message}", e)
            "❌ FAILED: Emergency device test failed: ${e.message}"
        }
    }
    
    /**
     * 📊 RESULT CLASSES
     */
    sealed class RegisterResult {
        data class Success(val deviceId: String) : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }
    
    data class UserDevice(
        val deviceId: String,
        val deviceName: String,
        val deviceModel: String,
        val lastSeenAt: Timestamp?,
        val registeredAt: Timestamp?,
        val androidVersion: String,
        val isCurrentDevice: Boolean
    )
}

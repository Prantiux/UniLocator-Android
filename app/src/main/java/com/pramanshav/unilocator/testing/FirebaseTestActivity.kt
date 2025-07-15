package com.pramanshav.unilocator.testing

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pramanshav.unilocator.repository.DeviceRepository
import com.pramanshav.unilocator.utils.DeviceRegistrationManager
import com.pramanshav.unilocator.utils.DeviceIdGenerator
import kotlinx.coroutines.launch

/**
 * 🧪 FIREBASE TEST ACTIVITY
 * 
 * This will test if Firebase reads work with the new rules
 */
class FirebaseTestActivity : AppCompatActivity() {
    
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var resultText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("FirebaseTestActivity", "🧪 Firebase Test Activity Created!")
        
        deviceRepository = DeviceRepository()
        
        // Create simple UI
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        val title = TextView(this).apply {
            text = "🧪 Firebase Connection Test"
            textSize = 18f
            setPadding(20, 20, 20, 40)
        }
        
        resultText = TextView(this).apply {
            text = "Ready to test Firebase...\nMake sure you're logged in!"
            textSize = 14f
            setPadding(20, 20, 20, 20)
            setBackgroundColor(0xFFEEEEEE.toInt())
        }
        
        val testBasicButton = Button(this).apply {
            text = "🔗 Test Firebase Connection"
            setOnClickListener { testBasicFirebase() }
        }
        
        val testCodeButton = Button(this).apply {
            text = "🔍 Test Code: CXO1-E6VM"
            setOnClickListener { testSpecificCode() }
        }
        
        val testLatestButton = Button(this).apply {
            text = "📊 Test Latest Code"
            setOnClickListener { testLatestCode() }
        }
        
        val testDeviceButton = Button(this).apply {
            text = "📱 Test Device Registration"
            setOnClickListener { testDeviceRegistration() }
        }
        
        layout.addView(title)
        layout.addView(testBasicButton)
        layout.addView(testCodeButton)
        layout.addView(testLatestButton)
        layout.addView(testDeviceButton)
        layout.addView(resultText)
        
        setContentView(layout)
        
        // Auto-start basic test
        testBasicFirebase()
    }
    
    private fun testBasicFirebase() {
        updateResult("🔄 Testing basic Firebase connection...")
        
        lifecycleScope.launch {
            try {
                val result = deviceRepository.quickFirebaseTest()
                updateResult("📊 Basic Firebase Test:\n$result")
                Log.d("FirebaseTest", "Basic test result: $result")
            } catch (e: Exception) {
                val error = "❌ Basic test failed: ${e.message}"
                updateResult(error)
                Log.e("FirebaseTest", "Basic test error", e)
            }
        }
    }
    
    private fun testSpecificCode() {
        updateResult("🔍 Testing specific code: CXO1-E6VM...")
        
        lifecycleScope.launch {
            try {
                val result = deviceRepository.emergencyFirebaseTest("CXO1-E6VM")
                updateResult("🔍 Specific Code Test:\n$result")
                Log.d("FirebaseTest", "Code test result: $result")
            } catch (e: Exception) {
                val error = "❌ Code test failed: ${e.message}"
                updateResult(error)
                Log.e("FirebaseTest", "Code test error", e)
            }
        }
    }
    
    private fun testLatestCode() {
        updateResult("📊 Testing latest valid code fetch...")
        
        lifecycleScope.launch {
            try {
                val result = deviceRepository.fetchLatestValidCode()
                val message = when (result) {
                    is com.pramanshav.unilocator.repository.CodeFetchResult.Success -> {
                        "✅ Found code: ${result.code.deviceCode}\nFrom: ${result.code.userEmail}"
                    }
                    is com.pramanshav.unilocator.repository.CodeFetchResult.Error -> {
                        "❌ Error: ${result.message}"
                    }
                    is com.pramanshav.unilocator.repository.CodeFetchResult.NoValidCode -> {
                        "⚠️ No valid codes found"
                    }
                    is com.pramanshav.unilocator.repository.CodeFetchResult.Timeout -> {
                        "⏱️ Request timed out"
                    }
                }
                updateResult("� Latest Code Test:\n$message")
                Log.d("FirebaseTest", "Latest code test: $message")
            } catch (e: Exception) {
                val error = "❌ Latest code test failed: ${e.message}"
                updateResult(error)
                Log.e("FirebaseTest", "Latest code test error", e)
            }
        }
    }
    
    private fun testDeviceRegistration() {
        updateResult("📱 Testing device registration system...")
        
        lifecycleScope.launch {
            try {
                val result = DeviceRegistrationManager.getInstance().emergencyDeviceTest(this@FirebaseTestActivity)
                updateResult("📱 Device Registration Test:\n$result")
                Log.d("FirebaseTest", "Device registration test result: $result")
            } catch (e: Exception) {
                val error = "❌ Device registration test failed: ${e.message}"
                updateResult(error)
                Log.e("FirebaseTest", "Device registration test error", e)
            }
        }
    }
    
    private fun updateResult(text: String) {
        runOnUiThread {
            resultText.text = text
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }
}

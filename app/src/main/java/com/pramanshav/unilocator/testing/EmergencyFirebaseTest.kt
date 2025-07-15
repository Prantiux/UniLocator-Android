package com.pramanshav.unilocator.testing

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pramanshav.unilocator.repository.DeviceRepository
import kotlinx.coroutines.launch

/**
 * 🚨 EMERGENCY FIREBASE TEST
 * 
 * Add this activity to your app to test Firebase directly
 * This will tell us exactly what's happening with Firebase
 */
class EmergencyFirebaseTest : AppCompatActivity() {
    
    private lateinit var deviceRepository: DeviceRepository
    private lateinit var resultText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        deviceRepository = DeviceRepository()
        
        // Create simple UI
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        
        resultText = TextView(this).apply {
            text = "Ready to test Firebase..."
            textSize = 14f
            setPadding(20, 20, 20, 20)
        }
        
        val testButton = Button(this).apply {
            text = "🧪 Test Firebase Connection"
            setOnClickListener { testFirebaseConnection() }
        }
        
        val testCodeButton = Button(this).apply {
            text = "🔍 Test Code CXO1-E6VM"
            setOnClickListener { testSpecificCode() }
        }
        
        layout.addView(testButton)
        layout.addView(testCodeButton)
        layout.addView(resultText)
        
        setContentView(layout)
    }
    
    private fun testFirebaseConnection() {
        resultText.text = "🔄 Testing Firebase connection..."
        
        lifecycleScope.launch {
            val result = deviceRepository.quickFirebaseTest()
            resultText.text = "📊 Firebase Test Result:\n$result"
        }
    }
    
    private fun testSpecificCode() {
        resultText.text = "🔍 Testing specific code CXO1-E6VM..."
        
        lifecycleScope.launch {
            val result = deviceRepository.emergencyFirebaseTest("CXO1-E6VM")
            resultText.text = "🔍 Code Test Result:\n$result"
        }
    }
}

/**
 * 🎯 HOW TO USE THIS TEST:
 * 
 * 1. Add this activity to your AndroidManifest.xml:
 *    <activity android:name=".testing.EmergencyFirebaseTest" />
 * 
 * 2. Start it from any activity:
 *    startActivity(Intent(this, EmergencyFirebaseTest::class.java))
 * 
 * 3. Or call the test methods directly:
 *    lifecycleScope.launch {
 *        val result = DeviceRepository().quickFirebaseTest()
 *        Log.d("FirebaseTest", result)
 *    }
 * 
 * 4. Check Android Studio Logcat for detailed logs with tag "DeviceRepository"
 */

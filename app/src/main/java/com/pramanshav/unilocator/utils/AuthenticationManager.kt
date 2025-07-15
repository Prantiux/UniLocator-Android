package com.pramanshav.unilocator.utils

import android.content.Context
import android.util.Log

/**
 * 🔐 AUTHENTICATION MANAGER
 * 
 * Handles login flow with automatic device registration
 */
class AuthenticationManager {
    
    companion object {
        private const val TAG = "AuthenticationManager"
    }
    
    /**
     * 🚀 HANDLE SUCCESSFUL LOGIN
     * 
     * Call this after Firebase Auth login succeeds
     */
    suspend fun onLoginSuccess(context: Context): AuthResult {
        Log.d(TAG, "🎉 Login successful, registering device...")
        
        return try {
            // Register current device
            val registrationResult = DeviceRegistrationManager.getInstance()
                .registerCurrentDevice(context)
            
            when (registrationResult) {
                is DeviceRegistrationManager.RegisterResult.Success -> {
                    Log.d(TAG, "✅ Device registered: ${registrationResult.deviceId.take(10)}...")
                    AuthResult.Success("Device registered successfully")
                }
                is DeviceRegistrationManager.RegisterResult.Error -> {
                    Log.e(TAG, "❌ Device registration failed: ${registrationResult.message}")
                    AuthResult.Error("Device registration failed: ${registrationResult.message}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in post-login setup: ${e.message}")
            AuthResult.Error("Post-login setup failed: ${e.message}")
        }
    }
    
    /**
     * 🔄 UPDATE DEVICE ACTIVITY
     * 
     * Call this periodically to show device is active
     */
    suspend fun updateDeviceActivity(context: Context): Boolean {
        return try {
            DeviceRegistrationManager.getInstance().updateLastSeen(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating device activity: ${e.message}")
            false
        }
    }
    
    /**
     * 📊 RESULT CLASSES
     */
    sealed class AuthResult {
        data class Success(val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }
}

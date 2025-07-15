package com.pramanshav.unilocator.utils

import android.content.Context
import com.pramanshav.unilocator.BuildConfig

/**
 * 🔒 SECURITY VALIDATION UTILITY
 * 
 * Provides security checks and validations for the app
 */
object SecurityValidator {
    
    /**
     * 🛡️ VALIDATE DEVICE CODE FORMAT
     * Ensures device codes match expected pattern and are safe
     */
    fun isValidDeviceCode(code: String?): Boolean {
        if (code.isNullOrBlank()) return false
        
        // Must match pattern: XXXX-XXXX (alphanumeric + hyphen)
        val pattern = Regex("^[A-Z0-9]{4}-[A-Z0-9]{4}$")
        return pattern.matches(code)
    }
    
    /**
     * 🔍 VALIDATE DEVICE ID FORMAT
     * Ensures device IDs are properly formatted
     */
    fun isValidDeviceId(deviceId: String?): Boolean {
        if (deviceId.isNullOrBlank()) return false
        
        // Device ID should be 20+ characters alphanumeric/underscore/hyphen
        return deviceId.length >= 20 && 
               deviceId.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }
    
    /**
     * 📧 VALIDATE EMAIL FORMAT
     * Basic email validation
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        
        val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        return emailPattern.matches(email)
    }
    
    /**
     * 🔐 VALIDATE DEVICE NAME
     * Ensures device names are safe and appropriate
     */
    fun isValidDeviceName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        
        // Device name should be 1-50 characters, no special chars except spaces, hyphens, underscores
        return name.length in 1..50 && 
               name.matches(Regex("^[a-zA-Z0-9 _-]+$"))
    }
    
    /**
     * 🚫 SANITIZE INPUT STRING
     * Removes potentially dangerous characters from user input
     */
    fun sanitizeInput(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        return input
            .trim()
            .replace(Regex("[<>\"'&]"), "") // Remove potential XSS characters
            .take(100) // Limit length
    }
    
    /**
     * 🔍 CHECK APP INTEGRITY
     * Validates that the app is running in a secure environment
     */
    fun checkAppIntegrity(context: Context): SecurityStatus {
        val issues = mutableListOf<String>()
        
        // Check if running in debug mode in production
        if (BuildConfig.DEBUG && BuildConfig.BUILD_TYPE == "release") {
            issues.add("Debug mode enabled in release build")
        }
        
        // Check if logging is enabled in release
        if (!BuildConfig.DEBUG && BuildConfig.ENABLE_LOGGING) {
            issues.add("Logging enabled in release build")
        }
        
        // Check if app is installed from known source (optional)
        try {
            val installer = context.packageManager.getInstallerPackageName(context.packageName)
            if (installer == null) {
                issues.add("App installed from unknown source")
            }
        } catch (e: Exception) {
            SecureLogger.w("SecurityValidator", "Could not check installer package")
        }
        
        return SecurityStatus(
            isSecure = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * 🔐 VALIDATE FIREBASE OPERATION
     * Ensures Firebase operations are safe to perform
     */
    fun validateFirebaseOperation(operation: String, data: Map<String, Any>): Boolean {
        SecureLogger.d("SecurityValidator", "Validating Firebase operation: $operation")
        
        // Check for suspicious data sizes
        data.values.forEach { value ->
            when (value) {
                is String -> {
                    if (value.length > 10000) { // 10KB limit
                        SecureLogger.w("SecurityValidator", "Large string detected in Firebase operation")
                        return false
                    }
                }
            }
        }
        
        // Check for sensitive keys that shouldn't be stored
        val sensitiveKeys = listOf("password", "secret", "private_key", "token")
        data.keys.forEach { key ->
            if (sensitiveKeys.any { key.lowercase().contains(it) }) {
                SecureLogger.e("SecurityValidator", "Sensitive data key detected: $key")
                return false
            }
        }
        
        return true
    }
    
    /**
     * 📊 SECURITY STATUS DATA CLASS
     */
    data class SecurityStatus(
        val isSecure: Boolean,
        val issues: List<String>
    )
}

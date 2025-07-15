package com.pramanshav.unilocator.utils

import android.util.Log
import com.pramanshav.unilocator.BuildConfig

/**
 * 🔒 SECURE LOGGING UTILITY
 * 
 * Provides secure logging that:
 * - Only logs in debug builds
 * - Sanitizes sensitive information
 * - Prevents logging in release builds
 */
object SecureLogger {
    
    private const val DEFAULT_TAG = "UniLocator"
    
    /**
     * 🔍 DEBUG LOG - Only in debug builds
     */
    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.ENABLE_LOGGING && BuildConfig.DEBUG) {
            Log.d(tag, sanitizeMessage(message))
        }
    }
    
    /**
     * ℹ️ INFO LOG - Only in debug builds
     */
    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.ENABLE_LOGGING && BuildConfig.DEBUG) {
            Log.i(tag, sanitizeMessage(message))
        }
    }
    
    /**
     * ⚠️ WARNING LOG - Allowed in release for important warnings
     */
    fun w(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.w(tag, sanitizeMessage(message))
        }
    }
    
    /**
     * ❌ ERROR LOG - Always allowed for crash reporting
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, sanitizeMessage(message), throwable)
    }
    
    /**
     * 🧹 SANITIZE SENSITIVE INFORMATION
     * 
     * Removes or masks sensitive data from log messages
     */
    private fun sanitizeMessage(message: String): String {
        return message
            // Hide device codes (keep first 2 and last 2 characters)
            .replace(Regex("[A-Z0-9]{4}-[A-Z0-9]{4}")) { matchResult ->
                val code = matchResult.value
                if (code.length == 9) {
                    "${code.substring(0, 2)}**-**${code.substring(7, 9)}"
                } else {
                    "****-****"
                }
            }
            // Hide device IDs (show only first 10 characters)
            .replace(Regex("deviceId\"?:\\s*\"?([a-zA-Z0-9_-]{20,})\"?")) { matchResult ->
                val deviceId = matchResult.groupValues[1]
                "deviceId: ${deviceId.take(10)}..."
            }
            // Hide user IDs
            .replace(Regex("userId\"?:\\s*\"?([a-zA-Z0-9_-]{20,})\"?")) { matchResult ->
                val userId = matchResult.groupValues[1]
                "userId: ${userId.take(8)}..."
            }
            // Hide email addresses
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")) { 
                "***@***.***" 
            }
            // Hide Firebase tokens
            .replace(Regex("\"token\":\\s*\"[^\"]{50,}\"")) {
                "\"token\": \"***REDACTED***\""
            }
    }
    
    /**
     * 🔐 SECURE SUCCESS LOG
     * Used for security-sensitive operations
     */
    fun secureSuccess(tag: String = DEFAULT_TAG, operation: String, identifier: String? = null) {
        if (BuildConfig.ENABLE_LOGGING) {
            val sanitizedId = identifier?.let { 
                if (it.length > 10) "${it.take(10)}..." else it 
            } ?: "N/A"
            i(tag, "✅ $operation completed successfully [ID: $sanitizedId]")
        }
    }
    
    /**
     * 🚨 SECURE ERROR LOG
     * Used for security-sensitive operations
     */
    fun secureError(tag: String = DEFAULT_TAG, operation: String, error: String, throwable: Throwable? = null) {
        e(tag, "❌ $operation failed: ${sanitizeMessage(error)}", throwable)
    }
    
    /**
     * 🧪 DEVELOPMENT ONLY LOG
     * Only logs in debug builds, used for temporary debugging
     */
    fun dev(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG && BuildConfig.BUILD_TYPE == "debug") {
            Log.d("DEV-$tag", sanitizeMessage(message))
        }
    }
}

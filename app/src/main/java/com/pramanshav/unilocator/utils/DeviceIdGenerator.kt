package com.pramanshav.unilocator.utils

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import java.util.*

/**
 * 🔧 DEVICE ID GENERATOR
 * 
 * Creates a unique Primary Device ID by combining:
 * - Android ID (device-specific)
 * - Custom UUID (app-specific, generated once)
 * 
 * Result: "ANDROID_ID_UUID"
 */
class DeviceIdGenerator private constructor() {
    
    companion object {
        private const val TAG = "DeviceIdGenerator"
        private const val PREF_NAME = "unilocator_device_prefs"
        private const val KEY_DEVICE_UUID = "primary_device_uuid"
        private const val KEY_DEVICE_ID = "primary_device_id"
        private const val FALLBACK_ANDROID_ID = "unknown_device"
        
        @Volatile
        private var INSTANCE: DeviceIdGenerator? = null
        
        fun getInstance(): DeviceIdGenerator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DeviceIdGenerator().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 🎯 MAIN FUNCTION: Get or Generate Primary Device ID
     * 
     * @param context Application context
     * @return Unique device ID in format "ANDROID_ID_UUID"
     */
    fun getPrimaryDeviceId(context: Context): String {
        val prefs = getSharedPreferences(context)
        
        // Check if we already have a stored device ID
        val existingDeviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (!existingDeviceId.isNullOrEmpty()) {
            Log.d(TAG, "📱 Using existing Device ID: ${existingDeviceId.take(10)}...")
            return existingDeviceId
        }
        
        // Generate new device ID
        Log.d(TAG, "🆕 Generating new Primary Device ID...")
        val newDeviceId = generateNewDeviceId(context)
        
        // Store it for future use
        prefs.edit().putString(KEY_DEVICE_ID, newDeviceId).apply()
        Log.d(TAG, "💾 Stored new Device ID: ${newDeviceId.take(10)}...")
        
        return newDeviceId
    }
    
    /**
     * 🔄 GENERATE NEW DEVICE ID
     */
    private fun generateNewDeviceId(context: Context): String {
        val androidId = getAndroidId(context)
        val customUuid = getOrCreateCustomUuid(context)
        
        val deviceId = "${androidId}_${customUuid}"
        Log.d(TAG, "🔧 Generated Device ID: Android=$androidId, UUID=${customUuid.take(8)}...")
        
        return deviceId
    }
    
    /**
     * 📱 GET ANDROID ID
     */
    private fun getAndroidId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            
            if (androidId.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ Android ID is null/empty, using fallback")
                FALLBACK_ANDROID_ID
            } else {
                Log.d(TAG, "📱 Android ID retrieved: ${androidId.take(8)}...")
                androidId
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting Android ID: ${e.message}")
            FALLBACK_ANDROID_ID
        }
    }
    
    /**
     * 🆔 GET OR CREATE CUSTOM UUID
     */
    private fun getOrCreateCustomUuid(context: Context): String {
        val prefs = getSharedPreferences(context)
        
        // Check if UUID already exists
        val existingUuid = prefs.getString(KEY_DEVICE_UUID, null)
        if (!existingUuid.isNullOrEmpty()) {
            Log.d(TAG, "🔄 Using existing UUID: ${existingUuid.take(8)}...")
            return existingUuid
        }
        
        // Generate new UUID
        val newUuid = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_UUID, newUuid).apply()
        Log.d(TAG, "🆕 Generated new UUID: ${newUuid.take(8)}...")
        
        return newUuid
    }
    
    /**
     * 🗂️ GET SHARED PREFERENCES
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 🧹 CLEAR DEVICE ID (for testing/reset)
     */
    fun clearDeviceId(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().clear().apply()
        Log.d(TAG, "🧹 Device ID cleared")
    }
    
    /**
     * 🔄 REGENERATE DEVICE ID (for testing)
     */
    fun regenerateDeviceId(context: Context): String {
        clearDeviceId(context)
        return getPrimaryDeviceId(context)
    }
    
    /**
     * ℹ️ GET DEVICE INFO (for debugging)
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        val androidId = getAndroidId(context)
        val customUuid = getOrCreateCustomUuid(context)
        val primaryId = getPrimaryDeviceId(context)
        
        return DeviceInfo(
            androidId = androidId,
            customUuid = customUuid,
            primaryDeviceId = primaryId
        )
    }
    
    /**
     * 📊 DEVICE INFO DATA CLASS
     */
    data class DeviceInfo(
        val androidId: String,
        val customUuid: String,
        val primaryDeviceId: String
    )
}

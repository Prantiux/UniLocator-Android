package com.pramanshav.unilocator.data

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class Device(
    @DocumentId
    val id: String = "",
    val deviceCode: String = "",
    val name: String = "",
    val type: String = "",
    val model: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val batteryLevel: Int? = null,
    val isOnline: Boolean = false,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastLocationName: String = "",
    @ServerTimestamp
    val lastSeen: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    val ownerId: String = "",
    val ownerEmail: String = ""
)

data class DeviceConnection(
    @DocumentId
    val id: String = "",
    val deviceCode: String = "",
    val ownerId: String = "",
    val connectedUserId: String = "",
    val connectedUserEmail: String = "",
    val deviceName: String = "",
    val connectionType: String = "QR_CODE", // QR_CODE or MANUAL_CODE
    @ServerTimestamp
    val connectedAt: Date? = null,
    val isActive: Boolean = true
)

data class UserDeviceCode(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val deviceCode: String = "",
    val qrCodeData: String = "",
    @ServerTimestamp
    val generatedAt: Date? = null,
    @ServerTimestamp
    val expiresAt: Date? = null,
    val isActive: Boolean = true
)

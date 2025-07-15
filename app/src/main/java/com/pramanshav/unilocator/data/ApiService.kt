package com.pramanshav.unilocator.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class VerifyCodeRequest(
    val device_code: String,
    val user_id: String,
    val user_email: String
)

data class VerifyCodeResponse(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null,
    val deviceId: String? = null,
    val ownerEmail: String? = null,
    val timeout: Boolean = false,
    val retry_suggested: Boolean = false
)

data class CheckCodeRequest(
    val device_code: String
)

data class CheckCodeResponse(
    val exists: Boolean,
    val message: String? = null,
    val error: String? = null
)

interface ApiService {
    @POST("devices/verify-code-mobile")
    suspend fun verifyDeviceCode(@Body request: VerifyCodeRequest): Response<VerifyCodeResponse>
    
    @POST("devices/check-code-exists")
    suspend fun checkCodeExists(@Body request: CheckCodeRequest): Response<CheckCodeResponse>
}

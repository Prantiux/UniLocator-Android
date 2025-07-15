package com.pramanshav.unilocator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pramanshav.unilocator.data.Device
import com.pramanshav.unilocator.data.UserDeviceCode
import com.pramanshav.unilocator.repository.DeviceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DevicesUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userDeviceCode: UserDeviceCode? = null,
    val isGeneratingCode: Boolean = false,
    val isConnecting: Boolean = false,
    val connectionResult: ConnectionResult? = null
)

sealed class ConnectionResult {
    object Success : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}

class DevicesViewModel : ViewModel() {
    private val repository = DeviceRepository()
    
    private val _uiState = MutableStateFlow(DevicesUiState())
    val uiState: StateFlow<DevicesUiState> = _uiState.asStateFlow()
    
    init {
        loadDevices()
    }
    
    fun loadDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                repository.getUserDevices().collect { devices ->
                    _uiState.value = _uiState.value.copy(
                        devices = devices,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load devices"
                )
            }
        }
    }
    
    fun generateUserCode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingCode = true)
            
            try {
                val userCode = repository.generateUserDeviceCode()
                _uiState.value = _uiState.value.copy(
                    userDeviceCode = userCode,
                    isGeneratingCode = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingCode = false,
                    error = "Failed to generate code: ${e.message}"
                )
            }
        }
    }
    
    fun connectByCode(deviceCode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, connectionResult = null)
            
            try {
                val result = repository.connectByCode(deviceCode)
                val viewModelResult = when (result) {
                    is com.pramanshav.unilocator.repository.ConnectionResult.Success -> ConnectionResult.Success
                    is com.pramanshav.unilocator.repository.ConnectionResult.Error -> ConnectionResult.Error(result.message)
                    is com.pramanshav.unilocator.repository.ConnectionResult.Timeout -> ConnectionResult.Error("Connection timed out")
                }
                
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    connectionResult = viewModelResult
                )
                
                if (result is com.pramanshav.unilocator.repository.ConnectionResult.Success) {
                    loadDevices() // Refresh devices list
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    connectionResult = ConnectionResult.Error(e.message ?: "Connection failed")
                )
            }
        }
    }
    
    fun connectByQR(qrData: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isConnecting = true, connectionResult = null)
            
            try {
                val success = repository.connectByQR(qrData)
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    connectionResult = if (success) {
                        ConnectionResult.Success
                    } else {
                        ConnectionResult.Error("Failed to connect. Invalid QR code.")
                    }
                )
                
                if (success) {
                    loadDevices() // Refresh devices list
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    connectionResult = ConnectionResult.Error(e.message ?: "Connection failed")
                )
            }
        }
    }
    
    fun clearConnectionResult() {
        _uiState.value = _uiState.value.copy(connectionResult = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

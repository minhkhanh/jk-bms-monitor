package com.jkbms.monitor.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jkbms.monitor.ble.BmsDevice
import com.jkbms.monitor.data.BmsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.jkbms_protocol.CellData
import uniffi.jkbms_protocol.DeviceInfo

data class BmsUiState(
    val isScanning: Boolean = false,
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val discoveredDevices: List<BmsDevice> = emptyList(),
    val connectedAddress: String? = null,
    val deviceInfo: DeviceInfo? = null,
    val cellData: CellData? = null,
    val error: String? = null
)

class BmsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BmsViewModel"
    }

    private val repository = BmsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(BmsUiState())
    val uiState: StateFlow<BmsUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var pollingJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isScanning = true,
            discoveredDevices = emptyList(),
            error = null
        )

        scanJob = viewModelScope.launch {
            try {
                val seen = mutableSetOf<String>()
                repository.scanDevices().collect { device ->
                    if (seen.add(device.address)) {
                        _uiState.value = _uiState.value.copy(
                            discoveredDevices = _uiState.value.discoveredDevices + device
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Scan error", e)
                _uiState.value = _uiState.value.copy(
                    error = "Scan failed: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    fun connect(address: String) {
        stopScan()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                repository.connect(address)
                _uiState.value = _uiState.value.copy(
                    isConnected = true,
                    isLoading = false,
                    connectedAddress = address
                )
                // Start polling data
                startPolling()
            } catch (e: Exception) {
                Log.e(TAG, "Connect error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Connect failed: ${e.message}"
                )
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        pollingJob = null
        repository.disconnect()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            connectedAddress = null,
            deviceInfo = null,
            cellData = null
        )
    }

    fun refreshData() {
        if (!repository.isConnected) return
        viewModelScope.launch {
            fetchData()
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            // Initial fetch
            fetchData()
            // Poll every 5 seconds
            while (true) {
                kotlinx.coroutines.delay(5000)
                fetchData()
            }
        }
    }

    private suspend fun fetchData() {
        try {
            val cellData = repository.getCellData()
            _uiState.value = _uiState.value.copy(cellData = cellData, error = null)
        } catch (e: Exception) {
            Log.e(TAG, "Fetch cell data error", e)
            _uiState.value = _uiState.value.copy(error = "Data error: ${e.message}")
        }

        try {
            val deviceInfo = repository.getDeviceInfo()
            _uiState.value = _uiState.value.copy(deviceInfo = deviceInfo)
        } catch (e: Exception) {
            Log.w(TAG, "Fetch device info error", e)
            // Device info is less critical, don't show error
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}

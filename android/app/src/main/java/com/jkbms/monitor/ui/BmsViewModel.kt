package com.jkbms.monitor.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jkbms.monitor.ble.BmsDevice
import com.jkbms.monitor.data.BmsRepository
import com.jkbms.monitor.worker.BmsWorkScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
    val error: String? = null,
    // Persistence
    val savedDeviceMac: String? = null,
    val savedDeviceName: String? = null,
    val lastUpdateTime: String? = null
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

    init {
        // Load saved device info and cached data
        val mac = repository.dataStore.getSelectedDeviceMac()
        val name = repository.dataStore.getSelectedDeviceName()
        val lastUpdate = if (repository.dataStore.hasCachedData())
            repository.dataStore.getLastUpdateFormatted() else null

        _uiState.value = _uiState.value.copy(
            savedDeviceMac = mac,
            savedDeviceName = name,
            lastUpdateTime = lastUpdate
        )

        // Auto-connect if a device was previously selected
        if (mac != null) {
            connect(mac, name ?: "JK BMS")
        }
    }

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

    fun connect(address: String, deviceName: String = "JK BMS") {
        stopScan()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                repository.connect(address)

                // Save selected device for auto-connect and background worker
                repository.dataStore.saveSelectedDevice(address, deviceName)

                _uiState.value = _uiState.value.copy(
                    isConnected = true,
                    isLoading = false,
                    connectedAddress = address,
                    savedDeviceMac = address,
                    savedDeviceName = deviceName
                )

                // Schedule background refresh (idempotent if already scheduled)
                BmsWorkScheduler.schedulePeriodicRefresh(getApplication())

                // Start observing data that the device emits
                startObserving()
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
        stopObserving()
        repository.disconnect()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            connectedAddress = null,
            deviceInfo = null,
            cellData = null
        )
        // NOTE: background worker keeps running for widget updates
    }

    fun refreshData() {
        if (!repository.isConnected) return
        viewModelScope.launch {
            fetchData()
        }
    }

    private var isPollingPaused = false

    fun pausePolling() {
        Log.d(TAG, "Pausing observation")
        isPollingPaused = true
    }

    fun resumePolling() {
        if (isPollingPaused) {
            Log.d(TAG, "Resuming observation")
            isPollingPaused = false
            if (_uiState.value.isConnected && (pollingJob == null || !pollingJob!!.isActive)) {
                startObserving()
            }
        }
    }

    private fun startObserving() {
        stopObserving()

        // We use pollingJob specifically to hold the observation coroutines
        pollingJob = viewModelScope.launch {
            // Send initial requests to kickstart the continuous transmission from the BMS
            if (!isPollingPaused) {
                try {
                    repository.getDeviceInfo()
                    repository.getCellData()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send initial fetch requests", e)
                }
            }

            // Launch an observer for CellData
            launch {
                repository.cellDataFlow.collect { cellData ->
                    if (isPollingPaused) return@collect
                    _uiState.value = _uiState.value.copy(
                        cellData = cellData,
                        error = null,
                        lastUpdateTime = repository.dataStore.getLastUpdateFormatted()
                    )
                }
            }

            // Launch an observer for DeviceInfo
            launch {
                repository.deviceInfoFlow.collect { deviceInfo ->
                    if (isPollingPaused) return@collect
                    _uiState.value = _uiState.value.copy(
                        deviceInfo = deviceInfo
                    )
                }
            }
        }
    }

    private fun stopObserving() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun fetchData() {
        // Send a request to trigger new data without blocking UI
        try {
            repository.getCellData()
        } catch (e: Exception) {
            Log.e(TAG, "Manual refresh failed", e)
            _uiState.value = _uiState.value.copy(error = "Refresh failed: ${e.message}")
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

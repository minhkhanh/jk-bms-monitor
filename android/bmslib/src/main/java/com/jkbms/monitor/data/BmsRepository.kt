package com.jkbms.monitor.data

import android.content.Context
import android.util.Log
import com.jkbms.monitor.ble.BleConnection
import com.jkbms.monitor.ble.BleScanner
import com.jkbms.monitor.ble.BmsDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import uniffi.jkbms_protocol.CellData
import uniffi.jkbms_protocol.DeviceInfo
import uniffi.jkbms_protocol.getDeviceInfoRequest
import uniffi.jkbms_protocol.getCellDataRequest
import uniffi.jkbms_protocol.parseCellData
import uniffi.jkbms_protocol.parseDeviceInfo

/**
 * High-level repository for BMS data operations.
 * Bridges BLE communication layer with Rust protocol parser.
 *
 * Uses a continuous frame assembler that listens to all BLE notifications,
 * reassembles 300-byte frames, and dispatches parsed results by record type.
 * This avoids the problem of missing frames when the BMS sends multiple
 * frame types (0x01, 0x02, 0x03) in a burst regardless of the request.
 */
class BmsRepository(context: Context) {

    companion object {
        private const val TAG = "BmsRepository"
        private const val DATA_TIMEOUT_MS = 10_000L

        private val RESPONSE_HEADER = byteArrayOf(
            0x55.toByte(), 0xAA.toByte(), 0xEB.toByte(), 0x90.toByte()
        )
        private const val FRAME_SIZE = 300

        private const val RECORD_TYPE_SETTINGS = 0x01
        private const val RECORD_TYPE_CELL_DATA = 0x02
        private const val RECORD_TYPE_DEVICE_INFO = 0x03
    }

    private val scanner = BleScanner(context)
    private val connection = BleConnection(context)
    val dataStore = BmsDataStore(context)

    val isBluetoothEnabled: Boolean get() = scanner.isBluetoothEnabled
    val isConnected: Boolean get() = connection.isConnected

    // --- Per-type dispatch channels (CONFLATED = keeps only the latest value) ---
    private var cellDataChannel = Channel<CellData>(Channel.CONFLATED)
    private var deviceInfoChannel = Channel<DeviceInfo>(Channel.CONFLATED)

    /**
     * Continuous stream of parsed CellData frames.
     */
    val cellDataFlow: Flow<CellData>
        get() = cellDataChannel.receiveAsFlow()

    /**
     * Continuous stream of parsed DeviceInfo frames.
     */
    val deviceInfoFlow: Flow<DeviceInfo>
        get() = deviceInfoChannel.receiveAsFlow()

    // --- Latest parsed data (in-memory cache) ---
    var latestCellData: CellData? = null
        private set
    var latestDeviceInfo: DeviceInfo? = null
        private set

    // --- Listener lifecycle ---
    private var listenerScope: CoroutineScope? = null

    fun scanDevices(): Flow<BmsDevice> = scanner.scanDevices()

    suspend fun connect(address: String) {
        connection.connect(address)
        startListening()
    }

    fun disconnect() {
        stopListening()
        connection.disconnect()
    }

    /**
     * Request cell data from the BMS.
     * Sends the command and waits for the next CellData frame dispatched by the listener.
     */
    suspend fun getCellData(): CellData {
        connection.writeCharacteristic(getCellDataRequest())
        return withTimeout(DATA_TIMEOUT_MS) {
            cellDataChannel.receive()
        }
    }

    /**
     * Request device info from the BMS.
     * Sends the command and waits for the next DeviceInfo frame dispatched by the listener.
     */
    suspend fun getDeviceInfo(): DeviceInfo {
        connection.writeCharacteristic(getDeviceInfoRequest())
        return withTimeout(DATA_TIMEOUT_MS) {
            deviceInfoChannel.receive()
        }
    }

    /**
     * Connect to saved device, fetch cell data, cache it, and disconnect.
     * Used by background worker for periodic widget updates.
     */
    suspend fun fetchAndCache(): CellData {
        val mac = dataStore.getSelectedDeviceMac()
            ?: throw Exception("No device selected")
        try {
            connect(mac)
            // Also try device info, but don't fail if it errors
            try { getDeviceInfo() } catch (_: Exception) {}
            val cellData = getCellData()
            return cellData
        } finally {
            disconnect()
        }
    }

    // =========================================================================
    // Continuous Frame Assembler + Message Dispatcher
    // =========================================================================

    /**
     * Start the background listener that continuously reads BLE notifications,
     * reassembles complete 300-byte frames, parses them, and dispatches results
     * to the appropriate per-type channels.
     */
    private fun startListening() {
        stopListening()

        // Reset channels so old data doesn't leak across connections
        cellDataChannel = Channel(Channel.CONFLATED)
        deviceInfoChannel = Channel(Channel.CONFLATED)

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        listenerScope = scope

        scope.launch {
            var buffer = ByteArray(0)

            connection.notifications.collect { chunk ->
                buffer += chunk

                while (true) {
                    val headerPos = findHeader(buffer, 0)

                    if (headerPos == null) {
                        // No header found anywhere.
                        // Discard everything except the last 3 bytes to preserve a potential partial header.
                        if (buffer.size > 3) {
                            buffer = buffer.copyOfRange(buffer.size - 3, buffer.size)
                        }
                        break // Wait for more data
                    }

                    if (headerPos > 0) {
                        // Drop garbage before the header
                        buffer = buffer.copyOfRange(headerPos, buffer.size)
                    }

                    // Now the buffer starts exactly with the header (at index 0)
                    if (buffer.size < FRAME_SIZE) {
                        break // Wait for more data
                    }

                    // We have exactly a full frame or more!
                    val frame = buffer.copyOfRange(0, FRAME_SIZE)
                    buffer = buffer.copyOfRange(FRAME_SIZE, buffer.size)

                    dispatchFrame(frame)
                }

                // Safety: prevent unbounded buffer growth in weird edge cases
                if (buffer.size > FRAME_SIZE * 4) {
                    Log.w(TAG, "Buffer overflow (${buffer.size} bytes), trimming")
                    buffer = buffer.copyOfRange(buffer.size - FRAME_SIZE * 2, buffer.size)
                }
            }
        }

        Log.d(TAG, "Listener started")
    }

    private fun stopListening() {
        listenerScope?.cancel()
        listenerScope = null
    }

    /**
     * Parse and dispatch a complete 300-byte frame by its record type.
     */
    private fun dispatchFrame(frame: ByteArray) {
        if (frame.size < 6) return

        val recordType = frame[4].toInt() and 0xFF
        Log.d(TAG, "Dispatching frame: type=0x${recordType.toString(16)}, size=${frame.size}")

        when (recordType) {
            RECORD_TYPE_CELL_DATA -> {
                try {
                    val cellData = parseCellData(frame)
                    latestCellData = cellData
                    dataStore.saveCachedCellData(cellData)
                    cellDataChannel.trySend(cellData)
                    Log.d(TAG, "CellData parsed: ${cellData.remainPercent}% ${cellData.batteryVoltage}V")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse CellData", e)
                }
            }
            RECORD_TYPE_DEVICE_INFO -> {
                try {
                    val deviceInfo = parseDeviceInfo(frame)
                    latestDeviceInfo = deviceInfo
                    dataStore.saveCachedDeviceInfo(deviceInfo)
                    deviceInfoChannel.trySend(deviceInfo)
                    Log.d(TAG, "DeviceInfo parsed: ${deviceInfo.deviceModel}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse DeviceInfo", e)
                }
            }
            RECORD_TYPE_SETTINGS -> {
                Log.d(TAG, "Settings frame received (ignored)")
            }
            else -> {
                Log.w(TAG, "Unknown record type: 0x${recordType.toString(16)}")
            }
        }
    }

    /**
     * Find the position of RESPONSE_HEADER in data starting from offset.
     */
    private fun findHeader(data: ByteArray, from: Int): Int? {
        if (data.size < RESPONSE_HEADER.size) return null
        for (i in from..data.size - RESPONSE_HEADER.size) {
            if (data[i] == RESPONSE_HEADER[0] &&
                data[i + 1] == RESPONSE_HEADER[1] &&
                data[i + 2] == RESPONSE_HEADER[2] &&
                data[i + 3] == RESPONSE_HEADER[3]) {
                return i
            }
        }
        return null
    }
}

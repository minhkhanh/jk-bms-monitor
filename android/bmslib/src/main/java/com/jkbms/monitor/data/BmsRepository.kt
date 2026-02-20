package com.jkbms.monitor.data

import android.content.Context
import android.util.Log
import com.jkbms.monitor.ble.BleConnection
import com.jkbms.monitor.ble.BleScanner
import com.jkbms.monitor.ble.BmsDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformWhile
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
 */
class BmsRepository(context: Context) {

    companion object {
        private const val TAG = "BmsRepository"
        private const val DATA_TIMEOUT_MS = 30_000L

        private val RESPONSE_HEADER = byteArrayOf(0x55.toByte(), 0xAA.toByte().toByte(), 0xEB.toByte(), 0x90.toByte())

        private const val RECORD_TYPE_CELL_DATA = 0x02
        private const val RECORD_TYPE_DEVICE_INFO = 0x03
        // Exact sizes from the Rust struct definitions
        private const val CELL_DATA_SIZE = 300   // RawCellData is ~300 bytes
        private const val DEVICE_INFO_SIZE = 150 // RawDeviceInfo is ~150 bytes
    }

    private val scanner = BleScanner(context)
    private val connection = BleConnection(context)

    val isBluetoothEnabled: Boolean get() = scanner.isBluetoothEnabled
    val isConnected: Boolean get() = connection.isConnected

    fun scanDevices(): Flow<BmsDevice> = scanner.scanDevices()

    suspend fun connect(address: String) {
        connection.connect(address)
    }

    fun disconnect() {
        connection.disconnect()
    }

    suspend fun getDeviceInfo(): DeviceInfo {
        connection.writeCharacteristic(getDeviceInfoRequest())
        val responseData = collectResponse(RECORD_TYPE_DEVICE_INFO)
        Log.d(TAG, "getDeviceInfo response: ${responseData.size} bytes")
        return parseDeviceInfo(responseData)
    }

    suspend fun getCellData(): CellData {
        connection.writeCharacteristic(getCellDataRequest())
        val responseData = collectResponse(RECORD_TYPE_CELL_DATA)
        Log.d(TAG, "getCellData response: ${responseData.size} bytes")
        return parseCellData(responseData)
    }

    /**
     * Accumulate BLE notification chunks until a complete JK BMS response is received.
     *
     * The BMS streams data via notifications including heartbeats (AT\r\n) and
     * response packets. We search for the response header anywhere in the
     * accumulated buffer, then check if we have enough data following it.
     */
    private suspend fun collectResponse(expectedRecordType: Int): ByteArray {
        return withTimeout(DATA_TIMEOUT_MS) {
            var accumulated = ByteArray(0)
            var result: ByteArray? = null

            connection.notifications
                .transformWhile { chunk ->
                    accumulated += chunk
                    result = tryExtractResponse(accumulated, expectedRecordType)
                    emit(Unit)
                    result == null // keep collecting while no result
                }
                .collect { }

            result ?: throw Exception("No complete response found in ${accumulated.size} bytes")
        }
    }

    /**
     * Search for a complete response message within the accumulated data.
     *
     * Scans for RESPONSE_HEADER, checks record type matches, and extracts
     * the response if enough bytes are available.
     */
    private fun tryExtractResponse(data: ByteArray, expectedRecordType: Int): ByteArray? {
        // Search for response header anywhere in the buffer
        var searchFrom = 0
        while (searchFrom + 6 <= data.size) {
            val headerPos = findHeader(data, searchFrom) ?: return null

            // Need at least header(4) + record_type(1) + record_number(1) = 6 bytes
            if (headerPos + 6 > data.size) return null

            val recordType = data[headerPos + 4].toInt() and 0xFF

            if (recordType != expectedRecordType) {
                // Not the record type we want, skip past this header
                searchFrom = headerPos + 4
                continue
            }

            val minSize = when (recordType) {
                RECORD_TYPE_CELL_DATA -> CELL_DATA_SIZE
                RECORD_TYPE_DEVICE_INFO -> DEVICE_INFO_SIZE
                else -> 50
            }

            if (headerPos + minSize <= data.size) {
                // We have enough data — extract the response
                // Find the end: either the next header or end of available data
                val endPos = findHeader(data, headerPos + 4) ?: data.size
                val responseData = data.copyOfRange(headerPos, endPos)
                Log.d(TAG, "Extracted response: type=0x${recordType.toString(16)}, " +
                        "${responseData.size} bytes at offset $headerPos")
                return responseData
            }

            // Not enough data yet after this header
            return null
        }
        return null
    }

    /**
     * Find the position of RESPONSE_HEADER in data starting from offset.
     */
    private fun findHeader(data: ByteArray, from: Int): Int? {
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

package com.jkbms.monitor.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

/**
 * Discovered BMS device information.
 */
data class BmsDevice(
    val name: String,
    val address: String,
    val rssi: Int
)

/**
 * BLE scanner for JK BMS devices.
 * Scans for devices advertising the JK BMS service UUID (0xFFE0).
 */
class BleScanner(context: Context) {

    companion object {
        val JK_BMS_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? get() = bluetoothAdapter?.bluetoothLeScanner

    val isBluetoothEnabled: Boolean get() = bluetoothAdapter?.isEnabled == true

    /**
     * Scan for JK BMS devices. Emits discovered devices as a Flow.
     * Flow completes when collector is cancelled.
     */
    @Suppress("MissingPermission")
    fun scanDevices(): Flow<BmsDevice> = callbackFlow {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(JK_BMS_SERVICE_UUID))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: result.scanRecord?.deviceName ?: "Unknown BMS"
                trySend(
                    BmsDevice(
                        name = name,
                        address = device.address,
                        rssi = result.rssi
                    )
                )
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("BLE scan failed with error code: $errorCode"))
            }
        }

        scanner?.startScan(listOf(scanFilter), scanSettings, callback)
            ?: close(Exception("Bluetooth LE scanner not available"))

        awaitClose {
            try {
                scanner?.stopScan(callback)
            } catch (_: SecurityException) {
                // Ignore - permissions may have been revoked
            }
        }
    }
}

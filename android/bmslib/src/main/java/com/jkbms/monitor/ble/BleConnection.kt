package com.jkbms.monitor.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages BLE connection to a JK BMS device.
 * Handles connect, service discovery, notifications, and read/write operations.
 */
class BleConnection(private val context: Context) {

    companion object {
        private const val TAG = "BleConnection"
        val JK_BMS_SERVICE_UUID: UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        val JK_BMS_CHAR_UUID: UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val CONNECT_TIMEOUT_MS = 10_000L
    }

    private var gatt: BluetoothGatt? = null
    private val notificationChannel = Channel<ByteArray>(Channel.BUFFERED)
    private var connectionDeferred: CompletableDeferred<Unit>? = null
    private var servicesDeferred: CompletableDeferred<Unit>? = null

    val isConnected: Boolean get() = gatt != null

    /**
     * Flow of raw BLE notification data from the JK BMS characteristic.
     */
    val notifications: Flow<ByteArray> = notificationChannel.receiveAsFlow()

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "Connection state changed: status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected, discovering services...")
                    @Suppress("MissingPermission")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected")
                    connectionDeferred?.completeExceptionally(
                        Exception("Disconnected with status $status")
                    )
                    this@BleConnection.gatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.d(TAG, "Services discovered: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectionDeferred?.complete(Unit)
            } else {
                connectionDeferred?.completeExceptionally(
                    Exception("Service discovery failed with status $status")
                )
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == JK_BMS_CHAR_UUID) {
                characteristic.value?.let { data ->
                    notificationChannel.trySend(data)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == JK_BMS_CHAR_UUID) {
                notificationChannel.trySend(value)
            }
        }
    }

    /**
     * Connect to the BMS device and enable notifications.
     */
    @Suppress("MissingPermission")
    suspend fun connect(address: String) {
        disconnect()

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter ?: throw Exception("Bluetooth not available")
        val device: BluetoothDevice = adapter.getRemoteDevice(address)

        connectionDeferred = CompletableDeferred()

        gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

        withTimeout(CONNECT_TIMEOUT_MS) {
            connectionDeferred!!.await()
        }

        // Enable notifications
        enableNotifications()
    }

    @Suppress("MissingPermission")
    private suspend fun enableNotifications() {
        val gatt = this.gatt ?: throw Exception("Not connected")
        val service = gatt.getService(JK_BMS_SERVICE_UUID)
            ?: throw Exception("JK BMS service not found")
        val characteristic = service.getCharacteristic(JK_BMS_CHAR_UUID)
            ?: throw Exception("JK BMS characteristic not found")

        gatt.setCharacteristicNotification(characteristic, true)

        // Write to CCCD to enable notifications
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            // Small delay to allow descriptor write to complete
            kotlinx.coroutines.delay(200)
        }

        Log.d(TAG, "Notifications enabled")
    }

    /**
     * Write data to the JK BMS characteristic (write without response).
     */
    @Suppress("MissingPermission")
    fun writeCharacteristic(data: ByteArray) {
        val gatt = this.gatt ?: throw Exception("Not connected")
        val service = gatt.getService(JK_BMS_SERVICE_UUID)
            ?: throw Exception("JK BMS service not found")
        val characteristic = service.getCharacteristic(JK_BMS_CHAR_UUID)
            ?: throw Exception("JK BMS characteristic not found")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(
                characteristic,
                data,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            )
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = data
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            gatt.writeCharacteristic(characteristic)
        }
    }

    /**
     * Disconnect from the BMS device.
     */
    @Suppress("MissingPermission")
    fun disconnect() {
        gatt?.let {
            it.disconnect()
            it.close()
        }
        gatt = null
    }
}

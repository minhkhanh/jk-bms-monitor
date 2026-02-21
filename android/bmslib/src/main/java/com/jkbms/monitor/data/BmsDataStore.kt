package com.jkbms.monitor.data

import android.content.Context
import android.content.SharedPreferences
import uniffi.jkbms_protocol.CellData
import uniffi.jkbms_protocol.DeviceInfo
import java.util.concurrent.TimeUnit

/**
 * SharedPreferences-backed cache for persisting selected device
 * and BMS data. Shared between main app, widget, and background worker.
 */
class BmsDataStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "bms_data"

        // Selected device keys
        private const val KEY_SELECTED_MAC = "selected_mac"
        private const val KEY_SELECTED_NAME = "selected_name"

        // Cached cell data keys
        private const val KEY_BATTERY_PERCENT = "battery_percent"
        private const val KEY_BATTERY_VOLTAGE = "battery_voltage"
        private const val KEY_BATTERY_CURRENT = "battery_current"
        private const val KEY_BATTERY_POWER = "battery_power"
        private const val KEY_MOSFET_TEMP = "mosfet_temp"
        private const val KEY_REMAIN_CAPACITY = "remain_capacity"
        private const val KEY_NOMINAL_CAPACITY = "nominal_capacity"
        private const val KEY_CYCLE_COUNT = "cycle_count"
        private const val KEY_BATTERY_TEMPS = "battery_temps"

        // Cached device info keys
        private const val KEY_DEVICE_MODEL = "device_model"
        private const val KEY_DEVICE_NAME = "device_name_info"
        private const val KEY_HW_VERSION = "hw_version"
        private const val KEY_SW_VERSION = "sw_version"
        private const val KEY_SERIAL_NUMBER = "serial_number"

        // Timestamp
        private const val KEY_LAST_UPDATE_MS = "last_update_ms"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Selected Device ---

    fun getSelectedDeviceMac(): String? = prefs.getString(KEY_SELECTED_MAC, null)

    fun getSelectedDeviceName(): String? = prefs.getString(KEY_SELECTED_NAME, null)

    fun saveSelectedDevice(mac: String, name: String) {
        prefs.edit()
            .putString(KEY_SELECTED_MAC, mac)
            .putString(KEY_SELECTED_NAME, name)
            .apply()
    }

    fun clearSelectedDevice() {
        prefs.edit()
            .remove(KEY_SELECTED_MAC)
            .remove(KEY_SELECTED_NAME)
            .apply()
    }

    fun hasSelectedDevice(): Boolean = getSelectedDeviceMac() != null

    // --- Cached Cell Data ---

    fun saveCachedCellData(cellData: CellData) {
        val tempsStr = cellData.batteryTemperature.joinToString(",")
        prefs.edit()
            .putInt(KEY_BATTERY_PERCENT, cellData.remainPercent.toInt())
            .putFloat(KEY_BATTERY_VOLTAGE, cellData.batteryVoltage.toFloat())
            .putFloat(KEY_BATTERY_CURRENT, cellData.batteryCurrent.toFloat())
            .putFloat(KEY_BATTERY_POWER, cellData.batteryPower.toFloat())
            .putFloat(KEY_MOSFET_TEMP, cellData.mosfetTemperature.toFloat())
            .putFloat(KEY_REMAIN_CAPACITY, cellData.remainCapacity.toFloat())
            .putFloat(KEY_NOMINAL_CAPACITY, cellData.nominalCapacity.toFloat())
            .putInt(KEY_CYCLE_COUNT, cellData.cycleCount.toInt())
            .putString(KEY_BATTERY_TEMPS, tempsStr)
            .putLong(KEY_LAST_UPDATE_MS, System.currentTimeMillis())
            .apply()
    }

    fun getCachedBatteryPercent(): Int = prefs.getInt(KEY_BATTERY_PERCENT, -1)
    fun getCachedVoltage(): Float = prefs.getFloat(KEY_BATTERY_VOLTAGE, 0f)
    fun getCachedCurrent(): Float = prefs.getFloat(KEY_BATTERY_CURRENT, 0f)
    fun getCachedPower(): Float = prefs.getFloat(KEY_BATTERY_POWER, 0f)
    fun getCachedMosfetTemp(): Float = prefs.getFloat(KEY_MOSFET_TEMP, 0f)
    fun getCachedRemainCapacity(): Float = prefs.getFloat(KEY_REMAIN_CAPACITY, 0f)
    fun getCachedNominalCapacity(): Float = prefs.getFloat(KEY_NOMINAL_CAPACITY, 0f)
    fun getCachedCycleCount(): Int = prefs.getInt(KEY_CYCLE_COUNT, 0)

    fun getCachedBatteryTemps(): List<Float> {
        val str = prefs.getString(KEY_BATTERY_TEMPS, null) ?: return emptyList()
        return str.split(",").mapNotNull { it.toFloatOrNull() }
    }

    // --- Cached Device Info ---

    fun saveCachedDeviceInfo(info: DeviceInfo) {
        prefs.edit()
            .putString(KEY_DEVICE_MODEL, info.deviceModel)
            .putString(KEY_DEVICE_NAME, info.deviceName)
            .putString(KEY_HW_VERSION, info.hardwareVersion)
            .putString(KEY_SW_VERSION, info.softwareVersion)
            .putString(KEY_SERIAL_NUMBER, info.serialNumber)
            .apply()
    }

    fun getCachedDeviceModel(): String? = prefs.getString(KEY_DEVICE_MODEL, null)
    fun getCachedDeviceInfoName(): String? = prefs.getString(KEY_DEVICE_NAME, null)
    fun getCachedHwVersion(): String? = prefs.getString(KEY_HW_VERSION, null)
    fun getCachedSwVersion(): String? = prefs.getString(KEY_SW_VERSION, null)
    fun getCachedSerialNumber(): String? = prefs.getString(KEY_SERIAL_NUMBER, null)

    // --- Timestamp ---

    fun getLastUpdateTimeMs(): Long = prefs.getLong(KEY_LAST_UPDATE_MS, 0)

    fun hasCachedData(): Boolean = getCachedBatteryPercent() >= 0

    /**
     * Returns human-readable time, e.g. "14:30 (2m ago)".
     */
    fun getLastUpdateFormatted(): String {
        val lastMs = getLastUpdateTimeMs()
        if (lastMs == 0L) return "--"
        
        val diffMs = System.currentTimeMillis() - lastMs
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        
        val relative = when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
        
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val absolute = sdf.format(java.util.Date(lastMs))
        
        return "$absolute ($relative)"
    }
}

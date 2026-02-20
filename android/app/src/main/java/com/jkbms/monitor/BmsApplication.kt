package com.jkbms.monitor

import android.app.Application
import com.jkbms.monitor.data.BmsDataStore
import com.jkbms.monitor.worker.BmsWorkScheduler

/**
 * Application class that schedules the background BMS refresh worker
 * on startup if a device has been previously selected.
 */
class BmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Schedule background refresh if a device is saved
        val dataStore = BmsDataStore(this)
        if (dataStore.hasSelectedDevice()) {
            BmsWorkScheduler.schedulePeriodicRefresh(this)
        }
    }
}

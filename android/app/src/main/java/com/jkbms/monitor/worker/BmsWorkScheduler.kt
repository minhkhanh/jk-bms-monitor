package com.jkbms.monitor.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Utility for scheduling/cancelling the periodic BMS refresh worker.
 */
object BmsWorkScheduler {

    private const val TAG = "BmsWorkScheduler"
    private const val PERIODIC_WORK_NAME = "bms_periodic_refresh"

    /**
     * Schedule periodic refresh every 15 minutes.
     * Uses KEEP policy — if already scheduled, does nothing.
     */
    fun schedulePeriodicRefresh(context: Context) {
        Log.d(TAG, "Scheduling periodic BMS refresh")
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<BmsRefreshWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    /**
     * Trigger a one-time immediate refresh (e.g. from widget tap or manual action).
     */
    fun triggerImmediateRefresh(context: Context) {
        Log.d(TAG, "Triggering immediate BMS refresh")
        val request = OneTimeWorkRequestBuilder<BmsRefreshWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }

    /**
     * Cancel all scheduled refreshes.
     */
    fun cancel(context: Context) {
        Log.d(TAG, "Cancelling periodic BMS refresh")
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}

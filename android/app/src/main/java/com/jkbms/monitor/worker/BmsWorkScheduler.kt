package com.jkbms.monitor.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jkbms.monitor.service.BmsForegroundService
import java.util.concurrent.TimeUnit

import com.jkbms.monitor.service.BmsUpdater

/**
 * Worker that simply wakes up every 15 minutes to start the foreground service.
 * We use this because WorkManager handles the 15-minute reliable scheduling,
 * but Foreground Service handles keeping the app alive during BLE connect/fetch.
 */
class BmsTriggerWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        Log.d("BmsTriggerWorker", "WorkManager woke up, triggering Foreground Service")
        
        return try {
            try {
                BmsWorkScheduler.triggerImmediateRefresh(applicationContext)
            } catch (e: Exception) {
                // Catch ForegroundServiceStartNotAllowedException (Android 12+) or IllegalStateException
                val isBgRestriction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                    e is android.app.ForegroundServiceStartNotAllowedException
                
                if (isBgRestriction || e is IllegalStateException) {
                    Log.d("BmsTriggerWorker", "Foreground service start not allowed in background. Running fetch directly in worker.")
                    BmsUpdater.performFetch(applicationContext)
                } else {
                    throw e
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("BmsTriggerWorker", "Worker failed", e)
            Result.failure()
        }
    }
}

/**
 * Utility for scheduling the periodic BMS refresh worker or triggering immediate refresh.
 */
object BmsWorkScheduler {

    private const val TAG = "BmsWorkScheduler"
    private const val PERIODIC_WORK_NAME = "bms_periodic_refresh"

    /**
     * Schedule periodic refresh every 15 minutes.
     */
    fun schedulePeriodicRefresh(context: Context) {
        Log.d(TAG, "Scheduling periodic BMS refresh")
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<BmsTriggerWorker>(
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
     * Trigger a one-time immediate refresh via Foreground Service.
     */
    fun triggerImmediateRefresh(context: Context) {
        Log.d(TAG, "Triggering immediate BMS refresh via Foreground Service")
        val intent = Intent(context, BmsForegroundService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Cancel all scheduled refreshes.
     */
    fun cancel(context: Context) {
        Log.d(TAG, "Cancelling periodic BMS refresh")
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }
}

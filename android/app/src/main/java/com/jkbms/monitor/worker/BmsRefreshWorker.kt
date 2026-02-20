package com.jkbms.monitor.worker

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jkbms.monitor.data.BmsRepository
import com.jkbms.monitor.widget.BmsWidget

/**
 * WorkManager worker that periodically connects to the saved BMS device,
 * fetches data, caches it, and updates the home screen widget.
 *
 * Runs every ~15 minutes regardless of whether the main UI is open.
 */
class BmsRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "BmsRefreshWorker"
    }

    override suspend fun doWork(): Result {
        val repo = BmsRepository(applicationContext)

        if (!repo.dataStore.hasSelectedDevice()) {
            Log.d(TAG, "No device selected, skipping refresh")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Starting background BMS fetch...")
            repo.fetchAndCache()
            Log.d(TAG, "Background fetch complete, updating widget")

            // Update all widget instances
            val widget = BmsWidget()
            val manager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = manager.getGlanceIds(BmsWidget::class.java)
            glanceIds.forEach { id ->
                widget.update(applicationContext, id)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background fetch failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

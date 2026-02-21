package com.jkbms.monitor.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.jkbms.monitor.data.BmsRepository
import com.jkbms.monitor.widget.BmsWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BmsForegroundService : Service() {

    companion object {
        private const val TAG = "BmsForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "bms_refresh_channel"
        private const val MAX_RETRIES = 3
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Starting BMS Foreground Service")

        // Start foreground immediately with "Connecting..." notification
        startForeground(NOTIFICATION_ID, createNotification("Connecting to BMS..."))

        // Launch the update process in a coroutine attached to the service lifecycle
        serviceScope.launch {
            BmsUpdater.performFetch(applicationContext) { msg ->
                updateNotification(msg)
            }

            Log.d(TAG, "BMS Foreground Service finished, stopping self.")
            // Stop the foreground service and remove the notification
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY // If killed, don't restart automatically
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BMS Widget Refresh",
                NotificationManager.IMPORTANCE_LOW // Low importance so it doesn't make a sound
            ).apply {
                description = "Shows progress when updating BMS widget data"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val iconRes = applicationInfo.icon

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JK BMS Monitor")
            .setContentText(content)
            .setSmallIcon(iconRes)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }
}

object BmsUpdater {
    suspend fun performFetch(context: Context, updateNotification: ((String) -> Unit)? = null): Boolean {
        val repo = BmsRepository(context)

        if (!repo.dataStore.hasSelectedDevice()) {
            Log.d("BmsUpdater", "No device selected, stopping")
            return false
        }

        var success = false
        val maxRetries = 3
        for (attempt in 1..maxRetries) {
            try {
                Log.d("BmsUpdater", "Attempt $attempt of $maxRetries to fetch data...")
                updateNotification?.invoke("Fetching data (Attempt $attempt)...")
                
                repo.fetchAndCache()
                
                Log.d("BmsUpdater", "Data fetched successfully, updating widget")
                updateWidgets(context)
                
                success = true
                break // Exit retry loop on success
            } catch (e: Exception) {
                Log.e("BmsUpdater", "Fetch failed on attempt $attempt", e)
                if (attempt < maxRetries) {
                    Log.d("BmsUpdater", "Waiting before retry...")
                    kotlinx.coroutines.delay(2000L * attempt) // Exponential backoff: 2s, 4s, 6s...
                }
            }
        }

        if (!success) {
            Log.e("BmsUpdater", "All $maxRetries attempts failed.")
        }
        return success
    }

    suspend fun updateWidgets(context: Context) {
        val widget = BmsWidget()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(BmsWidget::class.java)
        glanceIds.forEach { id ->
            widget.update(context, id)
        }
    }
}

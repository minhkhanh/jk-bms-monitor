package com.jkbms.monitor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import com.jkbms.monitor.MainActivity
import com.jkbms.monitor.data.BmsDataStore
import com.jkbms.monitor.worker.BmsWorkScheduler

class BmsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataStore = BmsDataStore(context)
        val percent = dataStore.getCachedBatteryPercent()
        val voltage = dataStore.getCachedVoltage()
        val current = dataStore.getCachedCurrent()
        val power = dataStore.getCachedPower()
        val mosfetTemp = dataStore.getCachedMosfetTemp()
        val deviceName = dataStore.getSelectedDeviceName() ?: "JK BMS"
        val lastUpdate = dataStore.getLastUpdateFormatted()

        provideContent {
            BmsWidgetContent(
                percent = percent,
                voltage = voltage,
                current = current,
                power = power,
                temperature = mosfetTemp,
                deviceName = deviceName,
                lastUpdate = lastUpdate
            )
        }
    }
}

@Preview
@Composable
fun BmsWidgetContent(
    percent: Int = 50,
    voltage: Float = 72.1f,
    current: Float = 0.0f,
    power: Float = 0.0f,
    temperature: Float = 30.0f,
    deviceName: String = "JK-BMS",
    lastUpdate: String = "Last update"
) {
    val batteryColor = when {
        percent < 0 -> ColorProvider(Color(0xFF757575), Color(0xFF757575))
        percent > 50 -> ColorProvider(Color(0xFF4CAF50), Color(0xFF4CAF50))
        percent > 20 -> ColorProvider(Color(0xFFFFC107), Color(0xFFFFC107))
        else -> ColorProvider(Color(0xFFF44336), Color(0xFFF44336))
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⚡ $deviceName",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF81C784), Color(0xFF81C784)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())
                )

                // Refresh Button
                Text(
                    "↻",
                    style = TextStyle(
                        color = ColorProvider(Color.White, Color.White),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()).padding(8.dp)
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Battery percentage
            if (percent >= 0) {
                Row(modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>())) {
                    Text(
                        "${percent}%",
                        style = TextStyle(
                            color = batteryColor,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                // Stats
                Text(
                    "%.1fV  %.1fA  %.1fW".format(voltage, current, power),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB0B0B0), Color(0xFFB0B0B0)),
                        fontSize = 14.sp
                    ),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    "🌡️ %.1f°C  •  $lastUpdate".format(temperature),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF757575), Color(0xFF757575)),
                        fontSize = 12.sp
                    ),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                )
            } else {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    "Tap to connect",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF757575), Color(0xFF757575)),
                        fontSize = 14.sp
                    ),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>())
                )
            }
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val dataStore = BmsDataStore(context)
        if (dataStore.hasSelectedDevice()) {
            BmsWorkScheduler.triggerImmediateRefresh(context)
        }
    }
}

class BmsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BmsWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // When widget is added, ensure background refresh is scheduled
        val dataStore = BmsDataStore(context)
        if (dataStore.hasSelectedDevice()) {
            BmsWorkScheduler.schedulePeriodicRefresh(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Trigger immediate fetch whenever Android updates the widget
        val dataStore = BmsDataStore(context)
        if (dataStore.hasSelectedDevice()) {
            BmsWorkScheduler.triggerImmediateRefresh(context)
        }
    }
}

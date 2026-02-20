package com.jkbms.monitor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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

@Composable
fun BmsWidgetContent(
    percent: Int,
    voltage: Float,
    current: Float,
    power: Float,
    temperature: Float,
    deviceName: String,
    lastUpdate: String
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
            .clickable(actionStartActivity<MainActivity>())
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Battery percentage
            if (percent >= 0) {
                Text(
                    "${percent}%",
                    style = TextStyle(
                        color = batteryColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Stats
                Text(
                    "%.1fV  %.1fA  %.1fW".format(voltage, current, power),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFB0B0B0), Color(0xFFB0B0B0)),
                        fontSize = 11.sp
                    )
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    "🌡️ %.1f°C  •  $lastUpdate".format(temperature),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF757575), Color(0xFF757575)),
                        fontSize = 10.sp
                    )
                )
            } else {
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    "Tap to connect",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF757575), Color(0xFF757575)),
                        fontSize = 14.sp
                    )
                )
            }
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
}

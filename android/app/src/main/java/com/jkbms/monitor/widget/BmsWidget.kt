package com.jkbms.monitor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.jkbms.monitor.MainActivity

class BmsWidget : GlanceAppWidget() {

    companion object {
        val KEY_BATTERY_PERCENT = intPreferencesKey("battery_percent")
        val KEY_BATTERY_VOLTAGE = floatPreferencesKey("battery_voltage")
        val KEY_BATTERY_CURRENT = floatPreferencesKey("battery_current")
        val KEY_BATTERY_POWER = floatPreferencesKey("battery_power")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_DEVICE_NAME = stringPreferencesKey("device_name")
        val KEY_LAST_UPDATE = stringPreferencesKey("last_update")
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            BmsWidgetContent()
        }
    }
}

@Composable
fun BmsWidgetContent() {
    val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
    val percent = prefs[BmsWidget.KEY_BATTERY_PERCENT] ?: -1
    val voltage = prefs[BmsWidget.KEY_BATTERY_VOLTAGE] ?: 0f
    val current = prefs[BmsWidget.KEY_BATTERY_CURRENT] ?: 0f
    val power = prefs[BmsWidget.KEY_BATTERY_POWER] ?: 0f
    val temperature = prefs[BmsWidget.KEY_TEMPERATURE] ?: 0f
    val deviceName = prefs[BmsWidget.KEY_DEVICE_NAME] ?: "JK BMS"
    val lastUpdate = prefs[BmsWidget.KEY_LAST_UPDATE] ?: "--"

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
}

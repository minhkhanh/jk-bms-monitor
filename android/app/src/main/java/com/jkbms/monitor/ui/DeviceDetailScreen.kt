package com.jkbms.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uniffi.jkbms_protocol.CellData
import uniffi.jkbms_protocol.DeviceInfo

@Composable
fun DeviceDetailContent(state: BmsUiState) {
    val cellData = state.cellData ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Battery overview card
        item {
            BatteryOverviewCard(cellData, state.lastUpdateTime)
        }

        // Voltage & Current
        item {
            DataCard(title = "⚡ Electrical") {
                DataRow("Voltage", "%.2f V".format(cellData.batteryVoltage))
                DataRow("Current", "%.2f A".format(cellData.batteryCurrent))
                DataRow("Power", "%.2f W".format(cellData.batteryPower))
            }
        }

        // Temperature
        item {
            DataCard(title = "🌡️ Temperature") {
                cellData.batteryTemperature.forEachIndexed { i, temp ->
                    DataRow("Battery ${i + 1}", "%.1f °C".format(temp))
                }
                DataRow("MOSFET", "%.1f °C".format(cellData.mosfetTemperature))
            }
        }

        // Capacity
        item {
            DataCard(title = "🔋 Capacity") {
                DataRow("Remaining", "%.2f Ah".format(cellData.remainCapacity))
                DataRow("Nominal", "%.2f Ah".format(cellData.nominalCapacity))
                DataRow("Cycles", "${cellData.cycleCount}")
                DataRow("Cycle Capacity", "%.2f Ah".format(cellData.cycleCapacity))
            }
        }

        // Cell voltages
        item {
            DataCard(title = "🔬 Cell Voltages") {
                cellData.cellVoltage.forEachIndexed { i, v ->
                    DataRow("Cell ${i + 1}", "%.3f V".format(v))
                }
                DataRow("Average", "%.3f V".format(cellData.averageCellVoltage))
                DataRow("Delta", "%.3f V".format(cellData.deltaCellVoltage))
            }
        }

        // Device info
        state.deviceInfo?.let { info ->
            item {
                DataCard(title = "ℹ️ Device Info") {
                    DataRow("Model", info.deviceModel)
                    DataRow("Name", info.deviceName)
                    DataRow("Hardware", info.hardwareVersion)
                    DataRow("Software", info.softwareVersion)
                    DataRow("Serial", info.serialNumber)
                    DataRow("Mfg Date", info.manufacturingDate)
                }
            }
        }
    }
}

@Composable
fun BatteryOverviewCard(cellData: CellData, lastUpdate: String? = null) {
    val percent = cellData.remainPercent.toInt()
    val batteryColor = when {
        percent > 50 -> Color(0xFF4CAF50)
        percent > 20 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            batteryColor.copy(alpha = 0.3f),
                            Color(0xFF1E1E1E)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "${percent}%",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = batteryColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "%.2f V  •  %.2f A  •  %.2f W".format(
                        cellData.batteryVoltage,
                        cellData.batteryCurrent,
                        cellData.batteryPower
                    ),
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0B0)
                )
                lastUpdate?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Updated $it",
                        fontSize = 11.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

@Composable
fun DataCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF81C784)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFB0B0B0), fontSize = 14.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

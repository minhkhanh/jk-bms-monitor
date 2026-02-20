package com.jkbms.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jkbms.monitor.ui.BmsUiState
import com.jkbms.monitor.ui.BmsViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted or denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBlePermissions()
        setContent {
            BmsMonitorTheme {
                BmsApp()
            }
        }
    }

    private fun requestBlePermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

// --- Theme ---

@Composable
fun BmsMonitorTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        secondary = Color(0xFF81C784),
        surface = Color(0xFF1E1E1E),
        background = Color(0xFF121212),
        onSurface = Color.White,
        onBackground = Color.White,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// --- Main App ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmsApp(viewModel: BmsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("⚡ JK BMS Monitor", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White
                ),
                actions = {
                    if (state.isConnected) {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.disconnect() }) {
                            Icon(Icons.Default.Close, "Disconnect", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF121212))
        ) {
            if (state.isConnected && state.cellData != null) {
                DeviceDetailContent(state)
            } else {
                ScanContent(state, viewModel)
            }

            // Error snackbar
            state.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

// --- Scan Screen ---

@Composable
fun ScanContent(state: BmsUiState, viewModel: BmsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Scan button
        Button(
            onClick = {
                if (state.isScanning) viewModel.stopScan() else viewModel.startScan()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isScanning) Color(0xFFF44336) else Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state.isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Stop Scanning", fontSize = 16.sp)
            } else {
                Icon(Icons.Default.Search, "Scan")
                Spacer(modifier = Modifier.width(12.dp))
                Text("Scan for BMS Devices", fontSize = 16.sp)
            }
        }

        if (state.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = Color(0xFF4CAF50))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Connecting...", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device list
        if (state.discoveredDevices.isNotEmpty()) {
            Text(
                "Discovered Devices",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF81C784),
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.discoveredDevices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.connect(device.address) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.BatteryChargingFull,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    device.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    device.address,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                "${device.rssi} dBm",
                                fontSize = 12.sp,
                                color = Color(0xFF81C784)
                            )
                        }
                    }
                }
            }
        } else if (!state.isScanning) {
            Spacer(modifier = Modifier.height(64.dp))
            Icon(
                Icons.Default.BluetoothSearching,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF424242)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Press scan to find JK BMS devices",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}

// --- Device Detail Screen ---

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
            BatteryOverviewCard(cellData)
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
fun BatteryOverviewCard(cellData: uniffi.jkbms_protocol.CellData) {
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

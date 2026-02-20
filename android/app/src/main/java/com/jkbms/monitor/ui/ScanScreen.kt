package com.jkbms.monitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanContent(state: BmsUiState, viewModel: BmsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Saved device indicator
        state.savedDeviceName?.let { name ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Saved: $name",
                            fontSize = 13.sp,
                            color = Color(0xFF81C784)
                        )
                        state.savedDeviceMac?.let { mac ->
                            Text(mac, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    if (state.lastUpdateTime != null) {
                        Text(
                            state.lastUpdateTime,
                            fontSize = 10.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                            .clickable { viewModel.connect(device.address, device.name) },
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

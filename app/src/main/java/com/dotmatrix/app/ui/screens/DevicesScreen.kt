package com.dotmatrix.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(sharedViewModel: SharedConnectionViewModel) {
    val scannedDevices  by sharedViewModel.scannedDevices.collectAsState()
    val connectedDevice by sharedViewModel.deviceName.collectAsState()
    val isConnected     by sharedViewModel.isConnected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Devices", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Scan button
            Button(
                onClick   = { sharedViewModel.startScan() },
                modifier  = Modifier.fillMaxWidth().height(48.dp),
                shape     = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan for Devices", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(20.dp))

            if (scannedDevices.isEmpty()) {
                // Passive empty state
                EmptyStateCard(
                    icon     = Icons.Outlined.BluetoothDisabled,
                    title    = "No Devices Found",
                    subtitle = "Tap \"Scan for Devices\" to search"
                )
            } else {
                Text(
                    "Nearby Devices",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scannedDevices) { device ->
                        val isThisConnected = isConnected && device.name == connectedDevice
                        DeviceRow(
                            name        = device.name ?: "Unknown Device",
                            isConnected = isThisConnected,
                            onConnect   = {
                                if (isThisConnected) sharedViewModel.disconnect()
                                else sharedViewModel.connectToDevice(device)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(name: String, isConnected: Boolean, onConnect: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Bluetooth,
                contentDescription = null,
                tint     = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                if (isConnected) {
                    Text(
                        "Connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            OutlinedButton(
                onClick = onConnect,
                shape   = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(if (isConnected) "Disconnect" else "Connect", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

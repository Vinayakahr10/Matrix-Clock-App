package com.dotmatrix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.ble.BleError
import com.dotmatrix.app.ui.components.EmptyStateCard
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(sharedViewModel: SharedConnectionViewModel) {
    val scannedDevices  by sharedViewModel.scannedDevices.collectAsState()
    val connectedDevice by sharedViewModel.deviceName.collectAsState()
    val isConnected     by sharedViewModel.isConnected.collectAsState()
    val isScanning      by sharedViewModel.isScanning.collectAsState()
    val bleError        by sharedViewModel.bleError.collectAsState()
    val temperature     by sharedViewModel.temperature.collectAsState()
    val humidity        by sharedViewModel.humidity.collectAsState()
    val haptic = LocalHapticFeedback.current

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bleError) {
        bleError?.let { error ->
            val message = when (error) {
                BleError.PERMISSION_DENIED     -> "Bluetooth permissions are required."
                BleError.BLUETOOTH_DISABLED     -> "Bluetooth is turned off."
                BleError.LOCATION_DISABLED      -> "Location services are required for scanning."
                BleError.SCAN_FAILED            -> "BLE scan failed."
                BleError.CONNECTION_FAILED      -> "Failed to connect to device."
                BleError.SERVICE_NOT_FOUND      -> "Device service not found."
                BleError.CHARACTERISTIC_NOT_FOUND -> "Device characteristic not found."
                BleError.WRITE_FAILED           -> "Failed to send command."
                BleError.OTA_FAILED             -> "Firmware update failed."
            }
            snackbarHostState.showSnackbar(message = message, actionLabel = "Dismiss")
            sharedViewModel.clearBleError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Bluetooth Devices", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Scan Control Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isScanning) "Searching..." else "Discovery Stopped",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isScanning) "Finding nearby Dot Matrix clocks" else "Tap to start searching",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                sharedViewModel.startScan() 
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Scan")
                        }
                    }
                }
            }

            // Room Temp Highlight (shown when connected)
            if (isConnected && temperature != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.DeviceThermostat, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Room Temp: ${temperature?.toInt()}°C • Humidity: ${humidity?.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (scannedDevices.isEmpty() && !isScanning) {
                EmptyStateCard(
                    icon     = Icons.Outlined.BluetoothSearching,
                    title    = "No Clocks Found",
                    subtitle = "Ensure your clock is powered on and nearby"
                )
            } else {
                Text(
                    "Available Devices (${scannedDevices.size})",
                    style    = MaterialTheme.typography.labelLarge,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp, top = 8.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(
                        items = scannedDevices,
                        key = { it.device.address }
                    ) { scanned ->
                        val isThisConnected = isConnected && (scanned.device.address == connectedDevice || scanned.displayName == connectedDevice)
                        
                        DeviceRow(
                            name = scanned.displayName,
                            address = scanned.device.address,
                            rssi = scanned.rssi,
                            isConnected = isThisConnected,
                            onConnect = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isThisConnected) sharedViewModel.disconnect()
                                else sharedViewModel.connect(scanned.device)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    name: String, 
    address: String, 
    rssi: Int,
    isConnected: Boolean, 
    onConnect: () -> Unit
) {
    val isStrongSignal = rssi > -60
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConnected) 0.dp else 2.dp),
        border = if (isConnected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal Strength Icon
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.SignalCellularAlt,
                    contentDescription = null,
                    tint = when {
                        isConnected -> MaterialTheme.colorScheme.primary
                        isStrongSignal -> Color(0xFF4CAF50) // Green
                        rssi > -80 -> Color(0xFFFFC107) // Amber
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (name == "Unknown Device") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onConnect,
                shape = RoundedCornerShape(12.dp),
                colors = if (isConnected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = if (isConnected) "Disconnect" else "Connect",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

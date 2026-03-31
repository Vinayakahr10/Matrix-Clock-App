package com.dotmatrix.app.ui.screens

import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val headerAlpha by animateFloatAsState(
        targetValue = if (scrollBehavior.state.collapsedFraction > 0.05f) 0.85f else 0f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "headerAlpha"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = headerAlpha))) {
                LargeTopAppBar(
                    title = { 
                        Text(
                            "Devices", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displaySmall
                        ) 
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Scan Control Hero Block
                val infiniteTransition = rememberInfiniteTransition(label = "scanSweep")
                val gradientColors = if (isScanning) {
                    val color1 by infiniteTransition.animateColor(
                        initialValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        targetValue = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "c1"
                    )
                    val color2 by infiniteTransition.animateColor(
                        initialValue = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutLinearInEasing), RepeatMode.Reverse),
                        label = "c2"
                    )
                    listOf(color1, color2)
                } else {
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(gradientColors))
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isScanning) "Searching..." else "Discovery Stopped",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isScanning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isScanning) "Locating nearby Dot Matrix units" else "Tap to start searching",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            var isRefreshPressed by remember { mutableStateOf(false) }
                            val refreshScale by animateFloatAsState(
                                targetValue = if (isRefreshPressed) 0.85f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "refreshScale"
                            )
                            LaunchedEffect(isRefreshPressed) {
                                if (isRefreshPressed) { delay(150); isRefreshPressed = false }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .graphicsLayer { scaleX = refreshScale; scaleY = refreshScale }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { 
                                        isRefreshPressed = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        sharedViewModel.startScan() 
                                    },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = "Scan", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Room Temp Highlight (shoown when connected)
            if (isConnected && temperature != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Indoor Pill
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.DeviceThermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("ROOM TEMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${temperature?.toInt()}°C",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Humidity Pill
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("HUMIDITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${humidity?.toInt()}%",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (scannedDevices.isEmpty() && !isScanning) {
                item {
                    Spacer(Modifier.height(16.dp))
                    EmptyStateCard(
                        icon     = Icons.Outlined.BluetoothSearching,
                        title    = "No Clocks Found",
                        subtitle = "Ensure your clock is powered on and within range"
                    )
                }
            } else {
                item {
                    Text(
                        "Available Devices (${scannedDevices.size})",
                        style    = MaterialTheme.typography.labelLarge,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 16.dp)
                    )
                }
                
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

@Composable
private fun DeviceRow(
    name: String, 
    address: String, 
    rssi: Int,
    isConnected: Boolean, 
    onConnect: () -> Unit
) {
    val isStrongSignal = rssi > -60
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "devScale"
    )
    LaunchedEffect(isPressed) {
        if (isPressed) { delay(150); isPressed = false }
    }

    val bgBrush = if (isConnected) {
        val infiniteTransition = rememberInfiniteTransition(label = "connSweep")
        val c1 by infiniteTransition.animateColor(
            initialValue = Color(0xFF10B981).copy(alpha = 0.15f),
            targetValue = Color(0xFF0EA5E9).copy(alpha = 0.25f),
            animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Reverse),
            label = "bc1"
        )
        val c2 by infiniteTransition.animateColor(
            initialValue = Color(0xFF0EA5E9).copy(alpha = 0.05f),
            targetValue = Color(0xFF10B981).copy(alpha = 0.15f),
            animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Reverse),
            label = "bc2"
        )
        Brush.linearGradient(listOf(c1, c2))
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onConnect()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConnected) 4.dp else 1.dp),
        border = if (isConnected) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)) else null
    ) {
        Box(
            modifier = Modifier
                .background(bgBrush)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Signal Strength Icon
                Surface(
                    shape = CircleShape,
                    color = if (isConnected) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SignalCellularAlt,
                            contentDescription = null,
                            tint = when {
                                isConnected -> Color(0xFF10B981)
                                isStrongSignal -> Color(0xFF4CAF50) // Green
                                rssi > -80 -> Color(0xFFFFC107) // Amber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (name == "Unknown Device") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = CircleShape,
                    color = if (isConnected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 0.dp
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isConnected) "Disconnect" else "Connect",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

package com.dotmatrix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    sharedViewModel: SharedConnectionViewModel
) {
    val isConnected by sharedViewModel.isConnected.collectAsState()
    val deviceName  by sharedViewModel.deviceName.collectAsState()
    val is24H       by sharedViewModel.is24HourFormat.collectAsState()
    val temperature by sharedViewModel.temperature.collectAsState()
    val humidity    by sharedViewModel.humidity.collectAsState()
    val haptic      = LocalHapticFeedback.current

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { currentTime = LocalDateTime.now(); delay(1000) } }

    val timeFmt = if (is24H) DateTimeFormatter.ofPattern("HH:mm")
                  else       DateTimeFormatter.ofPattern("h:mm")
    val amPmFmt = DateTimeFormatter.ofPattern("a")
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM dd")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Matrix Control", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("settings")
                    }) {
                        Icon(Icons.Outlined.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Clock Card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = currentTime.format(timeFmt),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-2).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!is24H) {
                                Text(
                                    text = " " + currentTime.format(amPmFmt),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 14.dp)
                                )
                            }
                        }
                        Text(
                            text = currentTime.format(dateFmt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Room Environment Card ───────────────────────────────────────
            EnvironmentCard(isConnected, temperature, humidity)

            Spacer(Modifier.height(16.dp))

            // ── Connection Status Card ──────────────────────────────────────
            val statusColor = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
            
            Card(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("devices") 
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isConnected) Icons.Outlined.BluetoothConnected else Icons.Outlined.BluetoothDisabled,
                                contentDescription = null,
                                tint = if (isConnected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) deviceName else "Disconnected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) "Connected and ready" else "Tap to scan for devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isConnected) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.3f))
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Quick Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Settings",
                    icon = Icons.Outlined.SettingsSuggest,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("settings") 
                    }
                )
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Visualizer",
                    icon = Icons.Outlined.AutoGraph,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("visualizer") 
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Sync Time",
                    icon = Icons.Outlined.Restore,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sharedViewModel.syncTime() 
                    }
                )
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Firmware",
                    icon = Icons.Outlined.Terminal,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("ota") 
                    }
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EnvironmentCard(isConnected: Boolean, temperature: Float?, humidity: Float?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DeviceThermostat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Room Environment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (!isConnected) {
                    Text(
                        "Connect to view sensor data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else if (temperature == null || humidity == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Reading sensor...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${temperature.toInt()}°C",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp
                                )
                            )
                            Spacer(Modifier.width(12.dp))
                            val indicatorColor = when {
                                temperature > 30f -> Color(0xFFEF4444) // Red
                                temperature < 18f -> Color(0xFF3B82F6) // Blue
                                else -> Color(0xFF10B981) // Green
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(indicatorColor)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${humidity.toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        "Room Temperature",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickControlCard(
    modifier: Modifier, 
    label: String, 
    icon: ImageVector, 
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = label, 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DextBotScreen(viewModel: SharedConnectionViewModel) {
    val isConnected by viewModel.isConnected.collectAsState()
    val dextBotState by viewModel.dextBotState.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isConnected) {
        while (isConnected) {
            viewModel.bleManager.writeData("DEXTBOT?")
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DextBot Live", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Robot Face Visualizer ───────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RobotEye(isDizzy = dextBotState.accelX > 20000)
                        RobotEye(isDizzy = dextBotState.accelX > 20000)
                    }
                }
            }

            // ── Distance Sensor ─────────────────────────────────────────────
            SensorDataCard(
                title = "Proximity",
                value = "${dextBotState.distance} mm",
                icon = Icons.Outlined.SocialDistance,
                description = "VL53L0X Laser Distance Sensor",
                status = if (dextBotState.vl53Ok) "Active" else "Offline",
                statusColor = if (dextBotState.vl53Ok) Color(0xFF10B981) else MaterialTheme.colorScheme.error
            )

            // ── IMU / Orientation ───────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Explore, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Orientation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (dextBotState.mpuOk) "MPU-6050 OK" else "IMU ERROR",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dextBotState.mpuOk) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        AxisData("X", dextBotState.accelX)
                        AxisData("Y", dextBotState.accelY)
                        AxisData("Z", dextBotState.accelZ)
                    }
                }
            }

            // ── Bot Actions ──────────────────────────────────────────────────
            Text("Bot Actions", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            
            Button(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.setDisplayMode("DEXTBOT") 
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = isConnected
            ) {
                Icon(Icons.Outlined.Pets, null)
                Spacer(Modifier.width(12.dp))
                Text("Activate DextBot Mode")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RobotEye(isDizzy: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isDizzy) 1.2f else 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isDizzy) 100 else 2000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(60.dp)
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale 
            }
            .background(Color(0xFF00FFCC).copy(alpha = 0.2f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color(0xFF00FFCC), CircleShape)
        )
    }
}

@Composable
private fun AxisData(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SensorDataCard(
    title: String,
    value: String,
    icon: ImageVector,
    description: String,
    status: String,
    statusColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                Text(status, style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
        }
    }
}

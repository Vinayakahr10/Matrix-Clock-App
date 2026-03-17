package com.dotmatrix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
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
    val haptic      = LocalHapticFeedback.current

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { currentTime = LocalDateTime.now(); delay(1000) } }

    val timeFmt = if (is24H) DateTimeFormatter.ofPattern("HH:mm")
                  else       DateTimeFormatter.ofPattern("hh:mm a")
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM dd")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Matrix Control", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Clock card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text       = currentTime.format(timeFmt),
                        fontSize   = 56.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.SansSerif,
                        color      = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = currentTime.format(dateFmt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Connection card ─────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("devices")
                },
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier            = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bluetooth,
                        contentDescription = null,
                        tint   = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text       = if (isConnected) deviceName else "Not Connected",
                            fontWeight = FontWeight.Medium,
                            style      = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text  = if (isConnected) "Tap to manage" else "Tap to scan for devices",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF137333) else MaterialTheme.colorScheme.outline)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Quick Controls",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // ── 2x2 grid ────────────────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickCard(Modifier.weight(1f), "Settings",  Icons.Outlined.Settings,
                    { haptic.performHapticFeedback(HapticFeedbackType.LongPress); navController.navigate("clock") })
                QuickCard(Modifier.weight(1f), "Visualizer", Icons.Outlined.GraphicEq,
                    { haptic.performHapticFeedback(HapticFeedbackType.LongPress); navController.navigate("visualizer") })
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickCard(Modifier.weight(1f), "Sync Time",  Icons.Outlined.Sync,
                    { haptic.performHapticFeedback(HapticFeedbackType.LongPress); sharedViewModel.syncTime() })
                QuickCard(Modifier.weight(1f), "Firmware",   Icons.Outlined.SystemUpdate,
                    { haptic.performHapticFeedback(HapticFeedbackType.LongPress); navController.navigate("ota") })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickCard(modifier: Modifier, label: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier  = modifier.height(90.dp).clickable(onClick = onClick),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

package com.dotmatrix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dotmatrix.app.viewmodel.OTAState
import com.dotmatrix.app.viewmodel.OTAViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTAScreen(otaViewModel: OTAViewModel = viewModel()) {
    val state by otaViewModel.otaState.collectAsState()
    val progress by otaViewModel.downloadProgress.collectAsState()
    val releaseInfo by otaViewModel.releaseInfo.collectAsState()
    val currentVersion = otaViewModel.currentVersion

    LaunchedEffect(Unit) {
        if (state == OTAState.Idle) {
            otaViewModel.checkForUpdates()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200),
        label = "OTA Progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Update", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Info Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("System Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    VersionRow("Current Firmware", currentVersion, MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    VersionRow(
                        label = "Latest Available", 
                        version = releaseInfo?.version ?: "Checking...", 
                        versionColor = if (state == OTAState.UpdateAvailable) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Card
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "OTA State"
            ) { currentState ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (currentState == OTAState.Success) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (currentState) {
                            OTAState.Checking, OTAState.Idle -> {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Checking GitHub for updates...")
                            }
                            OTAState.NoUpdate -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("You're up to date!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { otaViewModel.checkForUpdates() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp)
                                ) { Text("Check Again") }
                            }
                            OTAState.Error -> {
                                Text("Failed to fetch or download update.", color = MaterialTheme.colorScheme.error)
                                Button(
                                    onClick = { otaViewModel.checkForUpdates() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(50.dp)
                                ) { Text("Retry") }
                            }
                            OTAState.UpdateAvailable -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(64.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("New Update Ready", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    releaseInfo?.releaseNotes ?: "Bug fixes and improvements.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Button(
                                    onClick = { otaViewModel.downloadUpdate() },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Download (${(releaseInfo?.size ?: 0) / 1024} KB)", fontWeight = FontWeight.Bold)
                                }
                            }
                            OTAState.Downloading, OTAState.Installing -> {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary, 
                                    modifier = Modifier.size(64.dp).rotate(rotation)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (currentState == OTAState.Downloading) "Downloading from GitHub..." else "Installing to ESP32...", 
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${(animatedProgress * 100).toInt()}%", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                            OTAState.ReadyToInstall -> {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Download Complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { otaViewModel.startInstall() },
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp)
                                ) { Text("Install to Clock") }
                            }
                            OTAState.Success -> {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = null, 
                                    tint = Color(0xFF22C55E), 
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("System Updated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                                Text(
                                    "${releaseInfo?.version} has been installed successfully. Your device is rebooting.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Button(
                                    onClick = { otaViewModel.reset() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Great!", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VersionRow(label: String, version: String, versionColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(version, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = versionColor)
    }
}

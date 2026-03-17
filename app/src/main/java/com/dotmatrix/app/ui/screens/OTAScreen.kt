package com.dotmatrix.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dotmatrix.app.viewmodel.OTAState
import com.dotmatrix.app.viewmodel.OTAViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTAScreen(otaViewModel: OTAViewModel = viewModel()) {
    val state       by otaViewModel.otaState.collectAsState()
    val progress    by otaViewModel.downloadProgress.collectAsState()
    val releaseInfo by otaViewModel.releaseInfo.collectAsState()

    LaunchedEffect(Unit) {
        if (state == OTAState.Idle) otaViewModel.checkForUpdates()
    }

    val spinTransition = rememberInfiniteTransition(label = "spin")
    val rotation by spinTransition.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "spin"
    )
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "prog",
        animationSpec = tween(200))

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Firmware Update", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── System Info card ────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("System Info", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline)
                    VersionRow("Current Firmware", otaViewModel.currentVersion, MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline)
                    VersionRow(
                        label        = "Latest Available",
                        version      = releaseInfo?.version ?: if (state == OTAState.Checking || state == OTAState.Idle) "Checking…" else "—",
                        versionColor = if (state == OTAState.UpdateAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Action card ─────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state) {
                        OTAState.Checking, OTAState.Idle -> {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Checking for updates…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OTAState.NoUpdate -> {
                            IconInCircle(Icons.Outlined.CheckCircle, tint = Color(0xFF137333))
                            Spacer(Modifier.height(12.dp))
                            Text("You're up to date", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("${otaViewModel.currentVersion} is the latest version.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { otaViewModel.checkForUpdates() },
                                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Check Again")
                            }
                        }
                        OTAState.Error -> {
                            IconInCircle(Icons.Outlined.ErrorOutline, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(12.dp))
                            Text("Failed to fetch update", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { otaViewModel.checkForUpdates() },
                                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Retry")
                            }
                        }
                        OTAState.UpdateAvailable -> {
                            IconInCircle(Icons.Outlined.SystemUpdate, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("Update Available", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(releaseInfo?.releaseNotes?.take(120) ?: "Bug fixes and improvements.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { otaViewModel.downloadUpdate() },
                                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Download & Install  ·  ${(releaseInfo?.size ?: 0) / 1024} KB", fontWeight = FontWeight.Medium)
                            }
                        }
                        OTAState.Downloading, OTAState.Installing -> {
                            Icon(Icons.Outlined.Sync, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp).rotate(rotation))
                            Spacer(Modifier.height(12.dp))
                            Text(if (state == OTAState.Downloading) "Downloading…" else "Installing…",
                                fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("${(animatedProgress * 100).toInt()}%", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                        OTAState.ReadyToInstall -> {
                            IconInCircle(Icons.Outlined.DownloadDone, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text("Download Complete", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { otaViewModel.startInstall() },
                                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("Install to Clock", fontWeight = FontWeight.Medium)
                            }
                        }
                        OTAState.Success -> {
                            IconInCircle(Icons.Outlined.CheckCircle, tint = Color(0xFF137333))
                            Spacer(Modifier.height(12.dp))
                            Text("Update Installed", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("${releaseInfo?.version} installed. Your clock is rebooting.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            FilledTonalButton(onClick = { otaViewModel.reset() },
                                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text("Done")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconInCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Surface(color = tint.copy(alpha = 0.12f), shape = CircleShape, modifier = Modifier.size(64.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun VersionRow(label: String, version: String, versionColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(version, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace, color = versionColor)
    }
}

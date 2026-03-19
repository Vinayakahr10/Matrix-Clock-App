package com.dotmatrix.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.viewmodel.OTAState
import com.dotmatrix.app.viewmodel.OTAViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTAScreen(otaViewModel: OTAViewModel) {
    val state             by otaViewModel.otaState.collectAsState()
    val releaseInfo       by otaViewModel.releaseInfo.collectAsState()
    val currentVersion    by otaViewModel.currentVersion.collectAsState()
    val downloadProgress  by otaViewModel.downloadProgress.collectAsState()
    val errorMessage      by otaViewModel.errorMessage.collectAsState()
    val haptic            = LocalHapticFeedback.current
    val importFirmwareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            otaViewModel.uploadLocalFirmware(uri)
        }
    }

    LaunchedEffect(Unit) {
        if (state == OTAState.Idle) otaViewModel.checkForUpdates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Firmware Update", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (state != OTAState.Checking && state != OTAState.NoUpdate) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state == OTAState.Downloading || state == OTAState.Installing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (state == OTAState.Downloading) "Downloading..." else "Installing...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            )
                        } else {
                            val buttonText = when (state) {
                                OTAState.UpdateAvailable -> "Download Update"
                                OTAState.ReadyToInstall -> "Upload to Device"
                                OTAState.Success -> "Update Successful"
                                OTAState.Error -> if (releaseInfo == null) "Retry Check" else "Retry Download"
                                else -> "Update Firmware"
                            }
                            
                            val isEnabled = state != OTAState.Success
                            
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (state == OTAState.UpdateAvailable) {
                                        otaViewModel.downloadUpdate()
                                    } else if (state == OTAState.ReadyToInstall) {
                                        otaViewModel.startInstall()
                                    } else if (state == OTAState.Error) {
                                        otaViewModel.retry()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = isEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state == OTAState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (state == OTAState.Error) Icons.Outlined.Refresh else Icons.Outlined.SystemUpdate,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(buttonText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            if (state != OTAState.Success) {
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        importFirmwareLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.UploadFile,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Import BIN From Device",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Version Status Card ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isUpToDate = currentVersion.replace("v", "").trim() == 
                                         (releaseInfo?.version?.replace("v", "")?.trim() ?: currentVersion.replace("v", "").trim())
                        
                        val statusColor = when (state) {
                            OTAState.Error -> MaterialTheme.colorScheme.error
                            OTAState.Success -> Color(0xFF10B981)
                            else -> if (isUpToDate) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        }
                        
                        val statusIcon = when (state) {
                            OTAState.Error -> Icons.Outlined.ErrorOutline
                            OTAState.Success -> Icons.Outlined.CheckCircle
                            else -> if (isUpToDate) Icons.Outlined.CheckCircle else Icons.Outlined.Update
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(20.dp))
                            }
                        }
                        
                        Spacer(Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = when (state) {
                                    OTAState.Error -> "Update Failed"
                                    OTAState.Success -> "Successfully Updated"
                                    else -> if (isUpToDate) "System is up to date" else "Update Available"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (state) {
                                    OTAState.Error -> "Something went wrong. Please try again."
                                    OTAState.Success -> "Your device is now running the latest firmware."
                                    else -> if (isUpToDate) "You have the latest firmware" else "New version ${releaseInfo?.version} is ready"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state == OTAState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        VersionInfoBlock("Current", currentVersion)
                        VersionInfoBlock("Latest", releaseInfo?.version ?: "Checking...")
                    }

                    if (state == OTAState.Error && !errorMessage.isNullOrBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // ── Release Details ─────────────────────────────────────────────
            releaseInfo?.let { info ->
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )

                if (!info.fileName.isNullOrBlank() || info.size != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Package Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            info.fileName?.takeIf { it.isNotBlank() }?.let { fileName ->
                                Text(
                                    text = "File: $fileName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            info.size?.let { size ->
                                Text(
                                    text = "Size: ${formatFileSize(size)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (info.new_features.isNotEmpty()) {
                    UpdateSection("New Features", Icons.Outlined.Star, info.new_features)
                }

                if (info.improvements.isNotEmpty()) {
                    UpdateSection("Improvements", Icons.Outlined.AutoGraph, info.improvements)
                }

                if (info.notes.isNotEmpty()) {
                    UpdateSection("Notes", Icons.Outlined.Info, info.notes, isNotes = true)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                )
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Local Firmware",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "You can also import a .bin firmware file from your phone and upload it directly to the clock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state == OTAState.Checking) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun VersionInfoBlock(label: String, version: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(version, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun UpdateSection(title: String, icon: ImageVector, items: List<String>, isNotes: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNotes) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { item ->
                    Row {
                        Text("•", Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
                        Text(item, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024L
    val mb = kb * 1024L
    return when {
        bytes >= mb -> String.format("%.2f MB", bytes.toDouble() / mb.toDouble())
        bytes >= kb -> String.format("%.1f KB", bytes.toDouble() / kb.toDouble())
        else -> "$bytes B"
    }
}

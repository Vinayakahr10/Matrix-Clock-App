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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        if (state == OTAState.Idle) otaViewModel.checkForUpdates()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Firmware", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.displaySmall
                    ) 
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            if (state != OTAState.Checking && state != OTAState.NoUpdate) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .navigationBarsPadding()
                    ) {
                        if (state == OTAState.Downloading || state == OTAState.Installing) {
                            // Immersive Progress Bar
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (state == OTAState.Downloading) "Downloading Update..." else "Transferring to Clock...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(24.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = downloadProgress)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        } else {
                            val buttonText = when (state) {
                                OTAState.UpdateAvailable -> "Download Update"
                                OTAState.ReadyToInstall -> "Upload to Clock"
                                OTAState.Success -> "Update Successful"
                                OTAState.Error -> if (releaseInfo == null) "Retry Check" else "Retry Download"
                                else -> "Update Firmware"
                            }
                            
                            val isEnabled = state != OTAState.Success
                            val buttonShape = RoundedCornerShape(24.dp)
                            
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (state == OTAState.UpdateAvailable) otaViewModel.downloadUpdate()
                                    else if (state == OTAState.ReadyToInstall) otaViewModel.startInstall()
                                    else if (state == OTAState.Error) otaViewModel.retry()
                                },
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                shape = buttonShape,
                                enabled = isEnabled,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (state == OTAState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = if (state == OTAState.Error) Icons.Outlined.Refresh else Icons.Outlined.RocketLaunch,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(buttonText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val isUpToDate = currentVersion.replace("v", "").trim() == 
                             (releaseInfo?.version?.replace("v", "")?.trim() ?: currentVersion.replace("v", "").trim())
            
            val statusColor = when (state) {
                OTAState.Error -> MaterialTheme.colorScheme.error
                OTAState.Success -> Color(0xFF10B981)
                else -> if (isUpToDate) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
            }

            // ── Atmospheric Hero Status Card ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.15f),
                                statusColor.copy(alpha = 0.02f)
                            ),
                            radius = 600f
                        )
                    )
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val infiniteTransition = rememberInfiniteTransition(label = "heroPulse")
                    val iconScale by infiniteTransition.animateFloat(
                        initialValue = 0.95f, targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                        label = "iconScale"
                    )
                    
                    val statusIcon = when (state) {
                        OTAState.Error -> Icons.Outlined.ErrorOutline
                        OTAState.Success -> Icons.Outlined.CheckCircle
                        else -> if (isUpToDate) Icons.Outlined.Verified else Icons.Outlined.SystemUpdateAlt
                    }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.1f))
                            .graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(statusColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = when (state) {
                            OTAState.Error -> "Update Failed"
                            OTAState.Success -> "Successfully Updated"
                            else -> if (isUpToDate) "System is Up to Date" else "Update Available"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = when (state) {
                            OTAState.Error -> "Something went wrong. Please check your connection."
                            OTAState.Success -> "Your device is now running the latest software."
                            else -> if (isUpToDate) "You are running the latest firmware." else "Version ${releaseInfo?.version} is ready to be installed."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state == OTAState.Error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    // Version segmented pills
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Current: $currentVersion", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Outlined.ArrowRightAlt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (!isUpToDate) statusColor else MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Latest: ${releaseInfo?.version ?: currentVersion}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (!isUpToDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (state == OTAState.Error && !errorMessage.isNullOrBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── Release Details ─────────────────────────────────────────────
            releaseInfo?.let { info ->
                if (!info.fileName.isNullOrBlank() || info.size != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        info.fileName?.takeIf { it.isNotBlank() }?.let { fileName ->
                            DetailPill(Icons.Outlined.Description, fileName, Modifier.weight(1f))
                        }
                        info.size?.let { size ->
                            DetailPill(Icons.Outlined.SdStorage, formatFileSize(size), Modifier.weight(1f))
                        }
                    }
                }

                if (info.new_features.isNotEmpty()) {
                    UpdateSection("New Features", Icons.Outlined.AutoAwesome, info.new_features)
                }

                if (info.improvements.isNotEmpty()) {
                    UpdateSection("Improvements", Icons.Outlined.TrendingUp, info.improvements)
                }

                if (info.notes.isNotEmpty()) {
                    UpdateSection("Important Notes", Icons.Outlined.WarningAmber, info.notes, isNotes = true)
                }
            }

            if (state == OTAState.Checking) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(strokeWidth = 4.dp, modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Checking for updates...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(120.dp)) // Extra space for huge bottom bar
        }
    }
}

@Composable
private fun DetailPill(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpdateSection(title: String, icon: ImageVector, items: List<String>, isNotes: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)) {
            Icon(icon, null, tint = if (isNotes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNotes) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) 
                                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isNotes) 0.dp else 1.dp)
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isNotes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(item, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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

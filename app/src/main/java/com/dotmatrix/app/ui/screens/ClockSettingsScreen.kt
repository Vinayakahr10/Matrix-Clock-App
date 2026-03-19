package com.dotmatrix.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsScreen(sharedViewModel: SharedConnectionViewModel) {
    val is24H       by sharedViewModel.is24HourFormat.collectAsState()
    val brightness  by sharedViewModel.brightness.collectAsState()
    val animation   by sharedViewModel.animationStyle.collectAsState()
    val scrollText  by sharedViewModel.scrollText.collectAsState()
    val haptic      = LocalHapticFeedback.current

    var localBrightness  by remember(brightness)  { mutableFloatStateOf(brightness) }
    var localAnimation   by remember(animation)   { mutableStateOf(animation) }
    var expanded         by remember { mutableStateOf(false) }

    val animationOptions = listOf("None", "Fade", "Slide", "Bounce")

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("Clock Settings", fontWeight = FontWeight.Medium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Time Format ────────────────────────────────────────────────
            SettingsCard {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("24-Hour Format", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                        Text("Switch between 12h and 24h", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked         = is24H,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sharedViewModel.setTimeFormat(it) 
                        }
                    )
                }
            }

            // ── Brightness ─────────────────────────────────────────────────
            SettingsCard {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BrightnessHigh, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Brightness", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Text("${(localBrightness * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value         = localBrightness,
                        onValueChange = { 
                            if ((it * 100).toInt() != (localBrightness * 100).toInt()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            localBrightness = it 
                        },
                        onValueChangeFinished = { sharedViewModel.setBrightness(localBrightness) },
                        modifier      = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }

            // ── Animation Style ────────────────────────────────────────────
            SettingsCard {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Animation, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Animation Style", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value         = localAnimation,
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier      = Modifier.menuAnchor().width(120.dp),
                            textStyle     = MaterialTheme.typography.bodyMedium,
                            singleLine    = true
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            animationOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text    = { Text(opt) },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        localAnimation = opt
                                        sharedViewModel.setAnimationStyle(opt)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── Scroll Text ────────────────────────────────────────────────
            SettingsCard {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.TextFields, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Scroll Text", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                        Text("Scroll long text across display", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked         = scrollText,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sharedViewModel.setScrollText(it) 
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick   = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    /* apply already sent per-change */ 
                },
                modifier  = Modifier.fillMaxWidth().height(48.dp),
                shape     = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Settings", fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content   = { content() }
    )
}

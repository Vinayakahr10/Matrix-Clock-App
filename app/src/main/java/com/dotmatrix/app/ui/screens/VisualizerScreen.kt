package com.dotmatrix.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerScreen(sharedViewModel: SharedConnectionViewModel) {
    val isPlaying by sharedViewModel.isVisualizerPlaying.collectAsState()
    val mode by sharedViewModel.visualizerMode.collectAsState()
    val sensitivity by sharedViewModel.sensitivity.collectAsState()
    val haptic = LocalHapticFeedback.current

    val spinTransition = rememberInfiniteTransition(label = "spin")
    val rotation by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "spin"
    )

    val modes = listOf(
        Triple("Frequency Bars", Icons.Outlined.Equalizer, "frequency"),
        Triple("Audio Waveform", Icons.Outlined.Waves, "waveform"),
        Triple("Radial Pulse", Icons.Outlined.RadioButtonChecked, "radial")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Visualizer", fontWeight = FontWeight.Medium) },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Large circular play button ──────────────────────────────────
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                Surface(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(88.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            sharedViewModel.toggleVisualizer() 
                        },
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Start",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(36.dp)
                                .let {
                                    if (isPlaying) it.rotate(rotation) else it
                                }
                        )
                    }
                }
            }
            Text(
                if (isPlaying) "Stop Visualizer" else "Start Visualizer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            // ── Mode section ────────────────────────────────────────────────
            Text(
                "Visualization Mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            )
            modes.forEach { (label, icon, key) ->
                val selected = mode == key
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            sharedViewModel.setVisualizerMode(key) 
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // left accent bar when selected
                        if (selected) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .height(24.dp)
                                    .padding(end = 0.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxSize()
                                ) {}
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Icon(
                            icon, contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Icon(
                                Icons.Outlined.Check, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Sensitivity slider ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Mic, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Microphone Sensitivity", style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${sensitivity}%", color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Slider(
                        value = sensitivity.toFloat(),
                        onValueChange = { 
                            if (it.toInt() != sensitivity) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            sharedViewModel.setSensitivity(it.toInt()) 
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

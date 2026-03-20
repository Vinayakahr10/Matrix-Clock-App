package com.dotmatrix.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualizerScreen(sharedViewModel: SharedConnectionViewModel) {
    val isConnected by sharedViewModel.isConnected.collectAsState()
    val isPlaying by sharedViewModel.isVisualizerPlaying.collectAsState()
    val mode by sharedViewModel.visualizerMode.collectAsState()
    val visualizerSource by sharedViewModel.visualizerSource.collectAsState()
    val deviceEvents by sharedViewModel.deviceEvents.collectAsState(initial = null)
    val phoneMicPermissionGranted by sharedViewModel.phoneMicPermissionGranted.collectAsState()
    val isPhoneMicStreaming by sharedViewModel.isPhoneMicStreaming.collectAsState()
    val sensitivity by sharedViewModel.sensitivity.collectAsState()
    val haptic = LocalHapticFeedback.current
    val phoneMicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        sharedViewModel.onPhoneMicPermissionResult(granted)
        if (granted) {
            sharedViewModel.sendVisualizerSourcePhone()
        }
    }

    LaunchedEffect(Unit) {
        sharedViewModel.refreshPhoneMicPermission()
    }

    DisposableEffect(Unit) {
        sharedViewModel.setVisualizerScreenVisible(true)
        onDispose {
            sharedViewModel.setVisualizerScreenVisible(false)
        }
    }

    val previewTransition = rememberInfiniteTransition(label = "visualizerPreview")
    val previewPhase by previewTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "previewPhase"
    )

    val modes = listOf(
        VisualizerModeCard("Frequency Bars", Icons.Outlined.Equalizer, "bars", "Stacked meter bars with punchy peaks."),
        VisualizerModeCard("Audio Waveform", Icons.Outlined.Waves, "waveform", "Smooth flowing line across the matrix."),
        VisualizerModeCard("Radial Pulse", Icons.Outlined.RadioButtonChecked, "radial", "Center pulse with circular energy bursts.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Music Visualizer", fontWeight = FontWeight.Medium) },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "Visualizer Live" else "Visualizer Standby",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isPlaying) {
                                    "Streaming ${modeLabel(mode)} from ${sourceLabel(visualizerSource)} with sensitivity $sensitivity%"
                                } else {
                                    "Choose a mode and start the visualizer."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isPlaying) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Outlined.GraphicEq else Icons.Outlined.Mic,
                                    contentDescription = null,
                                    tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    VisualizerPreviewCard(
                        mode = mode,
                        sensitivity = sensitivity,
                        isPlaying = isPlaying,
                        phase = previewPhase
                    )

                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            sharedViewModel.toggleVisualizer()
                        },
                        enabled = isConnected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(if (isPlaying) "Stop Visualizer" else "Start Visualizer")
                    }

                    if (!isConnected) {
                        Text(
                            text = "Connect to the clock to change visualizer source or controls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Visualizer Source",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose whether the matrix listens to the ESP32 mic or the phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = visualizerSource == "DEVICE",
                            onClick = {
                                if (!isConnected || visualizerSource == "DEVICE") return@SegmentedButton
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sharedViewModel.sendVisualizerSourceDevice()
                            },
                            enabled = isConnected,
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text("Device Mic") }
                        )
                        SegmentedButton(
                            selected = visualizerSource == "PHONE",
                            onClick = {
                                if (!isConnected || visualizerSource == "PHONE") return@SegmentedButton
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (phoneMicPermissionGranted) {
                                    sharedViewModel.sendVisualizerSourcePhone()
                                } else {
                                    phoneMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = isConnected,
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text("Phone Mic") }
                        )
                    }

                    Text(
                        text = when (visualizerSource) {
                            "PHONE" -> if (phoneMicPermissionGranted) {
                                if (isPhoneMicStreaming && isPlaying) {
                                    "Phone mic source selected. Audio frames are being sent to the matrix."
                                } else {
                                    "Phone mic source selected. Start the visualizer to stream phone audio."
                                }
                            } else {
                                "Phone mic source selected. Microphone permission is required to stream audio."
                            }
                            else -> "Device mic source selected. The ESP32 will use its onboard microphone."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (visualizerSource == "PHONE") {
                        Text(
                            text = if (phoneMicPermissionGranted) {
                                if (isPhoneMicStreaming) "Phone mic streaming is active" else "Phone mic is ready"
                            } else {
                                "Grant microphone permission to use Phone Mic"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (phoneMicPermissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    }

                    if (!deviceEvents.isNullOrBlank() &&
                        (
                            deviceEvents!!.contains("Visualizer source") ||
                            deviceEvents!!.contains("Visualizer frame") ||
                            deviceEvents!!.contains("Phone microphone")
                        )
                    ) {
                        Text(
                            text = deviceEvents!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (deviceEvents!!.contains("rejected") || deviceEvents!!.contains("denied")) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }

            Text(
                text = "Visualization Mode",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )

            modes.forEach { item ->
                val selected = mode == item.key
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isConnected) return@clickable
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                when (item.key) {
                                    "waveform" -> sharedViewModel.sendVisualizerStyleWave()
                                    "radial" -> sharedViewModel.sendVisualizerStyleRadial()
                                    else -> sharedViewModel.sendVisualizerStyleBars()
                                }
                            }
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (selected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Microphone Sensitivity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$sensitivity%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = sensitivityDescription(sensitivity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = sensitivity.toFloat(),
                        onValueChange = {
                            if (it.toInt() != sensitivity) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                sharedViewModel.setSensitivity(it.toInt())
                            }
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isConnected
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VisualizerPreviewCard(
    mode: String,
    sensitivity: Int,
    isPlaying: Boolean,
    phase: Float
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val softColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val lineColor = if (isPlaying) activeColor else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (mode) {
                    "waveform" -> {
                        val path = Path()
                        val widthStep = size.width / 14f
                        val centerY = size.height / 2f
                        for (i in 0..14) {
                            val x = i * widthStep
                            val y = centerY + kotlin.math.sin((i / 2f) + phase * 6f) * (size.height * (0.12f + sensitivity / 220f))
                            if (i == 0) path.moveTo(x, y.toFloat()) else path.lineTo(x, y.toFloat())
                        }
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                        )
                    }
                    "radial" -> {
                        val radiusBase = size.minDimension * 0.15f
                        val pulse = radiusBase + (size.minDimension * 0.12f * phase)
                        drawCircle(
                            color = softColor,
                            radius = pulse,
                            center = center
                        )
                        drawCircle(
                            color = lineColor,
                            radius = radiusBase * 0.78f,
                            center = center
                        )
                        drawCircle(
                            color = lineColor,
                            radius = pulse,
                            center = center,
                            style = Stroke(width = 10f)
                        )
                    }
                    else -> {
                        val barCount = 12
                        val gap = 10f
                        val barWidth = (size.width - gap * (barCount - 1)) / barCount
                        repeat(barCount) { index ->
                            val normalized = (0.28f + ((index % 4) * 0.08f) + phase * 0.35f).coerceAtMost(1f)
                            val barHeight = size.height * normalized * (0.45f + sensitivity / 180f)
                            val left = index * (barWidth + gap)
                            drawRoundRect(
                                color = if (isPlaying) activeColor else softColor,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(14f, 14f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sensitivityDescription(value: Int): String = when {
    value < 25 -> "Low sensitivity for loud environments."
    value < 60 -> "Balanced response for everyday listening."
    value < 85 -> "High sensitivity for softer music details."
    else -> "Very reactive mode for subtle beats and vocals."
}

private fun modeLabel(mode: String): String = when (mode) {
    "waveform" -> "Audio Waveform"
    "radial" -> "Radial Pulse"
    else -> "Frequency Bars"
}

private fun sourceLabel(source: String): String = when (source) {
    "PHONE" -> "Phone Mic"
    else -> "Device Mic"
}

private data class VisualizerModeCard(
    val label: String,
    val icon: ImageVector,
    val key: String,
    val description: String
)

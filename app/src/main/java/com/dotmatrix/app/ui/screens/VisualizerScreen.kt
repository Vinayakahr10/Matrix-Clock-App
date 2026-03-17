package com.dotmatrix.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.ui.theme.TextSecondary
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

data class VisMode(val id: String, val name: String, val icon: ImageVector)

@Composable
fun VisualizerScreen(sharedViewModel: SharedConnectionViewModel) {
    val isPlaying by sharedViewModel.isVisualizerActive.collectAsState()
    val activeMode by sharedViewModel.visualizerMode.collectAsState()
    var sensitivity by remember { mutableFloatStateOf(50f) }

    val modes = listOf(
        VisMode("bars", "Frequency Bars", Icons.Default.GraphicEq),
        VisMode("wave", "Audio Waveform", Icons.Default.Waves),
        VisMode("pulse", "Radial Pulse", Icons.Default.WifiTethering)
    )

    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 1f,
        animationSpec = tween(300),
        label = "Play Button Scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Music Visualizer",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Play Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .shadow(if (isPlaying) 16.dp else 4.dp, CircleShape)
                        .clickable { sharedViewModel.toggleVisualizer() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isPlaying) "Visualizer Running" else "Start Visualizer",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else TextSecondary
                )
            }
        }

        Text(
            text = "Visualization Mode",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Modes Config
        modes.forEach { mode ->
            val selected = activeMode == mode.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (selected) Color.Transparent else Color(0xFFE2E8F0),
                        RoundedCornerShape(8.dp)
                    )
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.White)
                    .clickable { sharedViewModel.setVisualizerMode(mode.id) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = mode.name,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = mode.name,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sensitivity Slider
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Microphone Sensitivity", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                    Text("${sensitivity.toInt()}%", fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = sensitivity,
                    onValueChange = { 
                        sensitivity = it
                        sharedViewModel.setVisualizerSensitivity(it.toInt())
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color(0xFFE2E8F0)
                    )
                )
            }
        }
    }
}

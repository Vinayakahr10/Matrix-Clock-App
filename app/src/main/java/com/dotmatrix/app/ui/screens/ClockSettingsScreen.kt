package com.dotmatrix.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dotmatrix.app.ui.theme.TextSecondary
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsScreen(
    sharedViewModel: SharedConnectionViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("Clock", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) }
                )
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Alarms") },
                        icon = { Icon(Icons.Default.Alarm, contentDescription = null) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            if (selectedTab == 0) {
                SettingsContent(sharedViewModel)
            } else {
                AlarmTimerScreen(sharedViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(sharedViewModel: SharedConnectionViewModel) {
    val is24HourFormat by sharedViewModel.is24HourFormat.collectAsState()
    var brightness by remember { mutableFloatStateOf(80f) }
    var scrollText by remember { mutableStateOf(true) }
    var animationExpanded by remember { mutableStateOf(false) }
    var selectedAnimation by remember { mutableStateOf("Solid (No Animation)") }
    val animations = listOf("Solid (No Animation)", "Fade Transition", "Slide Up", "Typing Effect")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        SettingsCard(
            title = "Time Format",
            icon = Icons.Default.Schedule
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("24-Hour Format", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                    Text("Switch between 12h and 24h", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = is24HourFormat,
                    onCheckedChange = { sharedViewModel.set24HourFormat(it) }
                )
            }
        }

        SettingsCard(
            title = "Brightness",
            valueText = "${brightness.toInt()}%",
            icon = Icons.Default.BrightnessMedium
        ) {
            Slider(
                value = brightness,
                onValueChange = { 
                    brightness = it
                    sharedViewModel.setBrightness(it.toInt())
                },
                valueRange = 0f..100f
            )
        }

        SettingsCard(
            title = "Animation Style",
            icon = Icons.Default.Settings
        ) {
            Column {
                ExposedDropdownMenuBox(
                    expanded = animationExpanded,
                    onExpandedChange = { animationExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedTextField(
                        value = selectedAnimation,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = animationExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = animationExpanded,
                        onDismissRequest = { animationExpanded = false }
                    ) {
                        animations.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    selectedAnimation = selectionOption
                                    animationExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Scroll Text", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                            Text("Move text for long messages", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Switch(
                        checked = scrollText,
                        onCheckedChange = { scrollText = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { /* Save Settings */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Apply Settings", style = MaterialTheme.typography.titleSmall)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SettingsCard(
    title: String,
    valueText: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (valueText != null) {
                    Text(valueText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            content()
        }
    }
}

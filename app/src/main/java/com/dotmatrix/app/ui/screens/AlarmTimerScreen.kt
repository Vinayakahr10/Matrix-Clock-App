package com.dotmatrix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.ui.theme.Typography
import com.dotmatrix.app.viewmodel.Alarm
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

enum class ActiveTab(val label: String, val icon: ImageVector) {
    Settings("Settings", Icons.Outlined.Settings),
    Alarms("Alarms", Icons.Outlined.Alarm),
    Timers("Timers", Icons.Outlined.Timer),
    Stopwatch("Stopwatch", Icons.Outlined.Schedule)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTimerScreen(
    sharedViewModel: SharedConnectionViewModel,
    showAlarmTimePickerExternal: Boolean = false,
    onTimePickerDismissed: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(ActiveTab.Settings) }
    val alarms by sharedViewModel.alarms.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(showAlarmTimePickerExternal) {
        if (showAlarmTimePickerExternal) {
            activeTab = ActiveTab.Alarms
            showTimePicker = true
        }
    }

    val timerSeconds by sharedViewModel.timerSeconds.collectAsState()
    val initialTimerSeconds by sharedViewModel.initialTimerSeconds.collectAsState()
    val isTimerRunning by sharedViewModel.isTimerRunning.collectAsState()
    val stopwatchMillis by sharedViewModel.stopwatchMillis.collectAsState()
    val isSwRunning by sharedViewModel.isStopwatchRunning.collectAsState()

    val is24H by sharedViewModel.is24HourFormat.collectAsState()
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = is24H
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = { Text("Clock", style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                TabRow(
                    selectedTabIndex = activeTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    ActiveTab.values().forEach { tab ->
                        Tab(
                            selected = activeTab == tab,
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeTab = tab 
                            },
                            text = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(tab.icon, null) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab == ActiveTab.Alarms) {
                FloatingActionButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showTimePicker = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (activeTab) {
                ActiveTab.Settings -> ClockSettingsContent(sharedViewModel, haptic)
                ActiveTab.Alarms -> AlarmsContent(
                    alarms = alarms,
                    onToggle = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sharedViewModel.toggleAlarm(it) 
                    },
                    onDelete = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sharedViewModel.deleteAlarm(it) 
                    }
                )
                ActiveTab.Timers -> TimerContent(
                    secondsLeft = timerSeconds,
                    initialSeconds = initialTimerSeconds,
                    isRunning = isTimerRunning,
                    onStart = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sharedViewModel.startTimer(it) 
                    },
                    onStop = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sharedViewModel.stopTimer() 
                    },
                    onReset = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sharedViewModel.resetTimer() 
                    },
                    onAddSeconds = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sharedViewModel.addTimerSeconds(it) 
                    },
                    haptic = haptic
                )
                ActiveTab.Stopwatch -> StopwatchContent(
                    millis = stopwatchMillis,
                    isRunning = isSwRunning,
                    onStart = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sharedViewModel.startStopwatch() 
                    },
                    onPause = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sharedViewModel.pauseStopwatch() 
                    },
                    onReset = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sharedViewModel.resetStopwatch() 
                    }
                )
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismiss = { 
                showTimePicker = false
                onTimePickerDismissed()
            },
            onConfirm = {
                val hour = timePickerState.hour
                val minute = timePickerState.minute
                val timeStr = if (is24H) {
                    "%02d:%02d".format(hour, minute)
                } else {
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = if (hour % 12 == 0) 12 else hour % 12
                    "%d:%02d %s".format(displayHour, minute, amPm)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                sharedViewModel.addAlarm(timeStr, "Alarm")
                showTimePicker = false
                onTimePickerDismissed()
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsContent(sharedViewModel: SharedConnectionViewModel, haptic: HapticFeedback) {
    val is24H by sharedViewModel.is24HourFormat.collectAsState()
    val brightness by sharedViewModel.brightness.collectAsState()
    val animation by sharedViewModel.animationStyle.collectAsState()
    val scrollText by sharedViewModel.scrollText.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsItemCard(
            icon = Icons.Outlined.Schedule,
            title = "Time Format",
            subtitle = "Switch between 12h and 24h display",
            action = {
                Switch(checked = is24H, onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sharedViewModel.setTimeFormat(it) 
                })
            }
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Brightness", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${(brightness * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = brightness, 
                    onValueChange = { 
                        if ((it * 100).toInt() != (brightness * 100).toInt()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        sharedViewModel.setBrightness(it) 
                    },
                    valueRange = 0.1f..1f
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Settings, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Animation Style", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(8.dp))

                val animationOptions = listOf("None", "Wave", "Fade", "Scroll", "Rain")
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = animation,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        animationOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    sharedViewModel.setAnimationStyle(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SyncAlt, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Scroll Text", style = MaterialTheme.typography.titleMedium)
                        Text("Enable for long messages", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = scrollText, onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sharedViewModel.setScrollText(it) 
                    })
                }
            }
        }
        
        Button(
            onClick = { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                sharedViewModel.syncTime() 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Sync, null)
            Spacer(Modifier.width(8.dp))
            Text("Sync Time with Phone")
        }
    }
}

@Composable
fun SettingsItemCard(icon: ImageVector, title: String, subtitle: String, action: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action()
        }
    }
}

@Composable
fun AlarmsContent(alarms: List<Alarm>, onToggle: (Alarm) -> Unit, onDelete: (String) -> Unit) {
    if (alarms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No alarms set", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alarms) { alarm ->
                AlarmItem(alarm, onToggle, onDelete)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AlarmItem(alarm: Alarm, onToggle: (Alarm) -> Unit, onDelete: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(alarm.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(alarm.time, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
            Switch(checked = alarm.active, onCheckedChange = { onToggle(alarm) })
            IconButton(onClick = { onDelete(alarm.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TimerContent(
    secondsLeft: Int, 
    initialSeconds: Int,
    isRunning: Boolean, 
    onStart: (Int) -> Unit, 
    onStop: () -> Unit,
    onReset: () -> Unit,
    onAddSeconds: (Int) -> Unit,
    haptic: HapticFeedback
) {
    val h = secondsLeft / 3600
    val m = (secondsLeft % 3600) / 60
    val s = secondsLeft % 60
    
    val presets = listOf(60, 300, 600, 1800, 3600) // 1m, 5m, 10m, 30m, 1h
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
            val sweepAngle = if (initialSeconds > 0) (secondsLeft.toFloat() / initialSeconds) * 360f else 0f
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.2f), 
                    startAngle = -90f, sweepAngle = 360f, useCenter = false, 
                    style = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                )
                if (sweepAngle > 0) {
                    drawArc(
                        color = Color(0xFF1A73E8), 
                        startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, 
                        style = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp)
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        if (!isRunning && secondsLeft == 0) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { seconds ->
                    Button(
                        onClick = { onStart(seconds) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Text(if (seconds < 3600) "${seconds/60}m" else "1h")
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { onAddSeconds(60) },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("+1:00")
                }
                
                FloatingActionButton(
                    onClick = { if (isRunning) onStop() else onStart(secondsLeft) },
                    containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                }
                
                Button(
                    onClick = onReset,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
fun StopwatchContent(millis: Long, isRunning: Boolean, onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit) {
    val h = (millis / 3600000) % 24
    val m = (millis / 60000) % 60
    val s = (millis / 1000) % 60
    val ms = (millis % 1000) / 10
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp), 
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = ".%02d".format(ms),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        Spacer(Modifier.height(64.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onReset, 
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(28.dp))
            }
            
            FloatingActionButton(
                onClick = { if (isRunning) onPause() else onStart() }, 
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape, 
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, 
                    contentDescription = null, 
                    modifier = Modifier.size(36.dp)
                )
            }
            
            // Empty placeholder for symmetry
            Spacer(Modifier.size(64.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("OK", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                content()
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

package com.dotmatrix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.NumberPicker
import com.dotmatrix.app.ui.theme.Typography
import com.dotmatrix.app.viewmodel.Alarm
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel
import java.time.LocalTime
import kotlin.math.roundToInt

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
    val isConnected by sharedViewModel.isConnected.collectAsState()
    val currentClockTool by sharedViewModel.currentClockTool.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingAlarmLabel by remember { mutableStateOf("Alarm") }
    var pendingHour by remember { mutableIntStateOf(8) }
    var pendingMinute by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(currentClockTool) {
        activeTab = when (currentClockTool) {
            "CLOCK" -> ActiveTab.Settings
            "ALARM" -> ActiveTab.Alarms
            "TIMER" -> ActiveTab.Timers
            "STOPWATCH" -> ActiveTab.Stopwatch
            else -> activeTab
        }
    }

    LaunchedEffect(showAlarmTimePickerExternal) {
        if (showAlarmTimePickerExternal) {
            activeTab = ActiveTab.Alarms
            val now = LocalTime.now().plusMinutes(1)
            pendingHour = now.hour
            pendingMinute = now.minute
            pendingAlarmLabel = "Alarm"
            showTimePicker = true
        }
    }

    val timerSeconds by sharedViewModel.timerSeconds.collectAsState()
    val initialTimerSeconds by sharedViewModel.initialTimerSeconds.collectAsState()
    val isTimerRunning by sharedViewModel.isTimerRunning.collectAsState()
    val stopwatchMillis by sharedViewModel.stopwatchMillis.collectAsState()
    val isSwRunning by sharedViewModel.isStopwatchRunning.collectAsState()

    val is24H by sharedViewModel.is24HourFormat.collectAsState()

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
                        val now = LocalTime.now().plusMinutes(1)
                        pendingHour = now.hour
                        pendingMinute = now.minute
                        pendingAlarmLabel = "Alarm"
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
            Column(modifier = Modifier.fillMaxSize()) {
                ClockToolStatusCard(
                    isConnected = isConnected,
                    currentClockTool = currentClockTool
                )
                Box(modifier = Modifier.weight(1f)) {
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
                    onSetDuration = {
                        sharedViewModel.setTimerDuration(it)
                    },
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
                    },
                    onLap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sharedViewModel.lapStopwatch()
                    }
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        AlarmDialog(
            label = pendingAlarmLabel,
            onLabelChange = { pendingAlarmLabel = it },
            is24Hour = is24H,
            hour = pendingHour,
            minute = pendingMinute,
            onHourChange = { pendingHour = it },
            onMinuteChange = { pendingMinute = it },
            onDismiss = { 
                showTimePicker = false
                onTimePickerDismissed()
            },
            onConfirm = {
                val hour = pendingHour
                val minute = pendingMinute
                val timeStr = if (is24H) {
                    "%02d:%02d".format(hour, minute)
                } else {
                    val amPm = if (hour < 12) "AM" else "PM"
                    val displayHour = if (hour % 12 == 0) 12 else hour % 12
                    "%d:%02d %s".format(displayHour, minute, amPm)
                }
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                sharedViewModel.addAlarm(
                    time = timeStr,
                    label = pendingAlarmLabel.trim().ifBlank { "Alarm" }
                )
                showTimePicker = false
                onTimePickerDismissed()
            }
        )
    }
}

@Composable
private fun ClockToolStatusCard(
    isConnected: Boolean,
    currentClockTool: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Clock Tool",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isConnected) {
                    currentClockTool.lowercase().replaceFirstChar { it.uppercase() }
                } else {
                    "Connect to sync the active tool"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isConnected) {
                    "This follows the firmware clock submenu state reported in INFO."
                } else {
                    "The app will update this after reconnecting and requesting device info."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockSettingsContent(sharedViewModel: SharedConnectionViewModel, haptic: HapticFeedback) {
    val isConnected by sharedViewModel.isConnected.collectAsState()
    val is24H by sharedViewModel.is24HourFormat.collectAsState()
    val timeFormatStatusMessage by sharedViewModel.timeFormatStatusMessage.collectAsState()
    val brightness by sharedViewModel.brightness.collectAsState()
    val animation by sharedViewModel.animationStyle.collectAsState()
    val scrollText by sharedViewModel.scrollText.collectAsState()
    var localBrightness by remember(brightness) { mutableFloatStateOf(brightness.toFloat()) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Time Format", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Choose how the clock time is displayed on the matrix",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = is24H,
                        onClick = {
                            if (!isConnected || is24H) return@SegmentedButton
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sharedViewModel.setTimeFormat(true)
                        },
                        enabled = isConnected,
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("24 Hour") }
                    )
                    SegmentedButton(
                        selected = !is24H,
                        onClick = {
                            if (!isConnected || !is24H) return@SegmentedButton
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            sharedViewModel.setTimeFormat(false)
                        },
                        enabled = isConnected,
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("12 Hour") }
                    )
                }

                Text(
                    text = if (is24H) "Example: 18:45" else "Example: 06:45",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!timeFormatStatusMessage.isNullOrBlank()) {
                    Text(
                        text = timeFormatStatusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (timeFormatStatusMessage == "Invalid time format selected") {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                if (!isConnected) {
                    Text(
                        text = "Connect to the clock to change the display format.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    Icon(Icons.Outlined.WbSunny, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("Brightness", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${localBrightness.toInt()}", color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = localBrightness,
                    onValueChange = { 
                        val snapped = it.roundToInt().toFloat()
                        if (snapped.toInt() != localBrightness.toInt()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        localBrightness = snapped
                    },
                    onValueChangeFinished = {
                        sharedViewModel.setBrightness(localBrightness.toInt())
                    },
                    valueRange = 0f..15f,
                    steps = 14,
                    enabled = isConnected
                )
                Text(
                    text = "Brightness: ${localBrightness.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                sharedViewModel.sendCurrentTimeSync()
            },
            enabled = isConnected,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "No alarms yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap the + button to add your first alarm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
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
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.active) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            }
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (alarm.active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (alarm.active) Icons.Outlined.AlarmOn else Icons.Outlined.AlarmOff,
                        contentDescription = null,
                        tint = if (alarm.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alarm.label.ifBlank { "Alarm" },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (alarm.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    alarm.time,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (alarm.active) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
    onSetDuration: (Int) -> Unit,
    onStart: (Int) -> Unit, 
    onStop: () -> Unit,
    onReset: () -> Unit,
    onAddSeconds: (Int) -> Unit,
    haptic: HapticFeedback
) {
    val h = secondsLeft / 3600
    val m = (secondsLeft % 3600) / 60
    val s = secondsLeft % 60

    var configuredHours by remember { mutableIntStateOf(0) }
    var configuredMinutes by remember { mutableIntStateOf(5) }
    var configuredSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning, secondsLeft) {
        if (!isRunning) {
            val source = if (secondsLeft > 0) secondsLeft else configuredHours * 3600 + configuredMinutes * 60 + configuredSeconds
            configuredHours = source / 3600
            configuredMinutes = (source % 3600) / 60
            configuredSeconds = source % 60
        }
    }

    val configuredTotalSeconds = configuredHours * 3600 + configuredMinutes * 60 + configuredSeconds
    val presets = listOf(60, 300, 600, 900, 1800, 3600)
    val quickAdds = listOf(30, 60, 300)
    val timerTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val timerProgressColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(260.dp)) {
            val sweepAngle = if (initialSeconds > 0) (secondsLeft.toFloat() / initialSeconds) * 360f else 0f
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = timerTrackColor,
                    startAngle = -90f, sweepAngle = 360f, useCenter = false, 
                    style = Stroke(12.dp.toPx(), cap = StrokeCap.Round)
                )
                if (sweepAngle > 0) {
                    drawArc(
                        color = timerProgressColor,
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isRunning) "Timer running" else if (secondsLeft > 0) "Paused" else "Ready to start",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Set Timer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeWheel(
                            label = "Hours",
                            values = (0..23).toList(),
                            selectedValue = configuredHours,
                            formatter = { "%02d".format(it) },
                            onValueChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                configuredHours = it
                                onSetDuration(configuredHours * 3600 + configuredMinutes * 60 + configuredSeconds)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TimeWheel(
                            label = "Minutes",
                            values = (0..59).toList(),
                            selectedValue = configuredMinutes,
                            formatter = { "%02d".format(it) },
                            onValueChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                configuredMinutes = it
                                onSetDuration(configuredHours * 3600 + configuredMinutes * 60 + configuredSeconds)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TimeWheel(
                            label = "Seconds",
                            values = (0..59).toList(),
                            selectedValue = configuredSeconds,
                            formatter = { "%02d".format(it) },
                            onValueChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                configuredSeconds = it
                                onSetDuration(configuredHours * 3600 + configuredMinutes * 60 + configuredSeconds)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "Quick presets",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                    ) {
                        items(presets) { presetSeconds ->
                            FilterChip(
                                selected = configuredTotalSeconds == presetSeconds,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    configuredHours = presetSeconds / 3600
                                    configuredMinutes = (presetSeconds % 3600) / 60
                                    configuredSeconds = presetSeconds % 60
                                    onSetDuration(presetSeconds)
                                },
                                label = { Text(formatTimerPreset(presetSeconds)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Button(
                        onClick = { onStart(configuredTotalSeconds) },
                        enabled = configuredTotalSeconds > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (secondsLeft > 0) "Start Selected Timer" else "Start Timer")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Quick Add",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickAdds.forEach { extra ->
                            AssistChip(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAddSeconds(extra)
                                },
                                label = { Text("+${formatQuickAdd(extra)}") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reset")
                        }

                        Button(
                            onClick = { if (isRunning) onStop() else onStart(secondsLeft) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = if (isRunning) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimary
                                }
                            )
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isRunning) "Pause" else "Resume")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun StopwatchContent(
    millis: Long,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    val h = (millis / 3600000) % 24
    val m = (millis / 60000) % 60
    val s = (millis / 1000) % 60
    val ms = (millis % 1000) / 10

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = if (isRunning) "Stopwatch running" else if (millis > 0L) "Paused" else "Ready",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

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

                Text(
                    text = "Hours  Minutes  Seconds",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = when {
                        isRunning -> "Running"
                        millis > 0L -> "Paused at ${if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)}.%02d".format(ms)
                        else -> "Ready when you are"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = millis > 0L
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reset")
            }

            Button(
                onClick = { if (isRunning) onPause() else onStart() },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isRunning) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isRunning) "Pause" else if (millis > 0L) "Resume" else "Start")
            }
        }

        FilledTonalButton(
            onClick = onLap,
            enabled = millis > 0L,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Outlined.Flag, null)
            Spacer(Modifier.width(8.dp))
            Text("Lap")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDialog(
    label: String,
    onLabelChange: (String) -> Unit,
    is24Hour: Boolean,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AlarmAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "New Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                    },
                    label = { Text("Alarm label") },
                    placeholder = { Text("Alarm") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                AlarmTimeEditor(
                    is24Hour = is24Hour,
                    hour = hour,
                    minute = minute,
                    onHourChange = onHourChange,
                    onMinuteChange = onMinuteChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(onClick = onConfirm) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmTimeEditor(
    is24Hour: Boolean,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var hourInput by remember(hour, is24Hour) {
        mutableStateOf("%02d".format(if (is24Hour) hour else hour.to12Hour()))
    }
    var minuteInput by remember(minute) {
        mutableStateOf("%02d".format(minute))
    }
    val displayHour = if (is24Hour) hour else hour.to12Hour()
    val isAm = hour < 12

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeWheel(
                    label = "Hour",
                    values = if (is24Hour) (0..23).toList() else (1..12).toList(),
                    selectedValue = displayHour,
                    formatter = { "%02d".format(it) },
                    onValueChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (is24Hour) onHourChange(it) else onHourChange(hour.with12Hour(it))
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TimeWheel(
                    label = "Minute",
                    values = (0..59).toList(),
                    selectedValue = minute,
                    formatter = { "%02d".format(it) },
                    onValueChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onMinuteChange(it)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!is24Hour) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = isAm,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onHourChange(hour.withMeridiem(isAm = true))
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("AM") }
                    )
                    SegmentedButton(
                        selected = !isAm,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onHourChange(hour.withMeridiem(isAm = false))
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("PM") }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = hourInput,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter(Char::isDigit).take(2)
                        hourInput = filtered
                        filtered.toIntOrNull()?.let { typedHour ->
                            val validHour = if (is24Hour) {
                                typedHour in 0..23
                            } else {
                                typedHour in 1..12
                            }
                            if (validHour) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (is24Hour) onHourChange(typedHour) else onHourChange(hour.with12Hour(typedHour))
                            }
                        }
                    },
                    label = { Text("Type hour") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = minuteInput,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter(Char::isDigit).take(2)
                        minuteInput = filtered
                        filtered.toIntOrNull()?.let { typedMinute ->
                            if (typedMinute in 0..59) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onMinuteChange(typedMinute)
                            }
                        }
                    },
                    label = { Text("Type minute") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Text(
                text = if (is24Hour) {
                    "Selected time: %02d:%02d".format(hour, minute)
                } else {
                    "Selected time: %d:%02d %s".format(displayHour, minute, if (isAm) "AM" else "PM")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeWheel(
    label: String,
    values: List<Int>,
    selectedValue: Int,
    formatter: (Int) -> String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
        ) {
            AndroidView(
                factory = { context ->
                    NumberPicker(context).apply {
                        minValue = 0
                        maxValue = values.lastIndex
                        displayedValues = values.map(formatter).toTypedArray()
                        wrapSelectorWheel = true
                        descendantFocusability = NumberPicker.FOCUS_AFTER_DESCENDANTS
                        setOnValueChangedListener { _, _, newVal ->
                            onValueChange(values[newVal])
                        }
                    }
                },
                update = { picker ->
                    picker.displayedValues = null
                    picker.minValue = 0
                    picker.maxValue = values.lastIndex
                    picker.displayedValues = values.map(formatter).toTypedArray()
                    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
                    if (picker.value != selectedIndex) {
                        picker.value = selectedIndex
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun Int.with12Hour(displayHour: Int): Int {
    val normalized = when {
        displayHour <= 0 -> 12
        displayHour > 12 -> ((displayHour - 1) % 12) + 1
        else -> displayHour
    }
    val isCurrentlyAm = this < 12
    return if (isCurrentlyAm) {
        if (normalized == 12) 0 else normalized
    } else {
        if (normalized == 12) 12 else normalized + 12
    }
}

private fun Int.to12Hour(): Int = when (val mod = this % 12) {
    0 -> 12
    else -> mod
}

private fun Int.withMeridiem(isAm: Boolean): Int {
    val baseHour = this.to12Hour()
    return if (isAm) {
        if (baseHour == 12) 0 else baseHour
    } else {
        if (baseHour == 12) 12 else baseHour + 12
    }
}

private fun Int.adjust12Hour(delta: Int): Int {
    val isAm = this < 12
    val current12 = this.to12Hour()
    val next12 = (((current12 - 1 + delta) % 12) + 12) % 12 + 1
    return when {
        isAm && next12 == 12 && delta > 0 -> 12
        isAm && current12 == 12 && next12 == 11 && delta < 0 -> 11
        isAm -> if (next12 == 12) 0 else next12
        else -> if (next12 == 12) 12 else next12 + 12
    }
}

private fun formatTimerPreset(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun formatQuickAdd(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes > 0 && seconds > 0 -> "${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

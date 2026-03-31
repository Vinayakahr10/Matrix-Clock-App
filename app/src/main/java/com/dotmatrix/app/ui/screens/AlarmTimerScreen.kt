package com.dotmatrix.app.ui.screens

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.NumberPicker
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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

    val headerAlpha by animateFloatAsState(
        targetValue = if (scrollBehavior.state.collapsedFraction > 0.05f) 0.85f else 0f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "headerAlpha"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = headerAlpha))
            ) {
                LargeTopAppBar(
                    title = { 
                        Text(
                            "Clock", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displaySmall
                        ) 
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
                // ── Segmented Pill Navigation ────────────────────────────────
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shadowElevation = 2.dp
                ) {
                    Box(modifier = Modifier.padding(4.dp)) {
                        val tabs = ActiveTab.values()
                        val selectedIndex = tabs.indexOf(activeTab)
                        
                        BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                            val tabWidth = maxWidth / tabs.size
                            val indicatorOffset by animateDpAsState(
                                targetValue = tabWidth * selectedIndex,
                                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                                label = "indicatorOffset"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .offset(x = indicatorOffset)
                                    .width(tabWidth)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            tabs.forEach { tab ->
                                val isSelected = activeTab == tab
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, 
                                    animationSpec = tween(durationMillis = 250),
                                    label = "textColor"
                                )
                                
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { 
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            activeTab = tab 
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (activeTab == ActiveTab.Alarms) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val now = LocalTime.now().plusMinutes(1)
                        pendingHour = now.hour
                        pendingMinute = now.minute
                        pendingAlarmLabel = "Alarm"
                        showTimePicker = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("New Alarm", fontWeight = FontWeight.Bold) }
                )
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
                                sharedViewModel.deleteAlarm(it.id) 
                            }
                        )
                        ActiveTab.Timers -> TimerContent(
                            secondsLeft = timerSeconds,
                            initialSeconds = initialTimerSeconds,
                            isRunning = isTimerRunning,
                            onSetDuration = { sharedViewModel.setTimerDuration(it) },
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
    val statusColor = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        statusColor.copy(alpha = 0.2f),
                        statusColor.copy(alpha = 0.05f)
                    ),
                    radius = 400f
                )
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isConnected) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (isConnected) "Clock Connected" else "Disconnected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Active Mode: $currentClockTool",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isConnected) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockSettingsContent(
    sharedViewModel: SharedConnectionViewModel,
    haptic: HapticFeedback
) {
    val is24H by sharedViewModel.is24HourFormat.collectAsState()
    val scrollText by sharedViewModel.scrollText.collectAsState()
    val brightness by sharedViewModel.brightness.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(title = "Display Options") {
            SettingsToggleRow(
                title = "24-Hour Format",
                subtitle = "Toggle between 12h and 24h",
                icon = Icons.Outlined.Schedule,
                checked = is24H,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sharedViewModel.setTimeFormat(it) 
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            SettingsToggleRow(
                title = "Scroll Text",
                subtitle = "Enable scrolling for long text",
                icon = Icons.Outlined.Timelapse,
                checked = scrollText,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sharedViewModel.setScrollText(it) 
                }
            )
        }

        SettingsSection(title = "Brightness") {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${(brightness.toFloat() / 15f * 100).roundToInt()}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = brightness.toFloat(),
                    valueRange = 0f..15f,
                    onValueChange = { sharedViewModel.setBrightness(it.roundToInt()) },
                    onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "settingsScale"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable { 
                isPressed = true
                onCheckedChange(!checked) 
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AlarmsContent(
    alarms: List<Alarm>,
    onToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit
) {
    if (alarms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        Icons.Outlined.AlarmOff, 
                        null, 
                        modifier = Modifier.padding(24.dp), 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "No alarms set", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tap + to create a new alarm", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(alarms) { alarm ->
                AlarmItem(alarm = alarm, onToggle = { onToggle(alarm) }, onDelete = { onDelete(alarm) })
            }
        }
    }
}

@Composable
private fun AlarmItem(
    alarm: Alarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "alarmScale"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                             else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarm.active) 0.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
                // A quick flash of the press state purely for visual feedback since clickable consumes pointer
                isPressed = true
            }
    ) {
        // Quick coroutine to reset the 'isPressed' state since standard clickable doesn't expose press state easily
        LaunchedEffect(isPressed) {
            if (isPressed) {
                kotlinx.coroutines.delay(100)
                isPressed = false
            }
        }
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (alarm.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = alarm.active, 
                    onCheckedChange = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToggle()
                    }
                )
            }
        }
    }
}

@Composable
private fun TimerContent(
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
    var showPresetPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
            val progress = if (initialSeconds > 0) secondsLeft.toFloat() / initialSeconds else 0f
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
                label = "TimerProgress"
            )

            val primaryColor = MaterialTheme.colorScheme.primary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                drawArc(
                    color = primaryColor.copy(alpha = 0.1f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                val sweepGradient = Brush.sweepGradient(
                    0.0f to tertiaryColor,
                    0.5f to primaryColor,
                    1.0f to tertiaryColor
                )
                
                drawArc(
                    brush = sweepGradient,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(secondsLeft),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (initialSeconds > 0) {
                    Text(
                        text = "Total: ${formatTime(initialSeconds)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onReset,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Refresh, "Reset", modifier = Modifier.padding(16.dp))
            }

            Button(
                onClick = { 
                    if (isRunning) onStop() 
                    else if (secondsLeft > 0) onStart(secondsLeft)
                    else showPresetPicker = true
                },
                modifier = Modifier
                    .height(64.dp)
                    .width(160.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(if (isRunning) "PAUSE" else "START", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }

            Surface(
                onClick = { showPresetPicker = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Outlined.Timer, "Set", modifier = Modifier.padding(16.dp))
            }
        }

        if (isRunning) {
            Spacer(modifier = Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(30, 60, 300).forEach { secs ->
                    Surface(
                        onClick = { onAddSeconds(secs) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            "+${formatQuickAdd(secs)}",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    if (showPresetPicker) {
        TimerPresetDialog(
            onDismiss = { showPresetPicker = false },
            onSelected = { 
                onSetDuration(it)
                showPresetPicker = false 
            }
        )
    }
}

@Composable
private fun TimerPresetDialog(
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    val presets = listOf(
        60, 120, 300, 600, 900, 1800, 3600, 7200
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Set Timer", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(24.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(300.dp)
                ) {
                    items(presets) { seconds ->
                        Button(
                            onClick = { onSelected(seconds) },
                            modifier = Modifier.height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(formatTimerPreset(seconds))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun StopwatchContent(
    millis: Long,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.5f))
        
        Text(
            text = formatMillis(millis),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-4).sp
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onReset,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Refresh, "Reset", modifier = Modifier.padding(20.dp))
            }

            Surface(
                onClick = if (isRunning) onPause else onStart,
                shape = CircleShape,
                color = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.size(96.dp)
            ) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, 
                    null,
                    modifier = Modifier.padding(24.dp),
                    tint = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Surface(
                onClick = onLap,
                enabled = isRunning,
                shape = CircleShape,
                color = if (isRunning) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.SyncAlt, 
                    "Lap", 
                    modifier = Modifier.padding(20.dp),
                    tint = if (isRunning) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    val ms = (millis % 1000) / 10
    return "%02d:%02d.%02d".format(m, s, ms)
}

@Composable
private fun AlarmDialog(
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
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New Alarm",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hours = if (is24Hour) (0..23).toList() else (1..12).toList()
                    val minutes = (0..59).toList()

                    NumberPickerView(
                        label = "Hour",
                        selectedValue = if (is24Hour) hour else (if (hour % 12 == 0) 12 else hour % 12),
                        values = hours,
                        onValueChange = { newVal ->
                            if (is24Hour) {
                                onHourChange(newVal)
                            } else {
                                val isPm = hour >= 12
                                val actualHour = if (isPm) {
                                    if (newVal == 12) 12 else newVal + 12
                                } else {
                                    if (newVal == 12) 0 else newVal
                                }
                                onHourChange(actualHour)
                            }
                        },
                        formatter = { "%02d".format(it) },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        ":",
                        style = MaterialTheme.typography.displayMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    NumberPickerView(
                        label = "Minute",
                        selectedValue = minute,
                        values = minutes,
                        onValueChange = onMinuteChange,
                        formatter = { "%02d".format(it) },
                        modifier = Modifier.weight(1f)
                    )

                    if (!is24Hour) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            val isPm = hour >= 12
                            AmPmButton(selected = !isPm, label = "AM") { onHourChange(hour % 12) }
                            AmPmButton(selected = isPm, label = "PM") { onHourChange((hour % 12) + 12) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Alarm")
                    }
                }
            }
        }
    }
}

@Composable
private fun AmPmButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(width = 48.dp, height = 36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NumberPickerView(
    label: String,
    selectedValue: Int,
    values: List<Int>,
    onValueChange: (Int) -> Unit,
    formatter: (Int) -> String,
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

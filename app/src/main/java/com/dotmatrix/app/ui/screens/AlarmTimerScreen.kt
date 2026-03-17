package com.dotmatrix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.ui.theme.TextSecondary
import com.dotmatrix.app.viewmodel.Alarm
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

enum class ActiveTab { Alarms, Timer, Stopwatch }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTimerScreen(sharedViewModel: SharedConnectionViewModel) {
    var activeTab by remember { mutableStateOf(ActiveTab.Alarms) }
    val alarms by sharedViewModel.alarms.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val timerSeconds by sharedViewModel.timerSeconds.collectAsState()
    val isTimerRunning by sharedViewModel.isTimerRunning.collectAsState()

    val stopwatchMillis by sharedViewModel.stopwatchMillis.collectAsState()
    val isStopwatchRunning by sharedViewModel.isStopwatchRunning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Custom Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF1F5F9))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TabButton(
                title = "Alarms",
                icon = Icons.Default.Alarm,
                selected = activeTab == ActiveTab.Alarms,
                onClick = { activeTab = ActiveTab.Alarms },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "Timer",
                icon = Icons.Default.Timer,
                selected = activeTab == ActiveTab.Timer,
                onClick = { activeTab = ActiveTab.Timer },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                title = "Stopwatch",
                icon = Icons.Default.Schedule,
                selected = activeTab == ActiveTab.Stopwatch,
                onClick = { activeTab = ActiveTab.Stopwatch },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (activeTab) {
            ActiveTab.Alarms -> AlarmsContent(
                alarms = alarms,
                onToggle = { sharedViewModel.toggleAlarm(it) },
                onAddClick = { showAddDialog = true },
                onDelete = { sharedViewModel.deleteAlarm(it) }
            )
            ActiveTab.Timer -> TimerContent(
                secondsLeft = timerSeconds,
                isRunning = isTimerRunning,
                onStart = { sharedViewModel.startTimer(it) },
                onStop = { sharedViewModel.stopTimer() }
            )
            ActiveTab.Stopwatch -> StopwatchContent(
                millis = stopwatchMillis,
                isRunning = isStopwatchRunning,
                onStart = { sharedViewModel.startStopwatch() },
                onPause = { sharedViewModel.pauseStopwatch() },
                onReset = { sharedViewModel.resetStopwatch() }
            )
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { time, label ->
                sharedViewModel.addAlarm(time, label)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AlarmsContent(
    alarms: List<Alarm>,
    onToggle: (Alarm) -> Unit,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    Button(
        onClick = onAddClick,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).height(52.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text("Add New Alarm", fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }

    if (alarms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Alarms Set", color = TextSecondary)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(alarms) { alarm ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = alarm.time,
                            fontSize = 32.sp,
                            letterSpacing = 1.sp,
                            color = if (alarm.active) MaterialTheme.colorScheme.onSurface else TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onDelete(alarm.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                        }
                        Switch(
                            checked = alarm.active,
                            onCheckedChange = { onToggle(alarm) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var time by remember { mutableStateOf("08:00") }
    var label by remember { mutableStateOf("Wake up") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Alarm") },
        text = {
            Column {
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time (HH:mm)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(time, label) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimerContent(secondsLeft: Int, isRunning: Boolean, onStart: (Int) -> Unit, onStop: () -> Unit) {
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeStr = "%02d:%02d".format(minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeStr,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Stop", fontSize = 16.sp)
                }
                Button(
                    onClick = { if (!isRunning) onStart(15 * 60) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isRunning
                ) {
                    Text(if (isRunning) "Running" else "Start 15m", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun StopwatchContent(millis: Long, isRunning: Boolean, onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit) {
    val centis = (millis % 1000) / 10
    val seconds = (millis / 1000) % 60
    val minutes = (millis / 60000) % 60
    val timeStr = "%02d:%02d.%02d".format(minutes, seconds, centis)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeStr,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(52.dp).background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
                
                Button(
                    onClick = { if (isRunning) onPause() else onStart() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isRunning) Color(0xFFEF4444) else Color.White
                    )
                ) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Pause" else "Start", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp).padding(end = 4.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSurface else TextSecondary
            )
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else TextSecondary
            )
        }
    }
}

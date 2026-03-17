package com.dotmatrix.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotmatrix.app.viewmodel.Alarm
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

enum class ActiveTab { Alarms, Timer, Stopwatch }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmTimerScreen(sharedViewModel: SharedConnectionViewModel) {
    var activeTab        by remember { mutableStateOf(ActiveTab.Alarms) }
    val alarms           by sharedViewModel.alarms.collectAsState()
    var showAddDialog    by remember { mutableStateOf(false) }
    val timerSeconds     by sharedViewModel.timerSeconds.collectAsState()
    val isTimerRunning   by sharedViewModel.isTimerRunning.collectAsState()
    val stopwatchMillis  by sharedViewModel.stopwatchMillis.collectAsState()
    val isSwRunning      by sharedViewModel.isStopwatchRunning.collectAsState()

    Column(Modifier.fillMaxSize()) {

        // ── Tab row ─────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                Triple(ActiveTab.Alarms,    "Alarms",    Icons.Outlined.Alarm),
                Triple(ActiveTab.Timer,     "Timer",     Icons.Outlined.Timer),
                Triple(ActiveTab.Stopwatch, "Stopwatch", Icons.Outlined.Schedule)
            ).forEach { (tab, label, icon) ->
                TabPill(label, icon, selected = activeTab == tab, modifier = Modifier.weight(1f)) {
                    activeTab = tab
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        when (activeTab) {
            ActiveTab.Alarms    -> AlarmsContent(alarms,
                onToggle   = { sharedViewModel.toggleAlarm(it) },
                onAddClick = { showAddDialog = true },
                onDelete   = { sharedViewModel.deleteAlarm(it) })
            ActiveTab.Timer     -> TimerContent(timerSeconds, isTimerRunning,
                onStart = { sharedViewModel.startTimer(it) },
                onStop  = { sharedViewModel.stopTimer() })
            ActiveTab.Stopwatch -> StopwatchContent(stopwatchMillis, isSwRunning,
                onStart = { sharedViewModel.startStopwatch() },
                onPause = { sharedViewModel.pauseStopwatch() },
                onReset = { sharedViewModel.resetStopwatch() })
        }
    }

    if (showAddDialog) {
        AddAlarmDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { time, label -> sharedViewModel.addAlarm(time, label); showAddDialog = false }
        )
    }
}

@Composable
fun AlarmsContent(alarms: List<Alarm>, onToggle: (Alarm) -> Unit, onAddClick: () -> Unit, onDelete: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedButton(
            onClick    = onAddClick,
            modifier   = Modifier.fillMaxWidth().height(48.dp),
            shape      = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add New Alarm", fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(12.dp))
        if (alarms.isEmpty()) {
            EmptyStateCard(
                icon     = Icons.Outlined.Alarm,
                title    = "No Alarms Set",
                subtitle = "Tap \"Add New Alarm\" to schedule your first alarm"
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alarms) { alarm ->
                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(alarm.time, fontSize = 28.sp, letterSpacing = 1.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    color      = if (alarm.active) MaterialTheme.colorScheme.onSurface
                                                 else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Light)
                                Text(alarm.label, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onDelete(alarm.id) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                Switch(checked = alarm.active, onCheckedChange = { onToggle(alarm) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var time  by remember { mutableStateOf("08:00") }
    var label by remember { mutableStateOf("Wake up") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Add Alarm") },
        text = {
            Column {
                OutlinedTextField(value = time, onValueChange = { time = it },
                    label = { Text("Time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = label, onValueChange = { label = it },
                    label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onConfirm(time, label) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TimerContent(secondsLeft: Int, isRunning: Boolean, onStart: (Int) -> Unit, onStop: () -> Unit) {
    val m = secondsLeft / 60; val s = secondsLeft % 60
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%02d:%02d".format(m, s), fontSize = 60.sp, fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp)) {
                    Text("Stop")
                }
                Button(onClick = { if (!isRunning) onStart(15 * 60) }, modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp), enabled = !isRunning) {
                    Text(if (isRunning) "Running" else "Start 15m")
                }
            }
        }
    }
}

@Composable
fun StopwatchContent(millis: Long, isRunning: Boolean, onStart: () -> Unit, onPause: () -> Unit, onReset: () -> Unit) {
    val c = (millis % 1000) / 10; val s = (millis / 1000) % 60; val m = (millis / 60000) % 60
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%02d:%02d.%02d".format(m, s, c), fontSize = 52.sp, fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(onClick = onReset, modifier = Modifier.size(48.dp)
                    .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Reset")
                }
                Button(onClick = { if (isRunning) onPause() else onStart() },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(8.dp)) {
                    Icon(if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRunning) "Pause" else "Start")
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(icon: ImageVector, title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
fun TabPill(title: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { onClick() }.padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 2.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(title, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

package com.dotmatrix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dotmatrix.app.ble.FirmwareWeatherState
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    sharedViewModel: SharedConnectionViewModel
) {
    val isConnected by sharedViewModel.isConnected.collectAsState()
    val deviceName  by sharedViewModel.deviceName.collectAsState()
    val is24H       by sharedViewModel.is24HourFormat.collectAsState()
    val temperature by sharedViewModel.temperature.collectAsState()
    val humidity    by sharedViewModel.humidity.collectAsState()
    val currentMode by sharedViewModel.currentMode.collectAsState()
    val weatherState by sharedViewModel.weatherState.collectAsState()
    val messageAnimationStyle by sharedViewModel.messageAnimationStyle.collectAsState()
    val haptic      = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    var messageText by remember { mutableStateOf("") }

    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { currentTime = LocalDateTime.now(); delay(1000) } }
    LaunchedEffect(sharedViewModel) {
        sharedViewModel.deviceEvents.collect { event ->
            snackbarHostState.showSnackbar(event)
        }
    }

    val timeFmt = if (is24H) DateTimeFormatter.ofPattern("HH:mm")
                  else       DateTimeFormatter.ofPattern("h:mm")
    val amPmFmt = DateTimeFormatter.ofPattern("a")
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM dd")

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val headerAlpha by animateFloatAsState(
        targetValue = if (scrollBehavior.state.collapsedFraction > 0.05f) 0.85f else 0f,
        animationSpec = tween(150, easing = LinearEasing),
        label = "headerAlpha"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = headerAlpha))) {
                LargeTopAppBar(
                    title = { 
                        Text(
                            "Dashboard", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.displaySmall
                        ) 
                    },
                    actions = {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            navController.navigate("settings")
                        }) {
                            Icon(Icons.Outlined.Settings, "Settings")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Clock Card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let {
                            val infiniteTransition = rememberInfiniteTransition(label = "bgSweep")
                            val color1 by infiniteTransition.animateColor(
                                initialValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                targetValue = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                                label = "c1"
                            )
                            val color2 by infiniteTransition.animateColor(
                                initialValue = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                                targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                animationSpec = infiniteRepeatable(tween(4500, easing = FastOutLinearInEasing), RepeatMode.Reverse),
                                label = "c2"
                            )
                            it.background(Brush.linearGradient(listOf(color1, color2)))
                        }
                        .padding(vertical = 32.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = currentTime.format(timeFmt),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-2).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!is24H) {
                                Text(
                                    text = " " + currentTime.format(amPmFmt),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 14.dp)
                                )
                            }
                        }
                        Text(
                            text = currentTime.format(dateFmt),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Room Environment Card ───────────────────────────────────────
            EnvironmentCard(
                isConnected = isConnected,
                temperature = temperature,
                humidity = humidity,
                weatherState = weatherState,
                onSeeForecastClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.navigate("weather")
                }
            )

            Spacer(Modifier.height(16.dp))

            // ── Connection Status Card ──────────────────────────────────────
            val statusColor = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
            
            var isConnPressed by remember { mutableStateOf(false) }
            val connScale by animateFloatAsState(
                targetValue = if (isConnPressed) 0.96f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "connScale"
            )
            LaunchedEffect(isConnPressed) {
                if (isConnPressed) { delay(100); isConnPressed = false }
            }
            
            Card(
                onClick = {
                    isConnPressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    navController.navigate("devices")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = connScale; scaleY = connScale },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                     else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Outlined.BluetoothConnected else Icons.Outlined.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) deviceName else "Disconnected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isConnected) "Connected and ready" else "Tap to scan for devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isConnected) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.3f))
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            VirtualControlCard(
                isConnected = isConnected,
                currentMode = currentMode,
                messageText = messageText,
                messageAnimationStyle = messageAnimationStyle,
                onMessageChange = { messageText = it },
                onSendMessage = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sharedViewModel.sendMessageToDisplay(messageText)
                    messageText = ""
                },
                onTimeSync = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sharedViewModel.sendCurrentTimeSync()
                },
                onRequestMode = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    sharedViewModel.requestCurrentMode()
                },
                onMessageAnimationSelect = { style ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    when (style) {
                        "WAVE" -> sharedViewModel.sendMessageAnimationWave()
                        "SCROLL" -> sharedViewModel.sendMessageAnimationScroll()
                        "RAIN" -> sharedViewModel.sendMessageAnimationRain()
                        else -> sharedViewModel.sendMessageAnimationNone()
                    }
                },
                onButtonPress = { button ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    sharedViewModel.sendVirtualButtonCommand(button)
                }
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Quick Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Settings",
                    icon = Icons.Outlined.SettingsSuggest,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("settings") 
                    }
                )
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Clock Faces",
                    icon = Icons.Outlined.Watch,
                    enabled = isConnected,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("faces") 
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Visualizer",
                    icon = Icons.Outlined.AutoGraph,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("visualizer") 
                    }
                )
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "DextBot",
                    icon = Icons.Outlined.Pets,
                    enabled = isConnected,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("dextbot") 
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Games",
                    icon = Icons.Outlined.Games,
                    enabled = isConnected,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("games") 
                    }
                )
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Sync Time",
                    icon = Icons.Outlined.Restore,
                    enabled = isConnected,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        sharedViewModel.sendCurrentTimeSync() 
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                QuickControlCard(
                    modifier = Modifier.weight(1f),
                    label = "Firmware",
                    icon = Icons.Outlined.Terminal,
                    onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        navController.navigate("ota") 
                    }
                )
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VirtualControlCard(
    isConnected: Boolean,
    currentMode: String,
    messageText: String,
    messageAnimationStyle: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onTimeSync: () -> Unit,
    onRequestMode: () -> Unit,
    onMessageAnimationSelect: (String) -> Unit,
    onButtonPress: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Active Mode Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Virtual Controls",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    if (isConnected) {
                        val normalizedMode = currentMode.trim().uppercase().ifEmpty { "CLOCK" }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Mode: $normalizedMode",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            "Connect your clock to enable controls",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = onRequestMode,
                    enabled = isConnected,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, "Refresh Mode", modifier = Modifier.size(20.dp))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 4-Way D-Pad Remote Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DpadButton(
                    icon = Icons.Outlined.CompareArrows,
                    label = "MODE",
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    onClick = { onButtonPress("MODE") }
                )
                DpadButton(
                    icon = Icons.Outlined.SkipNext,
                    label = "NEXT",
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    onClick = { onButtonPress("NEXT") }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DpadButton(
                    icon = Icons.Outlined.Undo,
                    label = "BACK",
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    onClick = { onButtonPress("BACK") }
                )
                DpadButton(
                    icon = Icons.Outlined.CheckCircleOutline,
                    label = "SELECT",
                    modifier = Modifier.weight(1f),
                    enabled = isConnected,
                    onClick = { onButtonPress("SELECT") }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Message Configuration Box
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Message Broadcaster",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isConnected,
                        placeholder = { Text("Display text...") },
                        singleLine = true,
                        shape = CircleShape,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    )

                    // Unified Segmented Selectors for Animation
                    val animationOptions = listOf(
                        Triple("NONE", "None", Icons.Outlined.Title),
                        Triple("WAVE", "Wave", Icons.Outlined.Waves),
                        Triple("SCROLL", "Scroll", Icons.Outlined.SyncAlt),
                        Triple("RAIN", "Rain", Icons.Outlined.Grain)
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shadowElevation = 2.dp
                    ) {
                        Box(modifier = Modifier.padding(4.dp)) {
                            val selectedIndex = animationOptions.indexOfFirst { it.first == messageAnimationStyle }.coerceAtLeast(0)
                            
                            BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                                val tabWidth = maxWidth / animationOptions.size
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
                                animationOptions.forEach { (style, label, icon) ->
                                    val isSelected = messageAnimationStyle == style
                                    val textColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, 
                                        animationSpec = tween(durationMillis = 250),
                                        label = "text"
                                    )
                                    
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(CircleShape)
                                            .clickable(
                                                enabled = isConnected,
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onMessageAnimationSelect(style) }
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isConnected) textColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isConnected) textColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onSendMessage,
                            enabled = isConnected && messageText.trim().isNotEmpty(),
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Send, "Send", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Broadcast", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = onTimeSync,
                            enabled = isConnected,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.Restore, "Sync Time", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DpadButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f),
        tonalElevation = if (enabled) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.4f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.4f)
            )
        }
    }
}

@Composable
private fun EnvironmentCard(
    isConnected: Boolean, 
    temperature: Float?, 
    humidity: Float?,
    weatherState: FirmwareWeatherState,
    onSeeForecastClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.DeviceThermostat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Environment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            if (!isConnected) {
                Text(
                    "Connect to view sensor data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Indoor Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("INDOOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            if (temperature == null || humidity == null) {
                                Text("Loading...", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(
                                    text = "${temperature.toInt()}°C",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Humidity ${humidity.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Outdoor Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("OUTDOOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            if (weatherState.temp.isNaN()) {
                                Text("No data", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text(
                                    text = "${weatherState.temp.toInt()}°C",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${weatherState.condition} in ${weatherState.city}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // ── See Forecast Link ─────────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeeForecastClick() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "7-Day Forecast",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}


@Composable
private fun QuickControlCard(
    modifier: Modifier, 
    label: String, 
    icon: ImageVector, 
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "quickCtrlScale"
    )
    LaunchedEffect(isPressed) {
        if (isPressed) { delay(100); isPressed = false }
    }

    Surface(
        modifier = modifier
            .height(110.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(enabled = enabled) {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        tonalElevation = 1.dp,
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = if (enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = label, 
                style = MaterialTheme.typography.labelLarge, 
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

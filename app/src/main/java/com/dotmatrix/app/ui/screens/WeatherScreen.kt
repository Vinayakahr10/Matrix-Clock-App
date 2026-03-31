package com.dotmatrix.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dotmatrix.app.viewmodel.WeatherViewModel
import com.dotmatrix.app.weather.DayForecast
import com.dotmatrix.app.weather.HourForecast
import com.dotmatrix.app.weather.WeatherForecast
import com.dotmatrix.app.weather.wmoCodeToDescription
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel,
    navController: NavController
) {
    val forecast by viewModel.forecast.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val permissionGranted by viewModel.locationPermissionGranted.collectAsState()
    val haptic = LocalHapticFeedback.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            forecast?.city ?: "Weather",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.refreshWeather()
                    }) {
                        Icon(Icons.Outlined.Refresh, "Refresh")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading && forecast == null -> WeatherLoadingState()
                !permissionGranted -> WeatherPermissionState {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
                error != null && forecast == null -> WeatherErrorState(error!!) {
                    viewModel.refreshWeather()
                }
                forecast != null -> WeatherContent(forecast = forecast!!)
                else -> WeatherLoadingState()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherContent(forecast: WeatherForecast) {
    var showAll by remember { mutableStateOf(false) }
    val (_, currentEmoji) = wmoCodeToDescription(forecast.currentCode)
    val visibleDays = if (showAll) forecast.days else forecast.days.take(3)

    // Overall temperature range for the week (for range bar scaling)
    val weekMin = forecast.days.minOfOrNull { it.minTemp } ?: 0f
    val weekMax = forecast.days.maxOfOrNull { it.maxTemp } ?: 40f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Current Temp Hero ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${forecast.currentTemp.toInt()}°",
                    fontSize = 84.sp,
                    fontWeight = FontWeight.Thin,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 90.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currentEmoji,
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = forecast.currentCondition,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("↓", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                    Text(
                        " ${forecast.todayMin.toInt()}°  ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("↑", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                    Text(
                        " ${forecast.todayMax.toInt()}°",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ── Hourly Forecast Card ─────────────────────────────────────────────
        if (forecast.hours.isNotEmpty()) {
            HourlyForecastCard(hours = forecast.hours)
        }

        // ── Daily Forecast Card ───────────────────────────────────────────────
        DailyForecastCard(
            days = visibleDays,
            weekMin = weekMin,
            weekMax = weekMax,
            showAll = showAll,
            totalDays = forecast.days.size,
            onToggleShowAll = { showAll = !showAll }
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "Updated ${formatTime(forecast.fetchedAt)}  •  Open-Meteo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hourly Card with temperature curve
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HourlyForecastCard(hours: List<HourForecast>) {
    val minT = hours.minOfOrNull { it.temp } ?: 0f
    val maxT = hours.maxOfOrNull { it.temp } ?: 40f
    val range = (maxT - minT).coerceAtLeast(1f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Hourly forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Outlined.AccessTime, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(20.dp))

            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                // Temperature curve (Canvas)
                val curveHeight = 56.dp

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        // padding = 24.dp (row outer padding) + 24.dp (half item width)
                        .padding(horizontal = 48.dp)
                ) {
                    if (hours.size < 2) return@Canvas
                    val w = size.width
                    val h = curveHeight.toPx()
                    val yOffset = size.height - h - 42.dp.toPx()
                    val step = w / (hours.size - 1).toFloat()
                    val points = hours.mapIndexed { i, hour ->
                        val x = i * step
                        val y = yOffset + h - ((hour.temp - minT) / range) * h * 0.7f - h * 0.15f
                        Offset(x, y)
                    }
                    val path = Path()
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cx = (prev.x + curr.x) / 2f
                        path.cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                    }
                    // Draw gradient fill below the curve
                    val fillPath = Path()
                    fillPath.addPath(path)
                    fillPath.lineTo(points.last().x, yOffset + h)
                    fillPath.lineTo(points.first().x, yOffset + h)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            startY = yOffset,
                            endY = yOffset + h
                        )
                    )

                    // Draw the actual curve line
                    drawPath(path, color = primaryColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    // Tick marks
                    points.forEach { pt ->
                        drawCircle(color = surfaceColor, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = pt)
                        drawLine(
                            color = primaryColor.copy(alpha = 0.2f),
                            start = Offset(pt.x, yOffset + h),
                            end = Offset(pt.x, pt.y + 6.dp.toPx()),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                    }
                }

                // Temperatures row
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    hours.forEach { hour ->
                        val (_, emoji) = wmoCodeToDescription(hour.weatherCode)
                        Column(
                            modifier = Modifier.width(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${hour.temp.toInt()}°",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold)
                            Text(emoji, fontSize = 24.sp)
                            // spacer for curve (rendered below)
                            Spacer(Modifier.height(48.dp))
                            Text(hour.time,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Daily Forecast Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DailyForecastCard(
    days: List<DayForecast>,
    weekMin: Float,
    weekMax: Float,
    showAll: Boolean,
    totalDays: Int,
    onToggleShowAll: () -> Unit
) {
    val weekRange = (weekMax - weekMin).coerceAtLeast(1f)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daily forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(Icons.Outlined.CalendarToday, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            days.forEachIndexed { idx, day ->
                DailyRow(day = day, weekMin = weekMin, weekRange = weekRange)
                if (idx < days.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                }
            }

            // Show more / less
            if (totalDays > 3) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleShowAll() }
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showAll) "Show less" else "Show more",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (showAll) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyRow(day: DayForecast, weekMin: Float, weekRange: Float) {
    val (_, emoji) = wmoCodeToDescription(day.weatherCode)
    val barStart = ((day.minTemp - weekMin) / weekRange)
    val barEnd = ((day.maxTemp - weekMin) / weekRange)
    // Today marker position relative to bar
    val todayMarker = if (day.isToday) ((day.minTemp + day.maxTemp) / 2f - weekMin) / weekRange else null
    val barColor = MaterialTheme.colorScheme.primary
    val barBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    val markerColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day label
        Text(
            day.date,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.width(80.dp),
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )

        // Emoji icon
        Text(emoji, fontSize = 24.sp)

        Spacer(Modifier.width(16.dp))

        // Min temp
        Text(
            "${day.minTemp.toInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.width(12.dp))

        // Range bar
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
        ) {
            val w = size.width
            val h = size.height / 2f
            val radius = h
            // Background track
            drawRoundRect(
                color = barBgColor,
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
            // Filled segment
            val startX = barStart * w
            val endX = barEnd * w
            drawRoundRect(
                color = barColor,
                topLeft = Offset(startX, 0f),
                size = androidx.compose.ui.geometry.Size((endX - startX).coerceAtLeast(8f), size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
            )
            // Today marker
            todayMarker?.let {
                val mx = (it * w).coerceIn(startX + 3f, endX - 3f)
                drawCircle(color = surfaceColor, radius = 5.dp.toPx(), center = Offset(mx, h))
                drawCircle(color = markerColor, radius = 4.dp.toPx(), center = Offset(mx, h))
            }
        }

        Spacer(Modifier.width(12.dp))

        // Max temp
        Text(
            "${day.maxTemp.toInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// States
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WeatherLoadingState() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha"
    )
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.width(160.dp).height(90.dp).clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
    }
}

@Composable
private fun WeatherErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 56.sp)
        Spacer(Modifier.height(24.dp))
        Text("Weather Unavailable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeatherPermissionState(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📍", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text("Location Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("DOTMATRIX needs your location to fetch precise local weather metrics.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Allow Location", fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTime(epochMs: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMs))

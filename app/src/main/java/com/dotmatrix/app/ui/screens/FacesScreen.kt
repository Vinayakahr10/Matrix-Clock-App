package com.dotmatrix.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

data class ClockFace(
    val id: String,
    val title: String,
    val description: String,
    val accentColor: Color,
    val previewType: String // "digital", "binary", "analog", "word", "large"
)

val availableFaces = listOf(
    ClockFace(
        id = "CLASSIC",
        title = "Classic Digital",
        description = "Standard 5x7 or 3x5 font time and date",
        accentColor = Color(0xFF4285F4),
        previewType = "digital"
    ),
    ClockFace(
        id = "LARGE",
        title = "Large Numeric",
        description = "Thick, bold numbers filling the 8px height",
        accentColor = Color(0xFFE91E63),
        previewType = "large"
    ),
    ClockFace(
        id = "BINARY",
        title = "Binary Matrix",
        description = "Hacker-style vertically stacked binary blocks",
        accentColor = Color(0xFF10B981),
        previewType = "binary"
    ),
    ClockFace(
        id = "WORD",
        title = "Word Scroll",
        description = "Scrolling text stating the time naturally",
        accentColor = Color(0xFFF59E0B),
        previewType = "word"
    ),
    ClockFace(
        id = "ANALOG",
        title = "Analog Pixel",
        description = "Ultra-tiny 8x8 circular dial + digital time",
        accentColor = Color(0xFF8B5CF6),
        previewType = "analog"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacesScreen(
    sharedViewModel: SharedConnectionViewModel,
    navController: NavController
) {
    val haptic = LocalHapticFeedback.current
    val currentFace by sharedViewModel.currentFace.collectAsState()
    val isConnected by sharedViewModel.isConnected.collectAsState()

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) {
            snackbarHostState.showSnackbar(snackbarMessage, duration = SnackbarDuration.Short)
            showSnackbar = false
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text("Lockscreen Faces", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (!isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Device disconnected. Changes will be saved locally.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = "Optimized for 8x32 MAX7219",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp, start = 4.dp)
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(availableFaces, key = { it.id }) { face ->
                    FaceCardItem(
                        face = face,
                        isSelected = (currentFace == face.id),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            sharedViewModel.setClockFace(face.id)
                            snackbarMessage = "Applied ${face.title}"
                            showSnackbar = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FaceCardItem(
    face: ClockFace,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) { kotlinx.coroutines.delay(100); isPressed = false }
    }

    val containerColor = if (isSelected) face.accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    val outlineColor = if (isSelected) face.accentColor else Color.Transparent

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .border(2.dp, outlineColor, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Preview Banner (simulating an 8x32 matrix aesthetically)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                face.accentColor.copy(alpha = 0.8f),
                                face.accentColor.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Mockup representations of what the dot matrix looks like
                when (face.previewType) {
                    "digital" -> Text("10:42 59", style = MaterialTheme.typography.displaySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                    "large"   -> Text("1 0 4 2", style = MaterialTheme.typography.displayMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = Color.White)
                    "binary"  -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(4) { i -> Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(if ((it+i)%3==0) Color.White else Color.White.copy(alpha=0.2f))) }
                            }
                        }
                    }
                    "word"    -> Text("TEN PAST FOUR", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
                    "analog"  -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).border(2.dp, Color.White, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.width(2.dp).height(12.dp).background(Color.White).align(Alignment.TopCenter).offset(y = 4.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("10:42", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace, color = Color.White)
                    }
                }
            }

            // Description Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = face.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = face.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = face.accentColor,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(32.dp)
                    )
                }
            }
        }
    }
}

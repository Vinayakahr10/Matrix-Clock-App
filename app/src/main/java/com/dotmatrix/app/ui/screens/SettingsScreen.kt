package com.dotmatrix.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dotmatrix.app.viewmodel.FontFamilyOption
import com.dotmatrix.app.viewmodel.FontSizeOption
import com.dotmatrix.app.viewmodel.SettingsViewModel
import com.dotmatrix.app.viewmodel.ThemeMode

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, navController: NavController) {
    val haptic      = LocalHapticFeedback.current
    val themeMode   by viewModel.themeMode.collectAsState()
    val fontSize    by viewModel.fontSize.collectAsState()
    val fontFamily  by viewModel.fontFamily.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector        = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Appearance ──────────────────────────────────────────────────
            SettingsSectionLabel("Appearance")
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // 2×2 theme option grid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeOptionCard(
                            modifier   = Modifier.weight(1f),
                            label      = "Light",
                            icon       = Icons.Outlined.LightMode,
                            selected   = themeMode == ThemeMode.LIGHT,
                            onClick    = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setThemeMode(ThemeMode.LIGHT)
                            }
                        )
                        ThemeOptionCard(
                            modifier   = Modifier.weight(1f),
                            label      = "Dark",
                            icon       = Icons.Outlined.DarkMode,
                            selected   = themeMode == ThemeMode.DARK,
                            onClick    = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setThemeMode(ThemeMode.DARK)
                            }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeOptionCard(
                            modifier   = Modifier.weight(1f),
                            label      = "System",
                            icon       = Icons.Outlined.SettingsBrightness,
                            selected   = themeMode == ThemeMode.SYSTEM,
                            onClick    = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setThemeMode(ThemeMode.SYSTEM)
                            }
                        )
                        ThemeOptionCard(
                            modifier   = Modifier.weight(1f),
                            label      = "Pitch Dark",
                            icon       = Icons.Outlined.NightsStay,
                            selected   = themeMode == ThemeMode.PITCH_DARK,
                            onClick    = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setThemeMode(ThemeMode.PITCH_DARK)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Typography ──────────────────────────────────────────────────
            SettingsSectionLabel("Typography")

            // Font Size
            SettingsCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.FormatSize,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Font Size",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text  = fontSizeLabel(fontSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier                = Modifier.fillMaxWidth(),
                        horizontalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        FontSizeOption.values().forEach { option ->
                            val selected = fontSize == option
                            val animatedColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary
                                              else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(200), label = "fontSizeColor"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
                                              else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(200), label = "fontSizeTextColor"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(animatedColor)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.setFontSize(option)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text      = fontSizeLabel(option),
                                    color     = textColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize  = when (option) {
                                        FontSizeOption.SMALL  -> 13.sp
                                        FontSizeOption.MEDIUM -> 15.sp
                                        FontSizeOption.LARGE  -> 18.sp
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Font Family
            SettingsCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FontDownload,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Font Style",
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FontFamilyOption.values().forEach { option ->
                            FontFamilyRow(
                                label      = fontFamilyLabel(option),
                                fontFamily = fontFamilyFor(option),
                                selected   = fontFamily == option,
                                onClick    = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setFontFamily(option)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── About ───────────────────────────────────────────────────────
            SettingsSectionLabel("About")
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    AboutRow(Icons.Outlined.Info,       "Version",    "1.0.0")
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = 6.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    AboutRow(Icons.Outlined.Build,      "Build",      "DOTMATRIX-APP")
                    HorizontalDivider(
                        modifier  = Modifier.padding(vertical = 6.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    AboutRow(Icons.Outlined.Bluetooth,  "Protocol",   "BLE / GATT")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelLarge,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content  = content
        )
    }
}

@Composable
private fun ThemeOptionCard(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200), label = "themeBorder"
    )
    val bgColor by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200), label = "themeBg"
    )

    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = if (selected) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(24.dp)
            )
            Text(
                text       = label,
                style      = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (selected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FontFamilyRow(
    label: String,
    fontFamily: FontFamily,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue   = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent,
        animationSpec = tween(200), label = "fontFamilyBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text       = "Aa — $label",
            fontFamily = fontFamily,
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (selected) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "selected",
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun fontSizeLabel(option: FontSizeOption) = when (option) {
    FontSizeOption.SMALL  -> "Small"
    FontSizeOption.MEDIUM -> "Medium"
    FontSizeOption.LARGE  -> "Large"
}

private fun fontFamilyLabel(option: FontFamilyOption) = when (option) {
    FontFamilyOption.DEFAULT   -> "Default (Sans-Serif)"
    FontFamilyOption.SERIF     -> "Serif"
    FontFamilyOption.MONOSPACE -> "Monospace"
    FontFamilyOption.ROUNDED   -> "Rounded"
}

private fun fontFamilyFor(option: FontFamilyOption): FontFamily = when (option) {
    FontFamilyOption.DEFAULT   -> FontFamily.SansSerif
    FontFamilyOption.SERIF     -> FontFamily.Serif
    FontFamilyOption.MONOSPACE -> FontFamily.Monospace
    FontFamilyOption.ROUNDED   -> FontFamily.Cursive   // closest built-in approximation
}

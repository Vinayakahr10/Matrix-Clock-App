package com.dotmatrix.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dotmatrix.app.update.AppUpdateInfo
import com.dotmatrix.app.viewmodel.AppUpdateStatus
import com.dotmatrix.app.viewmodel.FontFamilyOption
import com.dotmatrix.app.viewmodel.FontSizeOption
import com.dotmatrix.app.viewmodel.SettingsViewModel
import com.dotmatrix.app.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, navController: NavController) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val installedVersion by viewModel.installedVersionName.collectAsState()
    val appUpdateStatus by viewModel.appUpdateStatus.collectAsState()
    val availableAppUpdate by viewModel.availableAppUpdate.collectAsState()
    val appUpdateError by viewModel.appUpdateError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary
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

            SettingsSectionLabel("Appearance")
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Theme",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeOptionCard(
                            modifier = Modifier.weight(1f),
                            label = "Light",
                            icon = Icons.Outlined.LightMode,
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setThemeMode(ThemeMode.LIGHT)
                            }
                        )
                        ThemeOptionCard(
                            modifier = Modifier.weight(1f),
                            label = "Dark",
                            icon = Icons.Outlined.DarkMode,
                            selected = themeMode == ThemeMode.DARK,
                            onClick = {
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
                            modifier = Modifier.weight(1f),
                            label = "System",
                            icon = Icons.Outlined.SettingsBrightness,
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setThemeMode(ThemeMode.SYSTEM)
                            }
                        )
                        ThemeOptionCard(
                            modifier = Modifier.weight(1f),
                            label = "Pitch Dark",
                            icon = Icons.Outlined.NightsStay,
                            selected = themeMode == ThemeMode.PITCH_DARK,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setThemeMode(ThemeMode.PITCH_DARK)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            SettingsSectionLabel("Typography")
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
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Font Size",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = fontSizeLabel(fontSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontSizeOption.values().forEach { option ->
                            val selected = fontSize == option
                            val animatedColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(200),
                                label = "fontSizeColor"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(200),
                                label = "fontSizeTextColor"
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
                                    text = fontSizeLabel(option),
                                    color = textColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = when (option) {
                                        FontSizeOption.SMALL -> 13.sp
                                        FontSizeOption.MEDIUM -> 15.sp
                                        FontSizeOption.LARGE -> 18.sp
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SettingsCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FontDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Font Style",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FontFamilyOption.values().forEach { option ->
                            FontFamilyRow(
                                label = fontFamilyLabel(option),
                                fontFamily = fontFamilyFor(option),
                                selected = fontFamily == option,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setFontFamily(option)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            SettingsSectionLabel("Updates")
            SettingsCard {
                AppUpdateRow(
                    installedVersion = installedVersion,
                    updateStatus = appUpdateStatus,
                    updateInfo = availableAppUpdate,
                    errorMessage = appUpdateError,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val updateUrl = availableAppUpdate?.apkUrl
                        if (appUpdateStatus == AppUpdateStatus.UPDATE_AVAILABLE && !updateUrl.isNullOrBlank()) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                        } else {
                            viewModel.refreshAppUpdateStatus()
                        }
                    }
                )
            }

            Spacer(Modifier.height(4.dp))

            SettingsSectionLabel("About")
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    AboutRow(Icons.Outlined.Info, "Version", installedVersion)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    AboutRow(Icons.Outlined.Build, "Build", "DOTMATRIX-APP")
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    AboutRow(Icons.Outlined.Bluetooth, "Protocol", "BLE / GATT")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
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
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "themeBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "themeBg"
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
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(200),
        label = "fontFamilyBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Aa - $label",
            fontFamily = fontFamily,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AboutRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AppUpdateRow(
    installedVersion: String,
    updateStatus: AppUpdateStatus,
    updateInfo: AppUpdateInfo?,
    errorMessage: String?,
    onClick: () -> Unit
) {
    val title = when (updateStatus) {
        AppUpdateStatus.CHECKING -> "Checking for app updates"
        AppUpdateStatus.UP_TO_DATE -> "App is up to date"
        AppUpdateStatus.UPDATE_AVAILABLE -> "New app version available"
        AppUpdateStatus.ERROR -> "Unable to check app updates"
    }

    val subtitle = when (updateStatus) {
        AppUpdateStatus.CHECKING -> "Current version $installedVersion"
        AppUpdateStatus.UP_TO_DATE -> "Current $installedVersion  |  Latest ${updateInfo?.versionName ?: installedVersion}"
        AppUpdateStatus.UPDATE_AVAILABLE -> "Current $installedVersion  |  Latest ${updateInfo?.versionName ?: "--"}"
        AppUpdateStatus.ERROR -> errorMessage ?: "Tap to retry"
    }

    val trailingText = when (updateStatus) {
        AppUpdateStatus.CHECKING -> "Checking"
        AppUpdateStatus.UP_TO_DATE -> "Latest"
        AppUpdateStatus.UPDATE_AVAILABLE -> "Open"
        AppUpdateStatus.ERROR -> "Retry"
    }

    val accentColor = when (updateStatus) {
        AppUpdateStatus.UPDATE_AVAILABLE -> MaterialTheme.colorScheme.primary
        AppUpdateStatus.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (updateStatus) {
                    AppUpdateStatus.UPDATE_AVAILABLE -> Icons.Outlined.SystemUpdateAlt
                    AppUpdateStatus.ERROR -> Icons.Outlined.Refresh
                    else -> Icons.Outlined.AppShortcut
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (updateStatus == AppUpdateStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                updateInfo?.notes?.takeIf { it.isNotEmpty() && updateStatus == AppUpdateStatus.UPDATE_AVAILABLE }?.let { notes ->
                    Spacer(Modifier.height(8.dp))
                    notes.take(2).forEach { note ->
                        Row {
                            Text(
                                text = "\u2022",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun fontSizeLabel(option: FontSizeOption) = when (option) {
    FontSizeOption.SMALL -> "Small"
    FontSizeOption.MEDIUM -> "Medium"
    FontSizeOption.LARGE -> "Large"
}

private fun fontFamilyLabel(option: FontFamilyOption) = when (option) {
    FontFamilyOption.DEFAULT -> "Default (Sans-Serif)"
    FontFamilyOption.SERIF -> "Serif"
    FontFamilyOption.MONOSPACE -> "Monospace"
    FontFamilyOption.ROUNDED -> "Rounded"
}

private fun fontFamilyFor(option: FontFamilyOption): FontFamily = when (option) {
    FontFamilyOption.DEFAULT -> FontFamily.SansSerif
    FontFamilyOption.SERIF -> FontFamily.Serif
    FontFamilyOption.MONOSPACE -> FontFamily.Monospace
    FontFamilyOption.ROUNDED -> FontFamily.Cursive
}

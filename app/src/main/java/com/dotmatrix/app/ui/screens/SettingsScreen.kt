package com.dotmatrix.app.ui.screens

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dotmatrix.app.update.AppUpdateInfo
import com.dotmatrix.app.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    navController: NavController,
    sharedViewModel: SharedConnectionViewModel? = null,
    otaViewModel: OTAViewModel = viewModel(
        factory = OTAViewModel.Factory(
            LocalContext.current.applicationContext as Application,
            sharedViewModel?.bleManager ?: error("SharedConnectionViewModel is required")
        )
    )
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val fontFamily by viewModel.fontFamily.collectAsState()
    val isDeveloperMode by viewModel.isDeveloperModeEnabled.collectAsState()
    val installedVersion by viewModel.installedVersionName.collectAsState()
    val appUpdateStatus by viewModel.appUpdateStatus.collectAsState()
    val availableAppUpdate by viewModel.availableAppUpdate.collectAsState()
    val appUpdateError by viewModel.appUpdateError.collectAsState()
    val versionTapCount by viewModel.versionTapCount.collectAsState()
    val isConnected by sharedViewModel?.isConnected?.collectAsState() ?: remember { mutableStateOf(false) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    val importFirmwareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            showConfirmDialog = true
        }
    }

    if (showConfirmDialog && selectedFileUri != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Install Firmware?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("You are about to flash a local firmware file to your device.")
                    Text(
                        "Estimated time: ~2 minutes. Please keep the phone near the device and do not turn it off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        otaViewModel.uploadLocalFirmware(selectedFileUri!!)
                        showConfirmDialog = false
                        navController.navigate("ota")
                    }
                ) {
                    Text("Flash", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Appearance Section
            SettingsSection(title = "Appearance") {
                ThemeSelectorCard(
                    themeMode = themeMode,
                    onThemeSelected = { newMode ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setThemeMode(newMode)
                    }
                )
            }

            // Typography Section
            SettingsSection(title = "Typography") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Font Size",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (fontSize) {
                                FontSizeOption.SMALL -> "Small"
                                FontSizeOption.MEDIUM -> "Medium"
                                FontSizeOption.LARGE -> "Large"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SegmentedFontSizeSelector(
                        selectedOption = fontSize,
                        onOptionSelected = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setFontSize(it)
                        }
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Font Style",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    FontFamilySelector(
                        selectedOption = fontFamily,
                        onOptionSelected = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setFontFamily(it)
                        }
                    )
                }
            }

            // Updates Section
            SettingsSection(title = "Updates") {
                AppUpdateItem(
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

            // About Section
            SettingsSection(title = "About") {
                SettingsListItem(
                    icon = Icons.Rounded.Info,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTintColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = "Version",
                    subtitle = installedVersion,
                    onClick = {
                        if (!isDeveloperMode) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.incrementVersionTap()
                            if (versionTapCount >= 7) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(context, "You are now a developer!", Toast.LENGTH_SHORT).show()
                            } else if (versionTapCount > 2) {
                                Toast.makeText(context, "You are ${7 - versionTapCount} steps away from being a developer", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsListItem(
                    icon = Icons.Rounded.Build,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTintColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = "Build",
                    subtitle = "DOTMATRIX-APP"
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SettingsListItem(
                    icon = Icons.Rounded.Bluetooth,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTintColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = "Protocol",
                    subtitle = "BLE / GATT"
                )
            }

            // Developer Options
            AnimatedVisibility(
                visible = isDeveloperMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SettingsSection(title = "Developer Options", modifier = Modifier.padding(bottom = 24.dp)) {
                    SettingsListItem(
                        icon = Icons.Outlined.UploadFile,
                        iconBgColor = if (isConnected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        iconTintColor = if (isConnected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f),
                        title = "Install Local Firmware",
                        subtitle = if (isConnected) "Select a .bin file from your device" else "Connect to a device to enable flashing",
                        titleColor = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f),
                        onClick = if (isConnected) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                importFirmwareLauncher.launch(arrayOf("application/octet-stream", "application/x-binary", "application/macbinary", "*/*"))
                            }
                        } else null
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsListItem(
                        icon = Icons.Outlined.DeveloperMode,
                        iconBgColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        iconTintColor = MaterialTheme.colorScheme.error,
                        title = "Disable Developer Mode",
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setDeveloperModeEnabled(false)
                            Toast.makeText(context, "Developer mode disabled", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            content = {
                Column {
                    content()
                }
            }
        )
    }
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTintColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ThemeSelectorCard(
    themeMode: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    val themes = listOf(
        Triple(ThemeMode.LIGHT, "Light", Icons.Outlined.LightMode),
        Triple(ThemeMode.DARK, "Dark", Icons.Outlined.DarkMode),
        Triple(ThemeMode.SYSTEM, "System", Icons.Outlined.SettingsBrightness),
        Triple(ThemeMode.PITCH_DARK, "Pitch Dark", Icons.Outlined.NightsStay)
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.take(2).forEach { (mode, label, icon) ->
                ThemeOptionItem(
                    modifier = Modifier.weight(1f),
                    label = label,
                    icon = icon,
                    selected = themeMode == mode,
                    onClick = { onThemeSelected(mode) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.drop(2).forEach { (mode, label, icon) ->
                ThemeOptionItem(
                    modifier = Modifier.weight(1f),
                    label = label,
                    icon = icon,
                    selected = themeMode == mode,
                    onClick = { onThemeSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "ThemeBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "ThemeContent"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}

@Composable
private fun SegmentedFontSizeSelector(
    selectedOption: FontSizeOption,
    onOptionSelected: (FontSizeOption) -> Unit
) {
    val options = FontSizeOption.values()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedOption == option
            val bgColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                label = "FontSizeBg"
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "FontSizeText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (option) {
                        FontSizeOption.SMALL -> "Small"
                        FontSizeOption.MEDIUM -> "Medium"
                        FontSizeOption.LARGE -> "Large"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FontFamilySelector(
    selectedOption: FontFamilyOption,
    onOptionSelected: (FontFamilyOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FontFamilyOption.values().forEach { option ->
            val isSelected = selectedOption == option
            val bgColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                label = "FontBg"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onOptionSelected(option) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Aa - Default (Sans-Serif)",
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUpdateItem(
    installedVersion: String,
    updateStatus: AppUpdateStatus,
    updateInfo: AppUpdateInfo?,
    errorMessage: String?,
    onClick: () -> Unit
) {
    val title = when (updateStatus) {
        AppUpdateStatus.CHECKING -> "Checking for updates"
        AppUpdateStatus.UP_TO_DATE -> "App is up to date"
        AppUpdateStatus.UPDATE_AVAILABLE -> "New version available"
        AppUpdateStatus.ERROR -> "Unable to check updates"
    }

    val subtitle = when (updateStatus) {
        AppUpdateStatus.CHECKING -> "Current $installedVersion"
        AppUpdateStatus.UP_TO_DATE -> "Current $installedVersion  |  Latest ${updateInfo?.versionName ?: installedVersion}"
        AppUpdateStatus.UPDATE_AVAILABLE -> "Current $installedVersion  |  Latest ${updateInfo?.versionName ?: "--"}"
        AppUpdateStatus.ERROR -> errorMessage ?: "Tap to retry"
    }

    val icon = when (updateStatus) {
        AppUpdateStatus.UPDATE_AVAILABLE -> Icons.Outlined.SystemUpdateAlt
        AppUpdateStatus.ERROR -> Icons.Outlined.Refresh
        else -> Icons.Outlined.AppShortcut
    }
    
    val iconColor = when (updateStatus) {
        AppUpdateStatus.UPDATE_AVAILABLE -> MaterialTheme.colorScheme.primary
        AppUpdateStatus.ERROR -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val iconBgColor = when (updateStatus) {
        AppUpdateStatus.UPDATE_AVAILABLE -> MaterialTheme.colorScheme.primaryContainer
        AppUpdateStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (updateStatus == AppUpdateStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (updateStatus == AppUpdateStatus.UPDATE_AVAILABLE && updateInfo?.notes?.isNotEmpty() == true) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "What's new in ${updateInfo.versionName}:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    updateInfo.notes.take(3).forEach { note ->
                        Row(modifier = Modifier.padding(bottom = 4.dp)) {
                            Text("\u2022 ", color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

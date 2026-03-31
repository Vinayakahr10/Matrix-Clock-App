package com.dotmatrix.app.ui.navigation

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dotmatrix.app.ui.screens.*
import com.dotmatrix.app.viewmodel.OTAViewModel
import com.dotmatrix.app.viewmodel.SettingsViewModel
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel
import com.dotmatrix.app.viewmodel.WeatherViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard",  "Home",      Icons.Default.Home)
    data object Devices   : Screen("devices",    "Devices",   Icons.Default.Bluetooth)
    data object Clock     : Screen("clock",      "Clock",     Icons.Default.Schedule)
    data object Visualizer: Screen("visualizer", "Visualizer",Icons.Default.GraphicEq)
    data object Weather   : Screen("weather",    "Weather",   Icons.Default.WbSunny)
    data object DextBot   : Screen("dextbot",    "DextBot",   Icons.Default.Pets)
    data object Games     : Screen("games",      "Games",     Icons.Default.Games)
    data object OTA       : Screen("ota",        "Update",    Icons.Default.SystemUpdateAlt)
    data object Settings  : Screen("settings",   "Settings",  Icons.Default.Settings)
    data object Faces     : Screen("faces",      "Clock Faces",Icons.Default.Watch)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Devices,
    Screen.Clock,
    Screen.Weather,
    Screen.OTA
)


@Composable
fun AppNavigation(
    sharedViewModel: SharedConnectionViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val context = LocalContext.current.applicationContext as Application
    val otaViewModel: OTAViewModel = viewModel(
        factory = OTAViewModel.Factory(context, sharedViewModel.bleManager)
    )
    val weatherViewModel: WeatherViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon  = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        },
        // Important: Set contentWindowInsets to zero to avoid double padding 
        // with nested Scaffolds in individual screens.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.96f) },
            exitTransition   = { fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Screen.Dashboard.route)  { DashboardScreen(navController, sharedViewModel) }
            composable(Screen.Devices.route)    { DevicesScreen(sharedViewModel) }
            composable(Screen.Clock.route)      { AlarmTimerScreen(sharedViewModel) }
            composable(Screen.Visualizer.route) { VisualizerScreen(sharedViewModel) }
            composable(Screen.DextBot.route)    { DextBotScreen(sharedViewModel) }
            composable(Screen.Games.route)      { GamesScreen(sharedViewModel) }
            composable(Screen.Weather.route)    { WeatherScreen(weatherViewModel, navController) }
            composable(Screen.Faces.route)      { FacesScreen(sharedViewModel, navController) }
            composable(Screen.OTA.route)        { 
                OTAScreen(otaViewModel = otaViewModel) 
            }
            composable(Screen.Settings.route)   { 
                SettingsScreen(
                    viewModel = settingsViewModel, 
                    navController = navController, 
                    sharedViewModel = sharedViewModel,
                    otaViewModel = otaViewModel
                ) 
            }
        }
    }
}

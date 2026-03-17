package com.dotmatrix.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dotmatrix.app.ui.screens.*
import com.dotmatrix.app.ui.theme.CustomCyan
import com.dotmatrix.app.ui.theme.TextSecondary
import com.dotmatrix.app.ui.theme.CardBackground
import com.dotmatrix.app.viewmodel.SharedConnectionViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Devices : Screen("devices", "Devices", Icons.Default.Bluetooth)
    data object Clock : Screen("clock", "Clock", Icons.Default.Schedule)
    data object Visualizer : Screen("visualizer", "Visualizer", Icons.Default.GraphicEq)
    data object OTA : Screen("ota", "Update", Icons.Default.SystemUpdateAlt)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Devices,
    Screen.Clock,
    Screen.Visualizer,
    Screen.OTA
)

@Composable
fun AppNavigation(sharedViewModel: SharedConnectionViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardBackground
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CustomCyan,
                            selectedTextColor = CustomCyan,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = CardBackground
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController, sharedViewModel) }
            composable(Screen.Devices.route) { DevicesScreen(sharedViewModel) }
            composable(Screen.Clock.route) { ClockSettingsScreen(sharedViewModel) }
            composable(Screen.Visualizer.route) { VisualizerScreen(sharedViewModel) }
            composable(Screen.OTA.route) { OTAScreen() }
        }
    }
}

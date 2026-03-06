package com.slowdown.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.slowdown.android.ui.calibrate.CalibrateScreen
import com.slowdown.android.ui.capture.CaptureScreen
import com.slowdown.android.ui.log.LogScreen
import com.slowdown.android.ui.reports.ReportScreen

enum class Screen(val route: String, val title: String, val icon: ImageVector) {
    Capture("capture", "Capture", Icons.Default.CameraAlt),
    Log("log", "Log", Icons.Default.List),
    Reports("reports", "Reports", Icons.Default.BarChart),
    Calibrate("calibrate", "Calibrate", Icons.Default.Settings),
}

@Composable
fun SlowDownApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
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
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Capture.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Capture.route) { CaptureScreen() }
            composable(Screen.Log.route) { LogScreen() }
            composable(Screen.Reports.route) { ReportScreen() }
            composable(Screen.Calibrate.route) { CalibrateScreen() }
        }
    }
}

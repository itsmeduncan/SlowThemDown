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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.slowdown.android.ui.calibrate.CalibrateScreen
import com.slowdown.android.ui.capture.CaptureScreen
import com.slowdown.android.ui.log.LogScreen
import com.slowdown.android.ui.onboarding.OnboardingScreen
import com.slowdown.android.ui.onboarding.OnboardingStore
import com.slowdown.android.ui.reports.ReportScreen
import com.slowdown.android.BuildConfig
import com.slowdown.android.data.db.SpeedEntryDao
import com.slowdown.android.debug.SeedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Screen(val route: String, val title: String, val icon: ImageVector) {
    Capture("capture", "Capture", Icons.Default.CameraAlt),
    Log("log", "Log", Icons.Default.List),
    Reports("reports", "Reports", Icons.Default.BarChart),
    Calibrate("calibrate", "Calibrate", Icons.Default.Settings),
}

private const val ONBOARDING_ROUTE = "onboarding"

@HiltViewModel
class AppViewModel @Inject constructor(
    val onboardingStore: OnboardingStore,
    private val speedEntryDao: SpeedEntryDao,
) : ViewModel() {
    suspend fun seedDemoDataIfDebug() {
        if (BuildConfig.DEBUG) {
            SeedData.seedIfEmpty(speedEntryDao)
        }
    }
}

@Composable
fun SlowDownApp(viewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()

    val onboardingCompleted by viewModel.onboardingStore.isCompleted.collectAsState(initial = null)

    // Seed demo data on first launch (debug builds only)
    LaunchedEffect(Unit) {
        viewModel.seedDemoDataIfDebug()
    }

    // Wait for onboarding state to load
    val completed = onboardingCompleted ?: return

    val startDestination = if (completed) Screen.Capture.route else ONBOARDING_ROUTE

    Scaffold(
        bottomBar = {
            if (currentDestination?.route != ONBOARDING_ROUTE) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ONBOARDING_ROUTE) {
                OnboardingScreen(
                    onComplete = {
                        scope.launch {
                            viewModel.onboardingStore.markCompleted()
                        }
                        navController.navigate(Screen.Capture.route) {
                            popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Capture.route) { CaptureScreen() }
            composable(Screen.Log.route) { LogScreen() }
            composable(Screen.Reports.route) { ReportScreen() }
            composable(Screen.Calibrate.route) { CalibrateScreen() }
        }
    }
}

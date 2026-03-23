package com.slowthemdown.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BadgedBox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.slowthemdown.android.R
import com.slowthemdown.android.ui.calibrate.CalibrateScreen
import com.slowthemdown.android.ui.capture.CaptureScreen
import com.slowthemdown.android.ui.log.LogScreen
import com.slowthemdown.android.ui.onboarding.OnboardingScreen
import com.slowthemdown.android.ui.onboarding.OnboardingStore
import com.slowthemdown.android.ui.calibrate.LicensesScreen
import com.slowthemdown.android.ui.reports.ReportScreen
import android.app.Application
import com.slowthemdown.android.BuildConfig
import com.slowthemdown.android.data.datastore.CalibrationStore
import com.slowthemdown.android.data.db.SpeedEntryDao
import com.slowthemdown.android.debug.SeedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Screen(val route: String, val titleRes: Int, val icon: ImageVector) {
    Capture("capture", R.string.nav_capture, Icons.Default.CameraAlt),
    Log("log", R.string.nav_log, Icons.AutoMirrored.Filled.List),
    Reports("reports", R.string.nav_reports, Icons.Default.BarChart),
    Settings("settings", R.string.nav_settings, Icons.Default.Settings),
}

private const val ONBOARDING_ROUTE = "onboarding"
private const val LICENSES_ROUTE = "licenses"

@HiltViewModel
class AppViewModel @Inject constructor(
    val onboardingStore: OnboardingStore,
    calibrationStore: CalibrationStore,
    private val speedEntryDao: SpeedEntryDao,
    private val application: Application,
) : ViewModel() {
    val isCalibrated: StateFlow<Boolean> = calibrationStore.calibration
        .map { it.isValid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun seedDemoDataIfDebug() {
        if (BuildConfig.DEBUG) {
            SeedData.seedIfEmpty(speedEntryDao, application)
        }
    }
}

@Composable
fun SlowThemDownApp(viewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val scope = rememberCoroutineScope()

    val onboardingCompleted by viewModel.onboardingStore.isCompleted.collectAsState(initial = null)
    val isCalibrated by viewModel.isCalibrated.collectAsState()

    // Seed demo data on first launch (debug builds only)
    LaunchedEffect(Unit) {
        viewModel.seedDemoDataIfDebug()
    }

    // Wait for onboarding state to load
    val completed = onboardingCompleted ?: return

    val startDestination = if (completed) Screen.Capture.route else ONBOARDING_ROUTE

    Scaffold(
        bottomBar = {
            if (currentDestination?.route != ONBOARDING_ROUTE && currentDestination?.route != LICENSES_ROUTE) {
                NavigationBar {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                if (screen == Screen.Settings) {
                                    BadgedBox(
                                        badge = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isCalibrated) Color(0xFF4CAF50)
                                                        else Color(0xFFFF9800)
                                                    )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            if (isCalibrated) Icons.Default.CheckCircle
                                            else Icons.Default.Warning,
                                            contentDescription = stringResource(screen.titleRes),
                                            tint = if (isCalibrated) Color(0xFF4CAF50)
                                                else Color(0xFFFF9800)
                                        )
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = stringResource(screen.titleRes))
                                }
                            },
                            label = { Text(stringResource(screen.titleRes)) },
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
                        navController.navigate(Screen.Settings.route) {
                            popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Capture.route) { CaptureScreen() }
            composable(Screen.Log.route) { LogScreen() }
            composable(Screen.Reports.route) { ReportScreen() }
            composable(Screen.Settings.route) {
                CalibrateScreen(
                    onNavigateToLicenses = { navController.navigate(LICENSES_ROUTE) },
                )
            }
            composable(LICENSES_ROUTE) {
                LicensesScreen(onClose = { navController.popBackStack() })
            }
        }
    }
}

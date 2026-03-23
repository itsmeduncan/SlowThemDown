package com.slowthemdown.android.ui.home

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.R
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.ui.capture.CaptureFlowOverlay
import com.slowthemdown.android.ui.components.DemoBanner
import com.slowthemdown.android.ui.components.SpeedBadge
import com.slowthemdown.android.ui.log.LogDetailSheet
import com.slowthemdown.android.viewmodel.CaptureFlowState
import com.slowthemdown.android.viewmodel.CaptureViewModel
import com.slowthemdown.android.viewmodel.LogViewModel
import com.slowthemdown.android.viewmodel.SortOrder
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.UnitConverter
import com.slowthemdown.shared.model.VehicleType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    captureViewModel: CaptureViewModel = hiltViewModel(),
    logViewModel: LogViewModel = hiltViewModel(),
) {
    val captureState by captureViewModel.state.collectAsState()
    val showSaved by captureViewModel.showSavedConfirmation.collectAsState()

    // Request location permission on first appear
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* LocationService handles gracefully */ }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Base layer: home content
        HomeContent(captureViewModel, logViewModel)

        // Overlay: capture flow when active
        if (captureState != CaptureFlowState.SELECT_SOURCE) {
            CaptureFlowOverlay(captureViewModel)
        }

        // Saved confirmation
        AnimatedVisibility(
            visible = showSaved,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(24.dp),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    stringResource(R.string.capture_logged),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    captureViewModel: CaptureViewModel,
    logViewModel: LogViewModel,
) {
    val context = LocalContext.current
    val calibration by captureViewModel.calibration.collectAsState()
    val isLoadingVideo by captureViewModel.isLoadingVideo.collectAsState()
    val entries by logViewModel.entries.collectAsState()
    val searchText by logViewModel.searchText.collectAsState()
    val overLimitOnly by logViewModel.overLimitOnly.collectAsState()
    val vehicleTypeFilter by logViewModel.vehicleTypeFilter.collectAsState()
    val sortOrder by logViewModel.sortOrder.collectAsState()
    val showingDemoData by logViewModel.showingDemoData.collectAsState()
    val measurementSystem by logViewModel.measurementSystem.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<SpeedEntryEntity?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { captureViewModel.loadVideo(it) }
    }

    var videoUri by remember { mutableStateOf<Uri?>(null) }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            videoUri?.let { captureViewModel.loadVideo(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val videosDir = File(context.cacheDir, "videos").apply { mkdirs() }
            val videoFile = File(videosDir, "capture_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            videoUri = uri
            videoCaptureLauncher.launch(uri)
        }
    }

    selectedEntry?.let { entry ->
        LogDetailSheet(
            entry = entry,
            system = measurementSystem,
            onDismiss = { selectedEntry = null },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Demo banner
        if (showingDemoData) {
            DemoBanner(onClear = { logViewModel.clearDemoData() })
        }

        // Calibration warning + capture buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Calibration warning
            if (!calibration.isValid) {
                CalibrationWarningCard(
                    color = Color(0xFFFFC107),
                    titleRes = R.string.capture_not_calibrated,
                    hintRes = R.string.capture_not_calibrated_hint,
                )
            } else if (calibration.needsRecalibration) {
                CalibrationWarningCard(
                    color = Color(0xFFFF9800),
                    titleRes = R.string.capture_needs_recalibration,
                    hintRes = R.string.capture_needs_recalibration_hint,
                )
            }

            // Capture buttons
            if (isLoadingVideo) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.capture_loading_video),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.capture_record_video))
                    }
                    OutlinedButton(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.capture_import_library))
                    }
                }
            }
        }

        // Search bar
        TextField(
            value = searchText,
            onValueChange = { logViewModel.setSearchText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text(stringResource(R.string.log_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { logViewModel.setSearchText("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.log_clear_search))
                        }
                    }
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.log_filters))
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (sortOrder == SortOrder.NEWEST_FIRST)
                                                Icons.Default.ArrowDownward
                                            else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (sortOrder == SortOrder.NEWEST_FIRST)
                                                stringResource(R.string.log_newest_first)
                                            else stringResource(R.string.log_oldest_first)
                                        )
                                    }
                                },
                                onClick = {
                                    logViewModel.toggleSortOrder()
                                    showFilterMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilterChip(
                                            selected = overLimitOnly,
                                            onClick = { logViewModel.setOverLimitOnly(!overLimitOnly) },
                                            label = { Text(stringResource(R.string.log_over_limit_only)) },
                                        )
                                    }
                                },
                                onClick = {
                                    logViewModel.setOverLimitOnly(!overLimitOnly)
                                    showFilterMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(vehicleTypeFilter?.label ?: stringResource(R.string.log_all_vehicles))
                                    }
                                },
                                onClick = {},
                            )
                            if (vehicleTypeFilter != null) {
                                DropdownMenuItem(
                                    text = { Text("  ${stringResource(R.string.log_all_vehicles)}") },
                                    onClick = {
                                        logViewModel.setVehicleTypeFilter(null)
                                        showFilterMenu = false
                                    },
                                )
                            }
                            VehicleType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text("  ${type.label}") },
                                    onClick = {
                                        logViewModel.setVehicleTypeFilter(type)
                                        showFilterMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        // Log entries
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val hasFilters = searchText.isNotEmpty() || overLimitOnly || vehicleTypeFilter != null
                Icon(
                    if (hasFilters) Icons.Default.Search else Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (hasFilters) stringResource(R.string.log_no_matching_entries) else stringResource(R.string.log_no_entries),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (hasFilters) stringResource(R.string.log_no_matching_hint)
                    else stringResource(R.string.log_no_entries_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(entries, key = { it.id }) { entry ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            logViewModel.delete(entry)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                    MaterialTheme.colorScheme.error
                                else Color.Transparent,
                                label = "dismiss-bg",
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.log_delete),
                                    tint = Color.White,
                                )
                            }
                        },
                    ) {
                        SpeedEntryCard(entry, measurementSystem, onClick = { selectedEntry = entry })
                    }
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }
    }
}

@Composable
private fun CalibrationWarningCard(color: Color, titleRes: Int, @Suppress("UNUSED_PARAMETER") hintRes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            stringResource(titleRes),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

@Composable
private fun SpeedEntryCard(
    entry: SpeedEntryEntity,
    system: MeasurementSystem,
    onClick: () -> Unit = {},
) {
    val directionIcon = when (entry.direction) {
        TravelDirection.TOWARD -> "\u2B07\uFE0F"
        TravelDirection.AWAY -> "\u2B06\uFE0F"
        TravelDirection.LEFT_TO_RIGHT -> "\u27A1\uFE0F"
        TravelDirection.RIGHT_TO_LEFT -> "\u2B05\uFE0F"
    }
    val speedUnit = UnitConverter.speedUnit(system)
    val limitDisplay = UnitConverter.displaySpeed(entry.speedLimit, system).toInt()

    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeedBadge(
                speed = entry.speed,
                speedLimit = entry.speedLimit,
                system = system,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (entry.streetName.isNotEmpty()) {
                    Text(entry.streetName, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.vehicleType.label} $directionIcon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.log_speed_limit, limitDisplay, speedUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

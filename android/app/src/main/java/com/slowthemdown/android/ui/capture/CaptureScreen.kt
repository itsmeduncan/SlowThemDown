package com.slowthemdown.android.ui.capture

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.R
import com.slowthemdown.android.service.HapticManager
import com.slowthemdown.android.viewmodel.CaptureFlowState
import com.slowthemdown.android.viewmodel.CaptureViewModel
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.RoadStandards
import com.slowthemdown.shared.model.SpeedCategory
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.UnitConverter
import com.slowthemdown.shared.model.VehicleCategory
import com.slowthemdown.shared.model.VehicleReferences
import com.slowthemdown.shared.model.VehicleType
import java.io.File

@Composable
fun CaptureScreen(viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // Request location permission on first appear (matching iOS)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results not needed — LocationService handles gracefully */ }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    when (state) {
        CaptureFlowState.SELECT_SOURCE -> SelectSourceContent(viewModel)
        CaptureFlowState.SELECT_FRAMES -> FrameSelectorContent(viewModel)
        CaptureFlowState.MARK_FRAME1 -> FrameMarkerContent(viewModel, frameNumber = 1)
        CaptureFlowState.MARK_FRAME2 -> FrameMarkerContent(viewModel, frameNumber = 2)
        CaptureFlowState.RESULT -> SpeedResultContent(viewModel)
        CaptureFlowState.RECORDING -> RecordingContent(viewModel)
    }
}

@Composable
private fun SelectSourceContent(viewModel: CaptureViewModel) {
    val context = LocalContext.current
    val calibration by viewModel.calibration.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadVideo(it) }
    }

    var videoUri by remember { mutableStateOf<Uri?>(null) }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            videoUri?.let { viewModel.loadVideo(it) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.capture_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Calibration status warning
        if (!calibration.isValid) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFC107).copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.capture_not_calibrated),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFFFC107),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.capture_not_calibrated_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFC107),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { videoPickerLauncher.launch("video/*") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.capture_import_library))
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.capture_record_video))
        }
    }
}

@Composable
private fun FrameSelectorContent(viewModel: CaptureViewModel) {
    val duration by viewModel.videoDurationSeconds.collectAsState()
    val t1 by viewModel.frame1Time.collectAsState()
    val t2 by viewModel.frame2Time.collectAsState()
    val timeDelta = kotlin.math.abs(t2 - t1)
    val canExtract = timeDelta >= 0.01

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.capture_select_frames), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.capture_frame_time, 1, t1))
        androidx.compose.material3.Slider(
            value = t1.toFloat(),
            onValueChange = { viewModel.setFrame1Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Text(stringResource(R.string.capture_frame_time, 2, t2))
        androidx.compose.material3.Slider(
            value = t2.toFloat(),
            onValueChange = { viewModel.setFrame2Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.capture_time_delta, timeDelta),
            style = MaterialTheme.typography.bodySmall,
            color = if (canExtract) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error,
        )
        if (!canExtract) {
            Text(
                stringResource(R.string.capture_frames_min_apart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.extractFrames() },
            enabled = canExtract,
        ) {
            Text(stringResource(R.string.capture_extract_frames))
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { viewModel.reset() }) {
            Text(stringResource(R.string.capture_cancel))
        }
    }
}

@Composable
private fun FrameMarkerContent(viewModel: CaptureViewModel, frameNumber: Int) {
    val frame1Image by viewModel.frame1Image.collectAsState()
    val frame2Image by viewModel.frame2Image.collectAsState()
    val frame1Marker by viewModel.frame1Marker.collectAsState()
    val frame2Marker by viewModel.frame2Marker.collectAsState()
    val useVehicleRef by viewModel.useVehicleReference.collectAsState()
    val selectedVehicleRef by viewModel.selectedVehicleRef.collectAsState()
    val vehicleRefMarkers by viewModel.vehicleRefMarkers.collectAsState()
    val system by viewModel.measurementSystem.collectAsState()

    val bitmap = if (frameNumber == 1) frame1Image else frame2Image
    val marker = if (frameNumber == 1) frame1Marker else frame2Marker
    val markerColor = if (frameNumber == 1) Color(0xFF42A5F5) else Color(0xFFFF9800)
    val hasMarker = marker != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.capture_mark_frame, frameNumber),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.capture_tap_vehicle_position, frameNumber),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (bitmap != null) {
            FrameImageWithMarker(
                bitmap = bitmap,
                marker = marker,
                markerColor = markerColor,
                frameNumber = frameNumber,
                onTap = { viewPoint, viewSize ->
                    if (frameNumber == 1) viewModel.addMarkerFrame1(viewPoint, viewSize)
                    else viewModel.addMarkerFrame2(viewPoint, viewSize)
                },
            )
        } else {
            Text(
                stringResource(R.string.capture_frame_not_available),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Vehicle reference section on Frame 2 (matching iOS)
        if (frameNumber == 2) {
            Spacer(modifier = Modifier.height(16.dp))
            VehicleReferenceSection(viewModel, system)
        }

        // Vehicle reference markers on frame 2
        if (frameNumber == 2 && useVehicleRef && bitmap != null) {
            Spacer(modifier = Modifier.height(8.dp))
            VehicleRefMarkerOverlay(
                bitmap = bitmap,
                markers = vehicleRefMarkers,
                selectedRef = selectedVehicleRef,
                onTap = { viewPoint, viewSize -> viewModel.addVehicleRefMarker(viewPoint, viewSize) },
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        if (frameNumber == 1) {
            Button(
                onClick = { viewModel.advanceToMarkFrame2() },
                enabled = hasMarker,
            ) {
                Text(stringResource(R.string.capture_next_mark_frame2))
            }
        } else {
            Button(
                onClick = { viewModel.calculateSpeed() },
                enabled = hasMarker,
            ) {
                Text(stringResource(R.string.capture_calculate_speed))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { viewModel.reset() }) {
            Text(stringResource(R.string.capture_cancel))
        }
    }
}

@Composable
private fun FrameImageWithMarker(
    bitmap: android.graphics.Bitmap,
    marker: Point?,
    markerColor: Color,
    frameNumber: Int,
    onTap: (Point, Size) -> Unit,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val aspectRatio = if (bitmap.height > 0) {
        bitmap.width.toFloat() / bitmap.height.toFloat()
    } else {
        16f / 9f
    }
    val imageSize = Size(bitmap.width.toDouble(), bitmap.height.toDouble())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
            .onSizeChanged { canvasSize = it }
            .pointerInput(canvasSize) {
                detectTapGestures { offset ->
                    val viewSize = Size(size.width.toDouble(), size.height.toDouble())
                    onTap(Point(offset.x.toDouble(), offset.y.toDouble()), viewSize)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = imageBitmap,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            )

            marker?.let { imagePoint ->
                val viewSize = Size(size.width.toDouble(), size.height.toDouble())
                val vp = CoordinateMapper.imageToView(imagePoint, viewSize, imageSize)
                val center = Offset(vp.x.toFloat(), vp.y.toFloat())

                drawCircle(color = markerColor, radius = 14f, center = center)
                drawCircle(
                    color = Color.White,
                    radius = 14f,
                    center = center,
                    style = Stroke(width = 2f),
                )

                val label = "$frameNumber"
                val textResult = textMeasurer.measure(
                    label,
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White),
                )
                drawText(
                    textResult,
                    topLeft = Offset(
                        center.x - textResult.size.width / 2f,
                        center.y - textResult.size.height / 2f,
                    ),
                )
            }
        }
    }
}

@Composable
private fun VehicleReferenceSection(viewModel: CaptureViewModel, system: MeasurementSystem) {
    val useVehicleRef by viewModel.useVehicleReference.collectAsState()
    val selectedRef by viewModel.selectedVehicleRef.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    val distUnit = UnitConverter.distanceUnit(system)

    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Checkbox(
            checked = useVehicleRef,
            onCheckedChange = { viewModel.setUseVehicleReference(it) },
        )
        Text(
            stringResource(R.string.capture_use_vehicle_reference),
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (useVehicleRef) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { showPicker = true }) {
            Text(selectedRef?.let {
                stringResource(R.string.capture_vehicle_ref_label, it.name, UnitConverter.displayDistance(it.lengthMeters, system), distUnit)
            } ?: stringResource(R.string.capture_select_vehicle))
        }

        DropdownMenu(
            expanded = showPicker,
            onDismissRequest = { showPicker = false },
        ) {
            VehicleReferences.byCategory().forEach { (category, vehicles) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    onClick = {},
                    enabled = false,
                )
                vehicles.forEach { vehicle ->
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.capture_vehicle_ref_item, vehicle.name, UnitConverter.displayDistance(vehicle.lengthMeters, system), distUnit))
                        },
                        onClick = {
                            viewModel.setSelectedVehicleRef(vehicle)
                            showPicker = false
                        },
                    )
                }
            }
        }

        if (selectedRef != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.capture_tap_front_back),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VehicleRefMarkerOverlay(
    bitmap: android.graphics.Bitmap,
    markers: List<Point>,
    selectedRef: com.slowthemdown.shared.model.VehicleReference?,
    onTap: (Point, Size) -> Unit,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val accentColor = Color(0xFF4CAF50)
    val imageSize = Size(bitmap.width.toDouble(), bitmap.height.toDouble())

    val aspectRatio = if (bitmap.height > 0) {
        bitmap.width.toFloat() / bitmap.height.toFloat()
    } else {
        16f / 9f
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.capture_mark_vehicle_ends, selectedRef?.name ?: "vehicle"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
                .onSizeChanged { canvasSize = it }
                .pointerInput(canvasSize) {
                    detectTapGestures { offset ->
                        val viewSize = Size(size.width.toDouble(), size.height.toDouble())
                        onTap(Point(offset.x.toDouble(), offset.y.toDouble()), viewSize)
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = imageBitmap,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                )

                val viewSize = Size(size.width.toDouble(), size.height.toDouble())

                if (markers.size == 2) {
                    val p1 = CoordinateMapper.imageToView(markers[0], viewSize, imageSize)
                    val p2 = CoordinateMapper.imageToView(markers[1], viewSize, imageSize)
                    drawLine(
                        color = accentColor,
                        start = Offset(p1.x.toFloat(), p1.y.toFloat()),
                        end = Offset(p2.x.toFloat(), p2.y.toFloat()),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f)),
                    )
                }

                markers.forEachIndexed { index, imagePoint ->
                    val vp = CoordinateMapper.imageToView(imagePoint, viewSize, imageSize)
                    val center = Offset(vp.x.toFloat(), vp.y.toFloat())

                    drawCircle(color = accentColor, radius = 12f, center = center)
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = center,
                        style = Stroke(width = 2f),
                    )

                    val label = if (index == 0) "F" else "B"
                    val textResult = textMeasurer.measure(
                        label,
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    )
                    drawText(
                        textResult,
                        topLeft = Offset(
                            center.x - textResult.size.width / 2f,
                            center.y - textResult.size.height / 2f,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedResultContent(viewModel: CaptureViewModel) {
    val speed by viewModel.calculatedSpeed.collectAsState()
    val speedLimitMps by viewModel.speedLimit.collectAsState()
    val vehicleType by viewModel.vehicleType.collectAsState()
    val direction by viewModel.direction.collectAsState()
    val streetName by viewModel.streetName.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val system by viewModel.measurementSystem.collectAsState()

    val displaySpeed = UnitConverter.displaySpeed(speed, system)
    val speedUnit = UnitConverter.speedUnit(system)
    val category = SpeedCategory.fromSpeed(speed, speedLimitMps)
    val speedColor = when (category) {
        SpeedCategory.UNDER_LIMIT -> Color(0xFF4CAF50)
        SpeedCategory.MARGINAL -> Color(0xFFFFC107)
        SpeedCategory.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }

    var speedLimitExpanded by remember { mutableStateOf(false) }
    var vehicleTypeExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }

    val speedLimits = RoadStandards.speedLimitsForSystem(system)
    val displaySpeedLimit = UnitConverter.displaySpeed(speedLimitMps, system).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.capture_estimated_speed), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.common_speed_format, displaySpeed),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 72.sp,
            ),
            color = speedColor,
        )
        Text(
            speedUnit,
            style = MaterialTheme.typography.titleLarge,
            color = speedColor,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Speed limit picker
        ExposedDropdownMenuBox(
            expanded = speedLimitExpanded,
            onExpandedChange = { speedLimitExpanded = it },
        ) {
            OutlinedTextField(
                value = stringResource(R.string.capture_speed_limit_value, displaySpeedLimit, speedUnit),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.capture_speed_limit_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speedLimitExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = speedLimitExpanded,
                onDismissRequest = { speedLimitExpanded = false },
            ) {
                speedLimits.forEach { limitMps ->
                    val displayLimit = UnitConverter.displaySpeed(limitMps, system).toInt()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.capture_speed_limit_value, displayLimit, speedUnit)) },
                        onClick = {
                            viewModel.setSpeedLimit(limitMps)
                            speedLimitExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vehicle type picker
        ExposedDropdownMenuBox(
            expanded = vehicleTypeExpanded,
            onExpandedChange = { vehicleTypeExpanded = it },
        ) {
            OutlinedTextField(
                value = vehicleType.label,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.capture_vehicle_type_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleTypeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = vehicleTypeExpanded,
                onDismissRequest = { vehicleTypeExpanded = false },
            ) {
                VehicleType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            viewModel.setVehicleType(type)
                            vehicleTypeExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Direction picker
        ExposedDropdownMenuBox(
            expanded = directionExpanded,
            onExpandedChange = { directionExpanded = it },
        ) {
            OutlinedTextField(
                value = direction.label,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.capture_direction_label)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = directionExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = directionExpanded,
                onDismissRequest = { directionExpanded = false },
            ) {
                TravelDirection.entries.forEach { dir ->
                    DropdownMenuItem(
                        text = { Text(dir.label) },
                        onClick = {
                            viewModel.setDirection(dir)
                            directionExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nearest intersection
        OutlinedTextField(
            value = streetName,
            onValueChange = { viewModel.setStreetName(it) },
            label = { Text(stringResource(R.string.capture_nearest_intersection)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.setNotes(it) },
            label = { Text(stringResource(R.string.capture_notes_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Measurement details
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                stringResource(R.string.capture_measurement_details),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.capture_time_delta, viewModel.timeDelta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.capture_pixel_displacement, viewModel.pixelDisplacement),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveEntry() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.capture_save_to_log))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.capture_discard))
        }
    }
}

@Composable
private fun RecordingContent(viewModel: CaptureViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                    )
                } catch (e: Exception) {
                    Log.e("CaptureScreen", "Camera bind failed", e)
                }
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.capture_recording),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.reset() }) {
                Text(stringResource(R.string.capture_stop_recording))
            }
        }
    }
}

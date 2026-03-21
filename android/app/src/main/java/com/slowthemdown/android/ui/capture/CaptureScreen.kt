package com.slowthemdown.android.ui.capture

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
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
            text = "Capture Speed",
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
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Not calibrated. Use a vehicle reference or calibrate first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFC107),
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
            Text("Import from Library")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Record Video")
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
        Text("Select Frames", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Frame 1: %.2fs".format(t1))
        androidx.compose.material3.Slider(
            value = t1.toFloat(),
            onValueChange = { viewModel.setFrame1Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Text("Frame 2: %.2fs".format(t2))
        androidx.compose.material3.Slider(
            value = t2.toFloat(),
            onValueChange = { viewModel.setFrame2Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Time delta: %.3fs".format(timeDelta),
            style = MaterialTheme.typography.bodySmall,
            color = if (canExtract) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error,
        )
        if (!canExtract) {
            Text(
                "Frames must be at least 10ms apart",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.extractFrames() },
            enabled = canExtract,
        ) {
            Text("Extract Frames")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { viewModel.reset() }) {
            Text("Cancel")
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
            "Mark Frame $frameNumber",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tap the vehicle position on Frame $frameNumber",
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
                "(Frame not available)",
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
                Text("Next: Mark Frame 2")
            }
        } else {
            Button(
                onClick = { viewModel.calculateSpeed() },
                enabled = hasMarker,
            ) {
                Text("Calculate Speed")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { viewModel.reset() }) {
            Text("Cancel")
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
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val viewSize = Size(canvasSize.width.toDouble(), canvasSize.height.toDouble())
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
            "Use vehicle reference instead of calibration",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (useVehicleRef) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { showPicker = true }) {
            Text(selectedRef?.let {
                "${it.name} (${"%.1f".format(UnitConverter.displayDistance(it.lengthMeters, system))} $distUnit)"
            } ?: "Select Vehicle")
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
                            Text("  ${vehicle.name} (${"%.1f".format(UnitConverter.displayDistance(vehicle.lengthMeters, system))} $distUnit)")
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
                "Tap the front and back of the vehicle on the frame above to mark its length",
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
            "Mark vehicle ends (${selectedRef?.name ?: "vehicle"})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val viewSize = Size(canvasSize.width.toDouble(), canvasSize.height.toDouble())
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
        Text("Estimated Speed", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "%.1f".format(displaySpeed),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = speedColor,
        )
        Text(
            speedUnit,
            style = MaterialTheme.typography.titleMedium,
            color = speedColor,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Speed limit picker
        ExposedDropdownMenuBox(
            expanded = speedLimitExpanded,
            onExpandedChange = { speedLimitExpanded = it },
        ) {
            OutlinedTextField(
                value = "$displaySpeedLimit $speedUnit",
                onValueChange = {},
                readOnly = true,
                label = { Text("Speed Limit") },
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
                        text = { Text("$displayLimit $speedUnit") },
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
                label = { Text("Vehicle Type") },
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
                label = { Text("Direction") },
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
            label = { Text("Nearest Intersection") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { viewModel.setNotes(it) },
            label = { Text("Notes (optional)") },
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
                "Measurement Details",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Time delta: %.3fs".format(viewModel.timeDelta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Pixel displacement: %.1f px".format(viewModel.pixelDisplacement),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.saveEntry() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save to Log")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Discard")
        }
    }
}

@Composable
private fun RecordingContent(viewModel: CaptureViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Recording...", style = MaterialTheme.typography.headlineLarge)
        // TODO: CameraX preview
    }
}

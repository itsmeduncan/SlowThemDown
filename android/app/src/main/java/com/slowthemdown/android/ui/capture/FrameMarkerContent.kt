package com.slowthemdown.android.ui.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.CaptureViewModel
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.UnitConverter
import com.slowthemdown.shared.model.VehicleReferences

@Composable
internal fun FrameMarkerContent(viewModel: CaptureViewModel, frameNumber: Int) {
    val frame1Image by viewModel.frame1Image.collectAsState()
    val frame2Image by viewModel.frame2Image.collectAsState()
    val frame1Marker by viewModel.frame1Marker.collectAsState()
    val frame2Marker by viewModel.frame2Marker.collectAsState()
    val useVehicleRef by viewModel.useVehicleReference.collectAsState()
    val selectedVehicleRef by viewModel.selectedVehicleRef.collectAsState()
    val vehicleRefMarkers by viewModel.vehicleRefMarkers.collectAsState()
    val system by viewModel.measurementSystem.collectAsState()
    val isCalculating by viewModel.isCalculating.collectAsState()

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
                enabled = hasMarker && !isCalculating,
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

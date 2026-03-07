package com.slowthemdown.android.ui.calibrate

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.viewmodel.CalibrationViewModel
import com.slowthemdown.shared.calculator.CoordinateMapper
import com.slowthemdown.shared.calculator.Point
import com.slowthemdown.shared.calculator.Size
import com.slowthemdown.shared.model.RoadStandards

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalibrateScreen(viewModel: CalibrationViewModel = hiltViewModel()) {
    val calibration by viewModel.calibration.collectAsState()
    val bitmap by viewModel.selectedImageBitmap.collectAsState()
    val markers by viewModel.markers.collectAsState()
    val imageSize by viewModel.imageSize.collectAsState()
    val pixelDist by viewModel.pixelDistance.collectAsState()
    val canSave by viewModel.canSave.collectAsState()
    val context = LocalContext.current

    var distanceText by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { bmp ->
                    viewModel.setImage(bmp)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (calibration.isValid) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
                        Text("Calibrated", style = MaterialTheme.typography.titleMedium)
                    } else {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFC107))
                        Text("Not Calibrated", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (calibration.isValid) {
                    Text(
                        "%.1f px/ft".format(calibration.pixelsPerFoot),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "Take a photo of a scene with a known distance, then mark two points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (calibration.isValid && bitmap == null) {
            OutlinedButton(
                onClick = { viewModel.clearCalibration() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Reset Calibration")
            }
        }

        // Step 1: Pick image
        if (bitmap == null) {
            Text("Step 1: Select a reference image", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose Photo")
            }
        }

        // Step 2: Mark points on image
        if (bitmap != null) {
            Text(
                "Step 2: Tap two points with a known distance apart",
                style = MaterialTheme.typography.titleMedium,
            )

            ImageMarkerOverlay(
                bitmap = bitmap!!,
                markers = markers,
                imageSize = imageSize,
                onTap = { viewPoint, viewSize -> viewModel.addMarker(viewPoint, viewSize) },
            )

            if (markers.size == 2) {
                Text(
                    "Pixel distance: %.1f px".format(pixelDist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.resetMarkers() }) {
                    Text("Clear Markers")
                }
                OutlinedButton(onClick = { viewModel.clearImage() }) {
                    Text("Change Photo")
                }
            }

            // Step 3: Enter distance
            Text(
                "Step 3: Enter the real-world distance",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = {
                        distanceText = it
                        it.toDoubleOrNull()?.let { d -> viewModel.setReferenceDistance(d) }
                    },
                    label = { Text("Distance in feet") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Text("feet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                "Or use a standard lane width:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoadStandards.allWidths.forEach { (label, feet) ->
                    TextButton(onClick = {
                        distanceText = "%.0f".format(feet)
                        viewModel.setReferenceDistance(feet)
                    }) {
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Button(
                onClick = { viewModel.saveCalibration() },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave,
            ) {
                Text("Save Calibration")
            }
        }
    }
}

@Composable
private fun ImageMarkerOverlay(
    bitmap: android.graphics.Bitmap,
    markers: List<Point>,
    imageSize: Size,
    onTap: (Point, Size) -> Unit,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val textMeasurer = rememberTextMeasurer()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val accentColor = MaterialTheme.colorScheme.primary

    val aspectRatio = if (imageSize.height > 0) {
        (imageSize.width / imageSize.height).toFloat()
    } else {
        16f / 9f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio, matchHeightConstraintsFirst = false)
            .clip(MaterialTheme.shapes.medium)
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

            // Draw line between markers
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

            // Draw markers
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

                val label = "${index + 1}"
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

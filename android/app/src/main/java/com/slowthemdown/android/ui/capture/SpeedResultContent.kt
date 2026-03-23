package com.slowthemdown.android.ui.capture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.CaptureViewModel
import com.slowthemdown.shared.model.RoadStandards
import com.slowthemdown.shared.model.SpeedCategory
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.UnitConverter
import com.slowthemdown.shared.model.VehicleType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeedResultContent(viewModel: CaptureViewModel) {
    val speed by viewModel.calculatedSpeed.collectAsState()
    val speedLimitMps by viewModel.speedLimit.collectAsState()
    val vehicleType by viewModel.vehicleType.collectAsState()
    val direction by viewModel.direction.collectAsState()
    val streetName by viewModel.streetName.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val system by viewModel.measurementSystem.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

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
            enabled = !isSaving,
        ) {
            if (isSaving) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.capture_saving))
                }
            } else {
                Text(stringResource(R.string.capture_save_to_log))
            }
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

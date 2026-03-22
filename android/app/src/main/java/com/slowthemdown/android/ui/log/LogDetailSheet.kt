package com.slowthemdown.android.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slowthemdown.android.R
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailSheet(
    entry: SpeedEntryEntity,
    system: MeasurementSystem,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    val speedUnit = UnitConverter.speedUnit(system)
    val distUnit = UnitConverter.distanceUnit(system)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(stringResource(R.string.log_detail_title), style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(stringResource(R.string.log_detail_section_speed))
            DetailRow(stringResource(R.string.log_detail_speed), stringResource(R.string.log_detail_speed_value, UnitConverter.displaySpeed(entry.speed, system), speedUnit))
            DetailRow(stringResource(R.string.log_detail_speed_limit), stringResource(R.string.log_detail_speed_limit_value, UnitConverter.displaySpeed(entry.speedLimit, system), speedUnit))
            DetailRow(stringResource(R.string.log_detail_status), entry.speedCategory.label)

            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.log_detail_section_details))
            if (entry.streetName.isNotEmpty()) {
                DetailRow(stringResource(R.string.log_detail_street), entry.streetName)
            }
            DetailRow(stringResource(R.string.log_detail_vehicle), entry.vehicleType.label)
            DetailRow(stringResource(R.string.log_detail_direction), entry.direction.label)
            DetailRow(stringResource(R.string.log_detail_date), dateFormat.format(Date(entry.timestamp)))

            if (entry.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(stringResource(R.string.log_detail_section_notes))
                Text(
                    entry.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.log_detail_section_measurement))
            DetailRow(stringResource(R.string.log_detail_time_delta), stringResource(R.string.log_detail_time_delta_value, entry.timeDeltaSeconds))
            DetailRow(stringResource(R.string.log_detail_pixel_displacement), stringResource(R.string.log_detail_pixel_displacement_value, entry.pixelDisplacement))
            DetailRow(
                stringResource(R.string.log_detail_pixels_per_unit, distUnit.replaceFirstChar { it.uppercase() }),
                stringResource(R.string.log_detail_pixels_per_unit_value, UnitConverter.displayPixelsPerUnit(entry.pixelsPerMeter, system))
            )
            DetailRow(
                stringResource(R.string.log_detail_reference_distance),
                stringResource(R.string.log_detail_reference_distance_value, UnitConverter.displayDistance(entry.referenceDistanceMeters, system), distUnit)
            )
            DetailRow(stringResource(R.string.log_detail_calibration), entry.calibrationMethod.label)

            val lat = entry.latitude
            val lon = entry.longitude
            if (lat != null && lon != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader(stringResource(R.string.log_detail_section_location))
                DetailRow(stringResource(R.string.log_detail_latitude), stringResource(R.string.log_detail_latitude_value, lat))
                DetailRow(stringResource(R.string.log_detail_longitude), stringResource(R.string.log_detail_longitude_value, lon))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
    HorizontalDivider()
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

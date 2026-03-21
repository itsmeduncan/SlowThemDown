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
import androidx.compose.ui.unit.dp
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
            Text("Entry Details", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader("Speed")
            DetailRow("Speed", "%.1f %s".format(UnitConverter.displaySpeed(entry.speed, system), speedUnit))
            DetailRow("Speed Limit", "%.0f %s".format(UnitConverter.displaySpeed(entry.speedLimit, system), speedUnit))
            DetailRow("Status", entry.speedCategory.label)

            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader("Details")
            if (entry.streetName.isNotEmpty()) {
                DetailRow("Street", entry.streetName)
            }
            DetailRow("Vehicle", entry.vehicleType.label)
            DetailRow("Direction", entry.direction.label)
            DetailRow("Date", dateFormat.format(Date(entry.timestamp)))

            if (entry.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Notes")
                Text(
                    entry.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader("Measurement")
            DetailRow("Time Delta", "%.3f s".format(entry.timeDeltaSeconds))
            DetailRow("Pixel Displacement", "%.1f px".format(entry.pixelDisplacement))
            DetailRow(
                "Pixels per ${distUnit.replaceFirstChar { it.uppercase() }}",
                "%.1f".format(UnitConverter.displayPixelsPerUnit(entry.pixelsPerMeter, system))
            )
            DetailRow(
                "Reference Distance",
                "%.1f %s".format(UnitConverter.displayDistance(entry.referenceDistanceMeters, system), distUnit)
            )
            DetailRow("Calibration", entry.calibrationMethod.label)

            if (entry.latitude != null && entry.longitude != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SectionHeader("Location")
                DetailRow("Latitude", "%.6f".format(entry.latitude))
                DetailRow("Longitude", "%.6f".format(entry.longitude))
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

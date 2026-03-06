package com.slowthemdown.android.ui.calibrate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.viewmodel.CalibrationViewModel
import com.slowthemdown.shared.model.CalibrationMethod

@Composable
fun CalibrateScreen(viewModel: CalibrationViewModel = hiltViewModel()) {
    val calibration by viewModel.calibration.collectAsState()
    val method by viewModel.method.collectAsState()
    var distanceText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Calibration", style = MaterialTheme.typography.headlineMedium)

        if (calibration.isValid) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Calibration", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("%.2f pixels/foot".format(calibration.pixelsPerFoot))
                    Text("Method: ${calibration.method.label}")
                }
            }
        }

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CalibrationMethod.entries.forEachIndexed { index, m ->
                SegmentedButton(
                    selected = method == m,
                    onClick = { viewModel.setMethod(m) },
                    shape = SegmentedButtonDefaults.itemShape(index, CalibrationMethod.entries.size),
                ) {
                    Text(m.label)
                }
            }
        }

        when (method) {
            CalibrationMethod.MANUAL_DISTANCE -> {
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = {
                        distanceText = it
                        it.toDoubleOrNull()?.let { d -> viewModel.setReferenceDistance(d) }
                    },
                    label = { Text("Reference Distance (feet)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            CalibrationMethod.VEHICLE_REFERENCE -> {
                Text("Select a vehicle reference, then mark its endpoints in the frame.")
                // TODO: Vehicle reference picker
            }
        }

        Text("Mark two points on the reference distance in the calibration frame.")
        // TODO: Image with tap-to-mark overlay for calibration

        Button(
            onClick = { viewModel.saveCalibration() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save Calibration")
        }
    }
}

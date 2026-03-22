package com.slowthemdown.android.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slowthemdown.android.R
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.TrafficStats
import com.slowthemdown.shared.model.UnitConverter

@Composable
fun V85Card(stats: TrafficStats, speedLimit: Double, system: MeasurementSystem) {
    val v85Display = UnitConverter.displaySpeed(stats.v85, system)
    val limitDisplay = UnitConverter.displaySpeed(speedLimit, system).toInt()
    val isOverLimit = stats.v85 > speedLimit
    val v85Color = if (isOverLimit) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
    val speedUnit = UnitConverter.speedUnit(system)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.reports_v85_speed), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "%.1f".format(v85Display),
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                color = v85Color,
            )
            Text(
                speedUnit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isOverLimit) stringResource(R.string.reports_v85_exceeds_limit, limitDisplay, speedUnit)
                else stringResource(R.string.reports_v85_within_limit, limitDisplay, speedUnit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

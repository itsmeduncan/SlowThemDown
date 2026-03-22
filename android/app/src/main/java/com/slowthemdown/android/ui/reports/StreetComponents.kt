package com.slowthemdown.android.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.StreetGroup
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.UnitConverter

@Composable
fun StreetFilterRow(
    streets: List<String>,
    selectedStreet: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedStreet == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.reports_all_streets)) },
        )
        streets.forEach { street ->
            FilterChip(
                selected = selectedStreet == street,
                onClick = { onSelect(street) },
                label = { Text(street) },
            )
        }
    }
}

@Composable
fun StreetBreakdownSection(
    groups: List<StreetGroup>,
    system: MeasurementSystem,
    onSelectStreet: (String) -> Unit,
) {
    val speedUnit = UnitConverter.speedUnit(system)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.reports_by_street), style = MaterialTheme.typography.titleMedium)
        groups.forEach { group ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectStreet(group.name) },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            group.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            stringResource(R.string.reports_street_summary, group.count, UnitConverter.displaySpeed(group.meanSpeed, system), speedUnit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(R.string.reports_over_limit_percent, group.overLimitPercent),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (group.overLimitPercent > 50) MaterialTheme.colorScheme.error
                        else Color(0xFF4CAF50),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.reports_view),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

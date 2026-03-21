package com.slowthemdown.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.SpeedCategory
import com.slowthemdown.shared.model.UnitConverter

@Composable
fun SpeedBadge(
    speed: Double,
    speedLimit: Double,
    system: MeasurementSystem,
    modifier: Modifier = Modifier,
) {
    val category = SpeedCategory.fromSpeed(speed, speedLimit)
    val color = when (category) {
        SpeedCategory.UNDER_LIMIT -> MaterialTheme.colorScheme.secondary
        SpeedCategory.MARGINAL -> MaterialTheme.colorScheme.tertiary
        SpeedCategory.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }

    val displaySpeed = UnitConverter.displaySpeed(speed, system)
    val unit = UnitConverter.speedUnit(system)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "%.1f %s".format(displaySpeed, unit),
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

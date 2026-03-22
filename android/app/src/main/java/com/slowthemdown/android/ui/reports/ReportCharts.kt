package com.slowthemdown.android.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowthemdown.android.viewmodel.HistogramBucket
import com.slowthemdown.android.viewmodel.HourlyAverage
import com.slowthemdown.android.viewmodel.ScatterPoint
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.UnitConverter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeedHistogramChart(buckets: List<HistogramBucket>) {
    val textMeasurer = rememberTextMeasurer()
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = buckets.maxOf { it.count }.coerceAtLeast(1)

    Card {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .padding(16.dp)
        ) {
            val leftPadding = 30f
            val bottomPadding = 40f
            val chartWidth = size.width - leftPadding
            val chartHeight = size.height - bottomPadding
            val barWidth = chartWidth / buckets.size * 0.7f
            val gap = chartWidth / buckets.size * 0.3f / 2f

            buckets.forEachIndexed { index, bucket ->
                val barHeight = (bucket.count.toFloat() / maxCount) * chartHeight
                val x = leftPadding + index * (chartWidth / buckets.size) + gap
                val y = chartHeight - barHeight

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                )

                // Label
                val labelResult = textMeasurer.measure(
                    bucket.label,
                    style = TextStyle(fontSize = 9.sp, color = labelColor),
                )
                drawText(
                    labelResult,
                    topLeft = Offset(
                        x + barWidth / 2 - labelResult.size.width / 2,
                        chartHeight + 8f,
                    ),
                )
            }
        }
    }
}

@Composable
fun HourlyAverageChart(data: List<HourlyAverage>, system: MeasurementSystem) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = Color(0xFFFF9800)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val displaySpeeds = data.map { UnitConverter.displaySpeed(it.averageSpeed, system) }
    val minSpeed = displaySpeeds.min() - 2
    val maxSpeed = displaySpeeds.max() + 2
    val speedRange = (maxSpeed - minSpeed).coerceAtLeast(1.0)

    Card {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .padding(16.dp)
        ) {
            val leftPadding = 40f
            val bottomPadding = 40f
            val chartWidth = size.width - leftPadding
            val chartHeight = size.height - bottomPadding

            fun xFor(index: Int): Float =
                leftPadding + index.toFloat() / (data.size - 1).coerceAtLeast(1) * chartWidth

            fun yFor(displaySpeed: Double): Float =
                (chartHeight - ((displaySpeed - minSpeed) / speedRange * chartHeight)).toFloat()

            // Draw line
            val path = Path()
            displaySpeeds.forEachIndexed { i, speed ->
                val x = xFor(i)
                val y = yFor(speed)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Draw points
            displaySpeeds.forEachIndexed { i, speed ->
                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset(xFor(i), yFor(speed)),
                )
            }

            // Labels (show every few)
            val step = if (data.size > 8) 3 else 1
            data.forEachIndexed { i, point ->
                if (i % step == 0) {
                    val labelResult = textMeasurer.measure(
                        point.label,
                        style = TextStyle(fontSize = 9.sp, color = labelColor),
                    )
                    drawText(
                        labelResult,
                        topLeft = Offset(
                            xFor(i) - labelResult.size.width / 2,
                            chartHeight + 8f,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun ScatterChart(points: List<ScatterPoint>, system: MeasurementSystem) {
    val dotColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val displaySpeeds = points.map { UnitConverter.displaySpeed(it.speed, system) }
    val minTime = points.minOf { it.timestampMillis }
    val maxTime = points.maxOf { it.timestampMillis }
    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
    val minSpeed = displaySpeeds.min() - 2
    val maxSpeed = displaySpeeds.max() + 2
    val speedRange = (maxSpeed - minSpeed).coerceAtLeast(1.0)
    val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())

    Card {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f)
                .padding(16.dp)
        ) {
            val leftPadding = 40f
            val bottomPadding = 40f
            val chartWidth = size.width - leftPadding
            val chartHeight = size.height - bottomPadding

            points.forEachIndexed { i, point ->
                val x = leftPadding + ((point.timestampMillis - minTime).toFloat() / timeRange) * chartWidth
                val y = (chartHeight - ((displaySpeeds[i] - minSpeed) / speedRange * chartHeight)).toFloat()
                drawCircle(
                    color = dotColor,
                    radius = 4f,
                    center = Offset(x, y),
                )
            }

            // Date labels at edges
            val dates = listOf(minTime, (minTime + maxTime) / 2, maxTime)
            dates.forEachIndexed { i, ts ->
                val x = leftPadding + (i.toFloat() / 2f) * chartWidth
                val label = dateFormat.format(Date(ts))
                val labelResult = textMeasurer.measure(
                    label,
                    style = TextStyle(fontSize = 9.sp, color = labelColor),
                )
                drawText(
                    labelResult,
                    topLeft = Offset(x - labelResult.size.width / 2, chartHeight + 8f),
                )
            }
        }
    }
}

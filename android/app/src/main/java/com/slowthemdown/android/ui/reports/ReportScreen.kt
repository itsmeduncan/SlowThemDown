package com.slowthemdown.android.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.ui.components.DemoBanner
import com.slowthemdown.android.viewmodel.HistogramBucket
import com.slowthemdown.android.viewmodel.HourlyAverage
import com.slowthemdown.android.viewmodel.ReportViewModel
import com.slowthemdown.android.viewmodel.ScatterPoint
import com.slowthemdown.android.viewmodel.StreetGroup
import com.slowthemdown.shared.model.TrafficStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsState()
    val histogram by viewModel.histogram.collectAsState()
    val hourlyAverages by viewModel.hourlyAverages.collectAsState()
    val scatterPoints by viewModel.scatterPoints.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val mostCommonSpeedLimit by viewModel.mostCommonSpeedLimit.collectAsState()
    val showingDemoData by viewModel.showingDemoData.collectAsState()
    val selectedStreet by viewModel.selectedStreet.collectAsState()
    val availableStreets by viewModel.availableStreets.collectAsState()
    val streetGroups by viewModel.streetGroups.collectAsState()
    val showAgencyPicker by viewModel.showAgencyPicker.collectAsState()
    val matchedAgencies by viewModel.matchedAgencies.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exportedFile) {
        exportedFile?.let { file ->
            viewModel.shareFile(context, file)
            viewModel.clearExportedFile()
        }
    }

    if (stats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "No data for reports. Capture some speed measurements!",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val s = stats!!

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showingDemoData) {
            DemoBanner(onClear = { viewModel.clearDemoData() })
        }

        Text("Traffic Report", style = MaterialTheme.typography.headlineMedium)

        // Street Filter
        if (availableStreets.size > 1) {
            StreetFilterRow(
                streets = availableStreets,
                selectedStreet = selectedStreet,
                onSelect = { viewModel.selectStreet(it) },
            )
        }

        // V85 Card
        V85Card(stats = s, speedLimit = mostCommonSpeedLimit)

        // Metrics Grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val halfWidth = Modifier.weight(1f)
            MetricCard("Mean Speed", "%.1f".format(s.mean), "MPH", modifier = halfWidth)
            MetricCard("Median Speed", "%.1f".format(s.median), "MPH", modifier = halfWidth)
            MetricCard("Total Entries", "${s.count}", "", modifier = halfWidth)
            MetricCard(
                "Over Limit",
                "${s.overLimitCount} (%.0f%%)".format(s.overLimitPercent),
                "",
                color = if (s.overLimitPercent > 50) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondary,
                modifier = halfWidth,
            )
        }

        // Speed Distribution Histogram
        if (histogram.isNotEmpty()) {
            Text("Speed Distribution", style = MaterialTheme.typography.titleMedium)
            SpeedHistogramChart(buckets = histogram)
        }

        // Average Speed by Hour
        if (hourlyAverages.size >= 2) {
            Text("Average Speed by Hour", style = MaterialTheme.typography.titleMedium)
            HourlyAverageChart(data = hourlyAverages)
        }

        // Speeds Over Time
        if (scatterPoints.size >= 2) {
            Text("Speeds Over Time", style = MaterialTheme.typography.titleMedium)
            ScatterChart(points = scatterPoints)
        }

        // Street Breakdown
        if (selectedStreet == null && streetGroups.size > 1) {
            StreetBreakdownSection(
                groups = streetGroups,
                onSelectStreet = { viewModel.selectStreet(it) },
            )
        }

        // Export buttons
        Text("Export", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.exportCsv() }) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CSV")
            }
            OutlinedButton(onClick = { viewModel.exportPdf() }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("PDF")
            }
        }
        OutlinedButton(onClick = { viewModel.showAgencyPicker() }) {
            Icon(Icons.Default.Business, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Report to Agency")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (isExporting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Generating report…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    } // Box

    if (showAgencyPicker) {
        AgencyPickerSheet(
            agencies = matchedAgencies,
            onSelect = { agency ->
                viewModel.dismissAgencyPicker()
                viewModel.composeAgencyEmail(context, agency)
            },
            onDismiss = { viewModel.dismissAgencyPicker() },
        )
    }
}

@Composable
private fun V85Card(stats: TrafficStats, speedLimit: Int) {
    val isOverLimit = stats.v85 > speedLimit
    val v85Color = if (isOverLimit) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)

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
            Text("V85 Speed", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "%.1f".format(stats.v85),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = v85Color,
            )
            Text(
                "MPH",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "85% of vehicles travel at or below this speed. " +
                    if (isOverLimit) "This exceeds the $speedLimit mph speed limit."
                    else "This is within the $speedLimit mph speed limit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedHistogramChart(buckets: List<HistogramBucket>) {
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
private fun HourlyAverageChart(data: List<HourlyAverage>) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = Color(0xFFFF9800)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val minSpeed = data.minOf { it.averageSpeed } - 2
    val maxSpeed = data.maxOf { it.averageSpeed } + 2
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

            fun yFor(speed: Double): Float =
                (chartHeight - ((speed - minSpeed) / speedRange * chartHeight)).toFloat()

            // Draw line
            val path = Path()
            data.forEachIndexed { i, point ->
                val x = xFor(i)
                val y = yFor(point.averageSpeed)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // Draw points
            data.forEachIndexed { i, point ->
                drawCircle(
                    color = lineColor,
                    radius = 5f,
                    center = Offset(xFor(i), yFor(point.averageSpeed)),
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
private fun ScatterChart(points: List<ScatterPoint>) {
    val dotColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val minTime = points.minOf { it.timestampMillis }
    val maxTime = points.maxOf { it.timestampMillis }
    val timeRange = (maxTime - minTime).coerceAtLeast(1L)
    val minSpeed = points.minOf { it.speedMPH } - 2
    val maxSpeed = points.maxOf { it.speedMPH } + 2
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

            points.forEach { point ->
                val x = leftPadding + ((point.timestampMillis - minTime).toFloat() / timeRange) * chartWidth
                val y = (chartHeight - ((point.speedMPH - minSpeed) / speedRange * chartHeight)).toFloat()
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

@Composable
private fun StreetFilterRow(
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
            label = { Text("All Streets") },
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
private fun StreetBreakdownSection(
    groups: List<StreetGroup>,
    onSelectStreet: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("By Street", style = MaterialTheme.typography.titleMedium)
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
                            "${group.count} entries · Avg ${"%.1f".format(group.meanSpeed)} MPH",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${"%.0f".format(group.overLimitPercent)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (group.overLimitPercent > 50) MaterialTheme.colorScheme.error
                        else Color(0xFF4CAF50),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.R
import com.slowthemdown.android.ui.components.DemoBanner
import com.slowthemdown.android.viewmodel.HistogramBucket
import com.slowthemdown.android.viewmodel.HourlyAverage
import com.slowthemdown.android.viewmodel.ReportViewModel
import com.slowthemdown.android.viewmodel.ScatterPoint
import com.slowthemdown.android.viewmodel.StreetGroup
import com.slowthemdown.shared.model.MeasurementSystem
import com.slowthemdown.shared.model.TrafficStats
import com.slowthemdown.shared.model.UnitConverter
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
    val system by viewModel.measurementSystem.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exportedFile) {
        exportedFile?.let { file ->
            viewModel.shareFile(context, file)
            viewModel.clearExportedFile()
        }
    }

    if (stats == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.reports_no_data), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.reports_no_data_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val s = stats!!
    val speedUnit = UnitConverter.speedUnit(system)

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

        Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineMedium)

        // Street Filter
        if (availableStreets.size > 1) {
            StreetFilterRow(
                streets = availableStreets,
                selectedStreet = selectedStreet,
                onSelect = { viewModel.selectStreet(it) },
            )
        }

        // V85 Card
        V85Card(stats = s, speedLimit = mostCommonSpeedLimit, system = system)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Metrics Grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val halfWidth = Modifier.weight(1f)
            MetricCard(stringResource(R.string.reports_mean_speed), "%.1f".format(UnitConverter.displaySpeed(s.mean, system)), speedUnit, modifier = halfWidth)
            MetricCard(stringResource(R.string.reports_median_speed), "%.1f".format(UnitConverter.displaySpeed(s.median, system)), speedUnit, modifier = halfWidth)
            MetricCard(stringResource(R.string.reports_total_entries), "${s.count}", "", modifier = halfWidth)
            MetricCard(
                stringResource(R.string.reports_over_limit),
                "${s.overLimitCount} (%.0f%%)".format(s.overLimitPercent),
                "",
                color = if (s.overLimitPercent > 50) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.secondary,
                modifier = halfWidth,
            )
        }

        // Speed Distribution Histogram
        if (histogram.isNotEmpty()) {
            Text(stringResource(R.string.reports_speed_distribution), style = MaterialTheme.typography.titleMedium)
            SpeedHistogramChart(buckets = histogram)
        }

        // Average Speed by Hour
        if (hourlyAverages.size >= 2) {
            Text(stringResource(R.string.reports_avg_speed_by_hour), style = MaterialTheme.typography.titleMedium)
            HourlyAverageChart(data = hourlyAverages, system = system)
        }

        // Speeds Over Time
        if (scatterPoints.size >= 2) {
            Text(stringResource(R.string.reports_speeds_over_time), style = MaterialTheme.typography.titleMedium)
            ScatterChart(points = scatterPoints, system = system)
        }

        // Street Breakdown
        if (selectedStreet == null && streetGroups.size > 1) {
            StreetBreakdownSection(
                groups = streetGroups,
                system = system,
                onSelectStreet = { viewModel.selectStreet(it) },
            )
        }

        // Export & Actions
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(stringResource(R.string.reports_export_actions), style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.exportCsv() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.reports_csv))
                    }
                    OutlinedButton(
                        onClick = { viewModel.exportPdf() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.reports_pdf))
                    }
                }
                OutlinedButton(
                    onClick = { viewModel.showAgencyPicker() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Business, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.reports_report_to_agency))
                }
            }
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
                    Text(stringResource(R.string.reports_generating), style = MaterialTheme.typography.bodyMedium)
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
private fun V85Card(stats: TrafficStats, speedLimit: Double, system: MeasurementSystem) {
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
private fun HourlyAverageChart(data: List<HourlyAverage>, system: MeasurementSystem) {
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
private fun ScatterChart(points: List<ScatterPoint>, system: MeasurementSystem) {
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
private fun StreetBreakdownSection(
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

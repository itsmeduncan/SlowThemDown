package com.slowthemdown.android.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.R
import com.slowthemdown.android.ui.components.DemoBanner
import com.slowthemdown.android.viewmodel.ReportViewModel
import com.slowthemdown.shared.model.UnitConverter

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

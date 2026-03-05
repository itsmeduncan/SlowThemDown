package com.slowdown.android.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowdown.android.viewmodel.ReportViewModel
import com.slowdown.shared.model.TrafficStats

@Composable
fun ReportScreen(viewModel: ReportViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsState()

    if (stats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("No data for reports", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        StatsContent(stats!!)
    }
}

@Composable
private fun StatsContent(stats: TrafficStats) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Traffic Report", style = MaterialTheme.typography.headlineMedium)
        MetricRow("Total Entries", stats.count.toString())
        MetricRow("Mean Speed", "%.1f MPH".format(stats.mean))
        MetricRow("Median Speed", "%.1f MPH".format(stats.median))
        MetricRow("V85", "%.1f MPH".format(stats.v85))
        MetricRow("Min / Max", "%.1f / %.1f MPH".format(stats.min, stats.max))
        MetricRow("Std Deviation", "%.1f".format(stats.standardDeviation))
        MetricRow("Over Limit", "${stats.overLimitCount} (%.0f%%)".format(stats.overLimitPercent))
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

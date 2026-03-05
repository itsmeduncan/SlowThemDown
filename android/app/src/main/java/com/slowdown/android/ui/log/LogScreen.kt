package com.slowdown.android.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.slowdown.android.data.db.SpeedEntryEntity
import com.slowdown.android.viewmodel.LogViewModel
import com.slowdown.shared.model.SpeedCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(viewModel: LogViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsState()

    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("No entries yet", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                SpeedEntryCard(entry)
            }
        }
    }
}

@Composable
private fun SpeedEntryCard(entry: SpeedEntryEntity) {
    val color = when (entry.speedCategory) {
        SpeedCategory.UNDER_LIMIT -> MaterialTheme.colorScheme.secondary
        SpeedCategory.MARGINAL -> MaterialTheme.colorScheme.tertiary
        SpeedCategory.OVER_LIMIT -> MaterialTheme.colorScheme.error
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "%.1f MPH".format(entry.speedMPH),
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                )
                Text(
                    "Limit: ${entry.speedLimit}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (entry.streetName.isNotEmpty()) {
                Text(entry.streetName, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
                    .format(Date(entry.timestamp)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

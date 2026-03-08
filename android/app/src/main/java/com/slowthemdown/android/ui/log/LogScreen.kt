package com.slowthemdown.android.ui.log

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.data.db.SpeedEntryEntity
import com.slowthemdown.android.ui.components.SpeedBadge
import com.slowthemdown.android.viewmodel.LogViewModel
import com.slowthemdown.android.viewmodel.SortOrder
import com.slowthemdown.shared.model.TravelDirection
import com.slowthemdown.shared.model.VehicleType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(viewModel: LogViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val overLimitOnly by viewModel.overLimitOnly.collectAsState()
    val vehicleTypeFilter by viewModel.vehicleTypeFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        TextField(
            value = searchText,
            onValueChange = { viewModel.setSearchText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search streets and notes") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchText("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                        ) {
                            // Sort order
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (sortOrder == SortOrder.NEWEST_FIRST)
                                                Icons.Default.ArrowDownward
                                            else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            if (sortOrder == SortOrder.NEWEST_FIRST)
                                                "Newest First"
                                            else "Oldest First"
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.toggleSortOrder()
                                    showFilterMenu = false
                                },
                            )

                            // Over limit filter
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilterChip(
                                            selected = overLimitOnly,
                                            onClick = { viewModel.setOverLimitOnly(!overLimitOnly) },
                                            label = { Text("Over Limit Only") },
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setOverLimitOnly(!overLimitOnly)
                                    showFilterMenu = false
                                },
                            )

                            // Vehicle type filters
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(vehicleTypeFilter?.label ?: "All Vehicles")
                                    }
                                },
                                onClick = {},
                            )
                            // Show "All" option
                            if (vehicleTypeFilter != null) {
                                DropdownMenuItem(
                                    text = { Text("  All Vehicles") },
                                    onClick = {
                                        viewModel.setVehicleTypeFilter(null)
                                        showFilterMenu = false
                                    },
                                )
                            }
                            VehicleType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text("  ${type.label}") },
                                    onClick = {
                                        viewModel.setVehicleTypeFilter(type)
                                        showFilterMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (searchText.isNotEmpty() || overLimitOnly || vehicleTypeFilter != null)
                        "No matching entries"
                    else "No entries yet. Capture some speed measurements!",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(entries, key = { it.id }) { entry ->
                    val dismissState = rememberSwipeToDismissBoxState()

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.delete(entry)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            val color by animateColorAsState(
                                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                    MaterialTheme.colorScheme.error
                                else Color.Transparent,
                                label = "dismiss-bg",
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                )
                            }
                        },
                    ) {
                        SpeedEntryCard(entry)
                    }
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }
        }
    }
}

@Composable
private fun SpeedEntryCard(entry: SpeedEntryEntity) {
    val directionIcon = when (entry.direction) {
        TravelDirection.TOWARD -> "\u2B07\uFE0F"
        TravelDirection.AWAY -> "\u2B06\uFE0F"
        TravelDirection.LEFT_TO_RIGHT -> "\u27A1\uFE0F"
        TravelDirection.RIGHT_TO_LEFT -> "\u2B05\uFE0F"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpeedBadge(
                speedMPH = entry.speedMPH,
                speedLimit = entry.speedLimit,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (entry.streetName.isNotEmpty()) {
                    Text(entry.streetName, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.vehicleType.label} $directionIcon",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${entry.speedLimit} mph limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

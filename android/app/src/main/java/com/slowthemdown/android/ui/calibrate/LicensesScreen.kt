package com.slowthemdown.android.ui.calibrate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slowthemdown.android.R

private data class OpenSourceLicense(
    val name: String,
    val license: String,
)

private val licenses = listOf(
    OpenSourceLicense("Jetpack Compose", "Apache 2.0"),
    OpenSourceLicense("Navigation Compose", "Apache 2.0"),
    OpenSourceLicense("Lifecycle / ViewModel", "Apache 2.0"),
    OpenSourceLicense("Room", "Apache 2.0"),
    OpenSourceLicense("DataStore", "Apache 2.0"),
    OpenSourceLicense("Hilt", "Apache 2.0"),
    OpenSourceLicense("CameraX", "Apache 2.0"),
    OpenSourceLicense("Play Services Location", "Apache 2.0"),
    OpenSourceLicense("ML Kit Face Detection", "Apache 2.0"),
    OpenSourceLicense("ML Kit Text Recognition", "Apache 2.0"),
    OpenSourceLicense("Firebase Crashlytics", "Apache 2.0"),
    OpenSourceLicense("Coil", "Apache 2.0"),
    OpenSourceLicense("Kotlin Coroutines", "Apache 2.0"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onClose: () -> Unit = {}) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_dismiss))
                    }
                },
            )
        },
    ) { innerPadding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.licenses_header),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text(
                stringResource(R.string.licenses_dependencies),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        items(licenses) { dep ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(dep.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        dep.license,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Text(
                stringResource(R.string.licenses_this_app),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Slow Them Down", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.licenses_mit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.licenses_copyright),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    } // Scaffold
}

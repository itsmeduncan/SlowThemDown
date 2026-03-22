package com.slowthemdown.android.ui.capture

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.CaptureViewModel

@Composable
internal fun FrameSelectorContent(viewModel: CaptureViewModel) {
    val duration by viewModel.videoDurationSeconds.collectAsState()
    val t1 by viewModel.frame1Time.collectAsState()
    val t2 by viewModel.frame2Time.collectAsState()
    val isExtracting by viewModel.isExtractingFrames.collectAsState()
    val timeDelta = kotlin.math.abs(t2 - t1)
    val canExtract = timeDelta >= 0.01 && !isExtracting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.capture_select_frames), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.capture_frame_time, 1, t1))
        androidx.compose.material3.Slider(
            value = t1.toFloat(),
            onValueChange = { viewModel.setFrame1Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Text(stringResource(R.string.capture_frame_time, 2, t2))
        androidx.compose.material3.Slider(
            value = t2.toFloat(),
            onValueChange = { viewModel.setFrame2Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.capture_time_delta, timeDelta),
            style = MaterialTheme.typography.bodySmall,
            color = if (canExtract) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.error,
        )
        if (!canExtract) {
            Text(
                stringResource(R.string.capture_frames_min_apart),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.extractFrames() },
            enabled = canExtract,
        ) {
            if (isExtracting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.capture_extracting_frames))
                }
            } else {
                Text(stringResource(R.string.capture_extract_frames))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { viewModel.reset() }) {
            Text(stringResource(R.string.capture_cancel))
        }
    }
}

package com.slowthemdown.android.ui.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.CaptureViewModel

@Composable
internal fun FrameSelectorContent(viewModel: CaptureViewModel) {
    val duration by viewModel.videoDurationSeconds.collectAsState()
    val t1 by viewModel.frame1Time.collectAsState()
    val t2 by viewModel.frame2Time.collectAsState()
    val preview1 by viewModel.previewFrame1.collectAsState()
    val preview2 by viewModel.previewFrame2.collectAsState()
    val isLoadingPreview1 by viewModel.isLoadingPreview1.collectAsState()
    val isLoadingPreview2 by viewModel.isLoadingPreview2.collectAsState()
    val isExtracting by viewModel.isExtractingFrames.collectAsState()
    val timeDelta = kotlin.math.abs(t2 - t1)
    val canExtract = timeDelta >= 0.01 && !isExtracting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.capture_select_frames), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        FrameScrubber(
            frameNumber = 1,
            time = t1,
            duration = duration,
            preview = preview1,
            isLoadingPreview = isLoadingPreview1,
            onTimeChange = { viewModel.setFrame1Time(it) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        FrameScrubber(
            frameNumber = 2,
            time = t2,
            duration = duration,
            preview = preview2,
            isLoadingPreview = isLoadingPreview2,
            onTimeChange = { viewModel.setFrame2Time(it) },
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
        TextButton(onClick = { viewModel.goBack() }) {
            Text(stringResource(R.string.capture_back))
        }
    }
}

@Composable
private fun FrameScrubber(
    frameNumber: Int,
    time: Double,
    duration: Double,
    preview: android.graphics.Bitmap?,
    isLoadingPreview: Boolean,
    onTimeChange: (Double) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FramePreview(bitmap = preview, isLoading = isLoadingPreview)
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.capture_frame_time, frameNumber, time))
        androidx.compose.material3.Slider(
            value = time.toFloat(),
            onValueChange = { onTimeChange(it.toDouble()) },
            valueRange = 0f..duration.coerceAtLeast(0.001).toFloat(),
        )
    }
}

/**
 * Shows the frame at the current scrub position. Keeps the last good image on
 * screen while the next one decodes, so dragging doesn't flash empty.
 */
@Composable
private fun FramePreview(bitmap: android.graphics.Bitmap?, isLoading: Boolean) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

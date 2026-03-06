package com.slowthemdown.android.ui.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.viewmodel.CaptureFlowState
import com.slowthemdown.android.viewmodel.CaptureViewModel

@Composable
fun CaptureScreen(viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    when (state) {
        CaptureFlowState.SELECT_SOURCE -> SelectSourceContent(viewModel)
        CaptureFlowState.SELECT_FRAMES -> FrameSelectorContent(viewModel)
        CaptureFlowState.MARK_FRAME1 -> FrameMarkerContent(viewModel, frameNumber = 1)
        CaptureFlowState.MARK_FRAME2 -> FrameMarkerContent(viewModel, frameNumber = 2)
        CaptureFlowState.RESULT -> SpeedResultContent(viewModel)
        CaptureFlowState.RECORDING -> RecordingContent(viewModel)
    }
}

@Composable
private fun SelectSourceContent(viewModel: CaptureViewModel) {
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadVideo(it) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Capture Speed",
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { videoPickerLauncher.launch("video/*") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import from Library")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { /* TODO: CameraX recording */ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Record Video")
        }
    }
}

@Composable
private fun FrameSelectorContent(viewModel: CaptureViewModel) {
    val duration by viewModel.videoDurationSeconds.collectAsState()
    val t1 by viewModel.frame1Time.collectAsState()
    val t2 by viewModel.frame2Time.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Select Frames", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Frame 1: %.2fs".format(t1))
        androidx.compose.material3.Slider(
            value = t1.toFloat(),
            onValueChange = { viewModel.setFrame1Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Text("Frame 2: %.2fs".format(t2))
        androidx.compose.material3.Slider(
            value = t2.toFloat(),
            onValueChange = { viewModel.setFrame2Time(it.toDouble()) },
            valueRange = 0f..duration.toFloat(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { viewModel.extractFrames() }) {
            Text("Extract Frames")
        }
    }
}

@Composable
private fun FrameMarkerContent(viewModel: CaptureViewModel, frameNumber: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Mark Frame $frameNumber",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap the vehicle position on Frame $frameNumber")
        Spacer(modifier = Modifier.height(16.dp))
        // TODO: Image display with tap-to-mark overlay
        Text("(Frame image and marker overlay will be rendered here)")
        Spacer(modifier = Modifier.height(24.dp))
        if (frameNumber == 1) {
            Button(onClick = { viewModel.advanceToMarkFrame2() }) {
                Text("Next: Mark Frame 2")
            }
        } else {
            Button(onClick = { viewModel.calculateSpeed() }) {
                Text("Calculate Speed")
            }
        }
    }
}

@Composable
private fun SpeedResultContent(viewModel: CaptureViewModel) {
    val speed by viewModel.calculatedSpeed.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Estimated Speed", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "%.1f MPH".format(speed),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { viewModel.saveEntry() }) {
            Text("Save Entry")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { viewModel.reset() }) {
            Text("Discard")
        }
    }
}

@Composable
private fun RecordingContent(viewModel: CaptureViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Recording...", style = MaterialTheme.typography.headlineLarge)
        // TODO: CameraX preview
    }
}

package com.slowthemdown.android.ui.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.viewmodel.CaptureFlowState
import com.slowthemdown.android.viewmodel.CaptureViewModel

@Composable
fun CaptureScreen(viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // Request location permission on first appear (matching iOS)
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results not needed — LocationService handles gracefully */ }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }

    when (state) {
        CaptureFlowState.SELECT_SOURCE -> SelectSourceContent(viewModel)
        CaptureFlowState.SELECT_FRAMES -> FrameSelectorContent(viewModel)
        CaptureFlowState.MARK_FRAME1 -> FrameMarkerContent(viewModel, frameNumber = 1)
        CaptureFlowState.MARK_FRAME2 -> FrameMarkerContent(viewModel, frameNumber = 2)
        CaptureFlowState.RESULT -> SpeedResultContent(viewModel)
        CaptureFlowState.RECORDING -> RecordingContent(viewModel)
    }
}

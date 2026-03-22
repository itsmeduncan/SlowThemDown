package com.slowthemdown.android.ui.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.CaptureFlowState
import com.slowthemdown.android.viewmodel.CaptureViewModel

@Composable
fun CaptureScreen(viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val showSaved by viewModel.showSavedConfirmation.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

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

    Box(modifier = Modifier.fillMaxSize()) {
        when (state) {
            CaptureFlowState.SELECT_SOURCE -> SelectSourceContent(viewModel)
            CaptureFlowState.SELECT_FRAMES -> FrameSelectorContent(viewModel)
            CaptureFlowState.MARK_FRAME1 -> FrameMarkerContent(viewModel, frameNumber = 1)
            CaptureFlowState.MARK_FRAME2 -> FrameMarkerContent(viewModel, frameNumber = 2)
            CaptureFlowState.RESULT -> SpeedResultContent(viewModel)
            CaptureFlowState.RECORDING -> RecordingContent(viewModel)
        }

        errorMessage?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(R.string.common_dismiss))
                    }
                },
            ) {
                Text(msg)
            }
        }

        AnimatedVisibility(
            visible = showSaved,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(24.dp),
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    stringResource(R.string.capture_logged),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

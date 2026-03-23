package com.slowthemdown.android.ui.capture

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.CaptureFlowState
import com.slowthemdown.android.viewmodel.CaptureViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

@Composable
fun CaptureFlowOverlay(viewModel: CaptureViewModel) {
    val state by viewModel.state.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (state) {
                CaptureFlowState.SELECT_SOURCE -> { /* Not shown in overlay */ }
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
        }
    }
}

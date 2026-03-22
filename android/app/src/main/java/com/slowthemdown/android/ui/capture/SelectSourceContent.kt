package com.slowthemdown.android.ui.capture

import android.Manifest
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.slowthemdown.android.R
import com.slowthemdown.android.viewmodel.CaptureViewModel
import java.io.File

@Composable
internal fun SelectSourceContent(viewModel: CaptureViewModel) {
    val context = LocalContext.current
    val calibration by viewModel.calibration.collectAsState()

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadVideo(it) }
    }

    var videoUri by remember { mutableStateOf<Uri?>(null) }

    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            videoUri?.let { viewModel.loadVideo(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            val videosDir = File(context.cacheDir, "videos").apply { mkdirs() }
            val videoFile = File(videosDir, "capture_${System.currentTimeMillis()}.mp4")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )
            videoUri = uri
            videoCaptureLauncher.launch(uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.capture_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Calibration status warning
        if (!calibration.isValid) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFC107).copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.capture_not_calibrated),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFFFFC107),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.capture_not_calibrated_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFC107),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { videoPickerLauncher.launch("video/*") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.capture_import_library))
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.capture_record_video))
        }
    }
}

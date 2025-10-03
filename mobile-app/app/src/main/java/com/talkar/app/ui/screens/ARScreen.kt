package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.talkar.app.ui.components.BasicCameraView
import com.talkar.app.ui.viewmodels.SimpleARViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    viewModel: SimpleARViewModel,
    hasCameraPermission: Boolean = false,
    onPermissionCheck: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val recognizedImage by viewModel.recognizedImage.collectAsState()
    val talkingHeadVideo by viewModel.talkingHeadVideo.collectAsState()
    val recognizedAugmentedImage by viewModel.recognizedAugmentedImage.collectAsState()
    
    // Show permission request UI if camera permission is not granted
    if (!hasCameraPermission) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📷",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TalkAR needs camera access to scan images and show AR overlays. Please grant camera permission to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Please go to Settings > Apps > TalkAR > Permissions and enable Camera access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Debug: hasCameraPermission = $hasCameraPermission",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Force check permission again
                            android.util.Log.d("ARScreen", "Manual permission check requested")
                            onPermissionCheck?.invoke()
                        }
                    ) {
                        Text("Check Permission Again")
                    }
                }
            }
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TalkAR") },
                actions = {
                    if (recognizedImage != null) {
                        TextButton(
                            onClick = { viewModel.resetRecognition() }
                        ) {
                            Text("Reset")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // Basic Camera Preview with AR Overlay - Stable and working
        BasicCameraView(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            onImageRecognized = { imageRecognition ->
                viewModel.recognizeImage(imageRecognition)
            },
            onAugmentedImageRecognized = { augmentedImage ->
                viewModel.setRecognizedAugmentedImage(augmentedImage)
            },
            onError = { errorMessage ->
                viewModel.setArError(errorMessage)
            },
            isImageDetected = recognizedImage != null
        )
    }
}
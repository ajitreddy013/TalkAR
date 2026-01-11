package com.talkar.app.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
// ARCore imports removed for now - using simplified implementation
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.AdContent
import com.talkar.app.ui.components.EmotionalAvatarView // Add import for EmotionalAvatarView
import com.talkar.app.ui.components.StreamingAvatarView // Add import for StreamingAvatarView
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import com.talkar.app.ui.components.AvatarPlaceholder
import kotlinx.coroutines.delay

/**
 * Enhanced AR View with Avatar Overlay Support
 */
@Composable
fun EnhancedARView(
    modifier: Modifier = Modifier,
    onImageDetected: (BackendImage, Avatar?) -> Unit = { _, _ -> },
    onImageLost: (String) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    val viewModel: EnhancedARViewModel = viewModel()
    
    // Observe when an image is detected and call the callback
    val currentImage by viewModel.currentImage.collectAsState()
    val currentAvatar by viewModel.currentAvatar.collectAsState()
    
    // Call onImageDetected when we have a new image
    LaunchedEffect(currentImage, currentAvatar) {
        currentImage?.let { image ->
            onImageDetected(image, currentAvatar)
        }
    }
    
    // Call onImageLost when tracking stops
    val isTracking by viewModel.isTracking.collectAsState()
    var wasTracking by remember { mutableStateOf(false) }
    var lastDetectedImageId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(isTracking, currentImage) {
        // Update the last detected image ID when we have a new image
        currentImage?.id?.let { 
            lastDetectedImageId = it
        }
        
        if (wasTracking && !isTracking && lastDetectedImageId != null) {
            // We were tracking but now we're not - image was lost
            onImageLost(lastDetectedImageId!!)
        }
        wasTracking = isTracking
    }
    
    // Observe ad content state
    val currentAdContent by viewModel.currentAdContent.collectAsState()
    val isAdContentLoading by viewModel.isAdContentLoading.collectAsState()
    val adContentError by viewModel.adContentError.collectAsState()
    
    // Use Simple AR View for now
    SimpleARView(
        modifier = modifier.fillMaxSize(),
        onImageDetected = { imageName ->
            // Simulate image detection
            viewModel.simulateImageDetection()
        },
        onImageLost = {
            // When SimpleARView reports image lost, propagate to our callback
            lastDetectedImageId?.let { onImageLost(it) }
        }
    )
    
    // Avatar Overlay UI
    StreamingAvatarView(
        isVisible = viewModel.isAvatarVisible.collectAsState().value,
        avatar = currentAvatar,
        image = currentImage,
        adContent = currentAdContent,
        isAdContentLoading = isAdContentLoading,
        adContentError = adContentError,
        onAvatarTapped = { viewModel.onAvatarTapped() },
        isTracking = isTracking
    )
}

// ARCore functions removed for simplified implementation

/**
 * Simple AR Camera View for testing
 */
@Composable
fun SimpleARView(
    modifier: Modifier = Modifier,
    onImageDetected: (String) -> Unit = { _ -> },
    onImageLost: () -> Unit = { }
) {
    val context = LocalContext.current
    var isDetecting by remember { mutableStateOf(false) }
    var detectedImage by remember { mutableStateOf<String?>(null) }
    
    // Use the simplified camera preview
    SimplifiedCameraPreview(
        onImageRecognized = { imageRecognition ->
            isDetecting = true
            detectedImage = imageRecognition.name
            onImageDetected(imageRecognition.name)
        },
        onError = { error ->
            Log.e("SimpleARView", "Camera error: $error")
        },
        modifier = modifier.fillMaxSize()
    )
    
    // Camera is now provided by SimplifiedCameraPreview
    // Detection indicator and avatar overlay can still be shown on top
}
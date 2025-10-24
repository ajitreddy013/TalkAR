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
import com.talkar.app.ui.components.EmotionalAvatarView // Add import for EmotionalAvatarView
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
    AvatarOverlayUI(
        isVisible = viewModel.isAvatarVisible.collectAsState().value,
        avatar = currentAvatar,
        image = currentImage,
        onAvatarTapped = { viewModel.onAvatarTapped() },
        isTracking = isTracking
    )
}

// ARCore functions removed for simplified implementation

/**
 * Avatar Overlay UI Component
 */
@Composable
private fun AvatarOverlayUI(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    onAvatarTapped: () -> Unit,
    isTracking: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isVisible && avatar != null && image != null) {
            // Emotional Avatar Overlay
            EmotionalAvatarView(
                isVisible = true,
                avatar = avatar,
                image = image,
                emotion = "neutral", // This will be updated based on dialogue emotion
                isTalking = true, // This will be controlled by video playback
                modifier = Modifier
                    .padding(16.dp)
                    .size(200.dp)
            )
            
            // Detection Status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "🎯 ${image?.name ?: "Image"} Detected",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // Info text below avatar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Tap to replay dialogue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        // Show scanning animation when not tracking
        if (!isTracking) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Simple scanning indicator
                CircularProgressIndicator(
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp
                )
            }
        }
    }
}

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
    
    // Simulate image detection, but only while the composable's lifecycle is at least STARTED
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isLifecycleActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            
            // When lifecycle becomes inactive and we were detecting, report image lost
            if (!isLifecycleActive && isDetecting) {
                onImageLost()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            // When composable is disposed and we were detecting, report image lost
            if (isDetecting) {
                onImageLost()
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isLifecycleActive) {
        if (!isLifecycleActive) return@LaunchedEffect
        // Only run the detection loop while the coroutine scope is active and lifecycle is STARTED
        while (isLifecycleActive) {
            delay(3000) // Simulate detection every 3 seconds
            isDetecting = true
            detectedImage = "Test Image ${System.currentTimeMillis() % 10}"
            onImageDetected(detectedImage ?: "")

            delay(2000) // Show for 2 seconds
            isDetecting = false
            onImageLost() // Report image lost when detection period ends
            detectedImage = null
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Camera placeholder
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📷 AR Camera View",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
        
        // Avatar overlay when image is detected
        if (isDetecting && detectedImage != null) {
            AvatarPlaceholder(
                isVisible = true,
                modifier = Modifier
                    .size(150.dp)
                    .padding(16.dp)
            )
            
            // Detection indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "🎯 $detectedImage Detected",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
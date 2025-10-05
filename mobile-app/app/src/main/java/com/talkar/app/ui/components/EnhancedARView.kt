package com.talkar.app.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
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
    
    // Use Simple AR View for now
    SimpleARView(
        modifier = modifier.fillMaxSize(),
        onImageDetected = { imageName ->
            // Simulate image detection
            viewModel.simulateImageDetection()
        }
    )
    
    // Avatar Overlay UI
    AvatarOverlayUI(
        isVisible = viewModel.isAvatarVisible.value,
        avatar = viewModel.currentAvatar.value,
        image = viewModel.currentImage.value,
        onAvatarTapped = { viewModel.onAvatarTapped() }
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
    onAvatarTapped: () -> Unit
) {
    if (isVisible && avatar != null && image != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Avatar Overlay
            AvatarOverlayView(
                isVisible = true,
                avatar = avatar,
                image = image,
                modifier = Modifier
                    .padding(16.dp)
                    .size(200.dp)
            )
            
            // Detection Status
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "🎯 ${image.name} Detected",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
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
    onImageDetected: (String) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    var isDetecting by remember { mutableStateOf(false) }
    var detectedImage by remember { mutableStateOf<String?>(null) }
    
    // Simulate image detection, but only while the composable's lifecycle is at least STARTED
    val lifecycleOwner = LocalLifecycleOwner.current
    var isLifecycleActive by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isLifecycleActive = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isLifecycleActive) {
        if (!isLifecycleActive) return@LaunchedEffect
        // Only run the detection loop while the coroutine scope is active and lifecycle is STARTED
        while (isActive && isLifecycleActive) {
            delay(3000) // Simulate detection every 3 seconds
            isDetecting = true
            detectedImage = "Test Image ${System.currentTimeMillis() % 10}"
            onImageDetected(detectedImage ?: "")

            delay(2000) // Show for 2 seconds
            isDetecting = false
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
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
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

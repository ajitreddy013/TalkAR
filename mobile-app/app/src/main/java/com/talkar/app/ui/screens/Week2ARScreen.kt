package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.talkar.app.ui.components.EnhancedCameraView
import com.talkar.app.ui.components.FeedbackAvatarOverlay
import com.talkar.app.ui.components.AvatarPlaceholder
import com.talkar.app.ui.screens.AdContentTestScreen
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar

/**
 * Week 2 AR Screen with Static Avatar Overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Week2ARScreen(
    viewModel: EnhancedARViewModel,
    hasCameraPermission: Boolean = false,
    onPermissionCheck: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showTestScreen by remember { mutableStateOf(false) }
    
    if (showTestScreen) {
        AdContentTestScreen(modifier = modifier)
        return
    }
    
    val isAvatarVisible by viewModel.isAvatarVisible.collectAsState()
    val currentAvatar by viewModel.currentAvatar.collectAsState()
    val currentImage by viewModel.currentImage.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val detectionStatus by viewModel.detectionStatus.collectAsState()
    
    // Show permission request UI if camera permission is not granted
    if (!hasCameraPermission) {
        PermissionRequestScreen(
            onPermissionCheck = onPermissionCheck,
            modifier = modifier
        )
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TalkAR - Week 2: Avatar Overlay") },
                actions = {
                    Button(
                        onClick = { viewModel.simulateImageDetection() }
                    ) {
                        Text("Test Detection")
                    }
                    IconButton(
                        onClick = { showTestScreen = true }
                    ) {
                        Text("🧪")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Enhanced camera preview (shows real camera output)
            EnhancedCameraView(
                modifier = Modifier.fillMaxSize(),
                isImageDetected = isTracking,
                onImageRecognized = { imageRecognition ->
                    // When wired to real AR, update the VM with the recognized image
                    viewModel.onImageRecognized(imageRecognition)
                },
                onAugmentedImageRecognized = {
                    // Placeholder hook if using ARCore-backed recognition
                    android.util.Log.d("Week2ARScreen", "Augmented image recognized: ${it.name}")
                },
                onError = { error ->
                    android.util.Log.e("Week2ARScreen", "Camera error: $error")
                }
            )
            
            // Avatar Overlay with Feedback Buttons
            if (isAvatarVisible && currentAvatar != null && currentImage != null) {
                FeedbackAvatarOverlay(
                    isVisible = true,
                    avatar = currentAvatar,
                    image = currentImage,
                    onFeedback = { isPositive ->
                        viewModel.onFeedbackReceived(isPositive)
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            
            // Status Indicator
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTracking) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🎯 AR Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = detectionStatus,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isAvatarVisible) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Avatar: ${currentAvatar?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Image: ${currentImage?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Instructions
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📱 Week 2 Instructions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = MaterialTheme.typography.titleSmall.fontWeight
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Tap 'Test Detection' to simulate image detection",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Avatar overlay appears",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Avatar should track with image movement",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Avatar disappears when image is lost",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Use 👍/👎 buttons to provide feedback on avatar content",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🧪 Tap the test icon to access ad content integration tests",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Permission Request Screen
 */
@Composable
private fun PermissionRequestScreen(
    onPermissionCheck: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
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
                Button(
                    onClick = {
                        android.util.Log.d("Week2ARScreen", "Manual permission check requested")
                        onPermissionCheck?.invoke()
                    }
                ) {
                    Text("Check Permission Again")
                }
            }
        }
    }
}
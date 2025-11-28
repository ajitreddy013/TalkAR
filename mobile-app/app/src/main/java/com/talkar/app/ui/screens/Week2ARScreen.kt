package com.talkar.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.talkar.app.BuildConfig
import com.talkar.app.ui.components.CameraPreviewView
import com.talkar.app.ui.components.FeedbackAvatarOverlay
import com.talkar.app.ui.feedback.FeedbackModal
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.services.BetaFeedbackService
import com.talkar.app.ui.components.OfflineBanner
import kotlinx.coroutines.launch

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
    var showFeedbackModal by remember { mutableStateOf(false) }
    var lastRecognizedImageId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val betaFeedbackService = remember { BetaFeedbackService() }
    val isBeta = BuildConfig.IS_BETA
    
    if (showTestScreen) {
        // Simple test screen without clutter
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Test Screen")
        }
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
                title = { Text("TalkAR") },
                actions = {
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
            // Camera preview (shows real camera output with ARCore integration)
            CameraPreviewView(
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
            
            // Offline Banner
            OfflineBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                onRetry = {
                    // Retry logic - refresh data or reconnect
                    android.util.Log.d("Week2ARScreen", "Retry button clicked")
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
            
            // Beta Feedback Modal (only show in beta builds)
            if (isBeta && showFeedbackModal && lastRecognizedImageId != null) {
                FeedbackModal(
                    posterId = lastRecognizedImageId!!,
                    onDismiss = {
                        showFeedbackModal = false
                    },
                    onSubmit = { rating, comment ->
                        scope.launch {
                            val result = betaFeedbackService.submitFeedbackWithRetry(
                                userId = null, // Anonymous for now
                                posterId = lastRecognizedImageId!!,
                                rating = rating,
                                comment = comment
                            )
                            
                            result.onSuccess {
                                android.util.Log.d("Week2ARScreen", "Feedback submitted successfully with ID: $it")
                                // Show success message to user
                                Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                android.util.Log.e("Week2ARScreen", "Failed to submit feedback", error)
                                // Show error message to user
                                Toast.makeText(context, "Failed to submit feedback. Please try again.", Toast.LENGTH_LONG).show()
                            }
                            
                            showFeedbackModal = false
                        }
                    }
                )
            }
            
            // Trigger feedback modal when avatar becomes invisible (session ends)
            LaunchedEffect(isAvatarVisible, currentImage) {
                val image = currentImage
                if (!isAvatarVisible && image != null && isBeta) {
                    // Avatar just disappeared, show feedback
                    lastRecognizedImageId = image.id
                    showFeedbackModal = true
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
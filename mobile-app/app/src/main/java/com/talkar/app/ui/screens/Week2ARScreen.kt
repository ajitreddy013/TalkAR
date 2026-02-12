package com.talkar.app.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.talkar.app.BuildConfig
import com.talkar.app.ui.components.SimplifiedCameraPreview
import com.talkar.app.ui.components.CaptureController
import com.talkar.app.ui.components.FeedbackAvatarOverlay
import com.talkar.app.ui.feedback.FeedbackModal
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.services.BetaFeedbackService
import com.talkar.app.ui.components.OfflineBanner
import com.talkar.app.data.services.ConversationalARService
import com.talkar.app.data.services.SpeechRecognitionManager
import com.talkar.app.data.services.ImageMatcherService
import com.talkar.app.data.services.ConversationState
import com.talkar.app.ui.components.VideoOverlayView
import kotlinx.coroutines.launch

/**
 * Week 2 AR Screen with Conversational Video Overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Week2ARScreen(
    viewModel: EnhancedARViewModel,
    hasCameraPermission: Boolean = false, // Ignored, handled internally now
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
    
    // Initialize Core Services (Unify instances to sync cooldowns)
    val speechManager = remember { SpeechRecognitionManager(context) }
    val imageMatcher = remember { ImageMatcherService(context) }
    val arService = remember { ConversationalARService(context, speechManager, imageMatcher) }
    
    // 🔥 ALWAYS Ensure cleanup runs at top-level
    DisposableEffect(Unit) {
        onDispose {
            arService.destroy()
            speechManager.destroy()
            imageMatcher.destroy()
        }
    }
    
    // Permission State
    val permissions = remember { 
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
    var permissionsGranted by remember { 
        mutableStateOf(
            permissions.all { 
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED 
            }
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permissionsGranted = perms.values.all { it }
    }
    
    LaunchedEffect(Unit) {
        // Only launch if not all permissions are granted
        if (!permissionsGranted) {
            launcher.launch(permissions)
        }
    }
    
    // Observe Service State
    val arState by arService.state.collectAsState()
    val currentVideoSource by arService.currentVideoSource.collectAsState()
    val isListening by arService.isListening.collectAsState()
    val transcript by arService.transcript.collectAsState()
    val uiMessage by arService.uiMessage.collectAsState()
    val isDetectionPaused by arService.isDetectionPaused.collectAsState()

    // Start scanning on mount
    LaunchedEffect(Unit) {
        arService.startScanning()
    }

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
    
    // Show permission request UI if permissions are not granted
    if (!permissionsGranted) {
        PermissionRequestScreen(
            onPermissionCheck = { launcher.launch(permissions) },
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
                    if (isBeta) {
                         IconButton(
                            onClick = { 
                                // Reset flow
                                arService.reset()
                                arService.startScanning()
                            }
                        ) {
                            Text("🔄")
                        }
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
            // Camera preview using CameraX
            val captureController = remember { com.talkar.app.ui.components.CaptureController() }
            
            SimplifiedCameraPreview(
                modifier = Modifier.fillMaxSize(),
                isPaused = isDetectionPaused,
                imageMatcher = imageMatcher,
                onImageRecognized = { imageRecognition ->
                    // Trigger the conversational flow
                    arService.onObjectDetected(imageRecognition.id, imageRecognition.name)
                    
                    // Also update legacy VM for compatibility if needed
                    viewModel.onImageRecognized(imageRecognition)
                },
                onError = { error ->
                    android.util.Log.e("Week2ARScreen", "Camera error: $error")
                },
                captureController = captureController
            )

            // Interaction Overlay (Green Box & Gesture)
            if (arState == ConversationState.DETECTED) {
                // Gesture Detector
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    // 1. Trigger interaction flow (Intro)
                                    arService.confirmSelection()
                                    
                                    // 2. Capture "Visual Context" for the upcoming query
                                    try {
                                        captureController.capture { bitmap ->
                                            if (bitmap != null) {
                                                android.util.Log.d("Week2ARScreen", "Captured visual context for query")
                                                arService.setContextImage(bitmap)
                                            } else {
                                                android.util.Log.e("Week2ARScreen", "Captured bitmap is null")
                                                Toast.makeText(context, "Failed to capture visual context", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("Week2ARScreen", "Capture execution failed", e)
                                        Toast.makeText(context, "Camera busy. Please try again.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Visual Feedback (Green Box mimicking iOS)
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .border(4.dp, Color.Green, RoundedCornerShape(12.dp))
                    ) {
                        Text(
                            text = "Hold to Interact",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp)
                        )
                    }
                }
            }
            
            // Video Overlay
            if (currentVideoSource != null && (arState == ConversationState.PLAYING_INTRO || arState == ConversationState.PLAYING_RESPONSE)) {
                VideoOverlayView(
                    videoSource = currentVideoSource!!,
                    arService = arService,
                    onVideoCompleted = {
                        arService.onVideoCompleted()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Loading Overlay
            if (arState == ConversationState.LOADING || uiMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiMessage ?: "Loading...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Listening UI
            if (arState == ConversationState.LISTENING) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (transcript.isEmpty()) "Listening..." else transcript,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            
            // Offline Banner
            OfflineBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
                onRetry = {
                    android.util.Log.d("Week2ARScreen", "Retry button clicked")
                }
            )
            
            // Legacy Avatar Overlay (Only show if not in conversational mode)
            if (arState == ConversationState.IDLE || arState == ConversationState.SCANNING) {
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
                                Toast.makeText(context, "Thank you for your feedback!", Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                android.util.Log.e("Week2ARScreen", "Failed to submit feedback", error)
                                Toast.makeText(context, "Failed to submit feedback. Please try again.", Toast.LENGTH_LONG).show()
                            }
                            
                            showFeedbackModal = false
                        }
                    }
                )
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
                    text = "📷 🎙️",
                    style = MaterialTheme.typography.displayLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "TalkAR needs Camera and Microphone access for the interactive experience. Please grant permissions to continue.",
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
                    Text("Grant Permissions")
                }
            }
        }
    }
}
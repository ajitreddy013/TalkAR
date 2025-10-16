package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.talkar.app.ui.components.SimpleARView
import com.talkar.app.ui.components.AnimatedAvatarOverlay
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar

/**
 * Week 4 AR Screen - UI & User Experience
 * Features:
 * - Touch interaction (tap to play/pause)
 * - Script display button
 * - Smooth animations (appear/disappear)
 * - Responsive avatar overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Week4ARScreen(
    viewModel: EnhancedARViewModel,
    hasCameraPermission: Boolean = false,
    onPermissionCheck: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isAvatarVisible by viewModel.isAvatarVisible.collectAsState()
    val currentAvatar by viewModel.currentAvatar.collectAsState()
    val currentImage by viewModel.currentImage.collectAsState()
    val currentDialogue by viewModel.currentDialogue.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val detectionStatus by viewModel.detectionStatus.collectAsState()
    val isVideoPlaying by viewModel.isVideoPlaying.collectAsState()
    
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
                title = { Text("TalkAR - Week 4: UI & UX") },
                actions = {
                    Button(
                        onClick = { viewModel.simulateImageDetection() }
                    ) {
                        Text("Test Detection")
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
            // Simple AR View for testing
            SimpleARView(
                modifier = Modifier.fillMaxSize(),
                onImageDetected = { imageName ->
                    android.util.Log.d("Week4ARScreen", "Image detected: $imageName")
                }
            )
            
            // Animated Avatar Overlay with Touch Interaction
            AnimatedAvatarOverlay(
                isVisible = isAvatarVisible,
                avatar = currentAvatar,
                image = currentImage,
                dialogue = currentDialogue,
                isPlaying = isVideoPlaying,
                onAvatarTapped = { viewModel.onAvatarTapped() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
            
            // Status Card
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
                        style = MaterialTheme.typography.titleMedium
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isVideoPlaying) "▶️ Playing" else "⏸️ Paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isVideoPlaying) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
            
            // Feature Highlights Card
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
                        text = "✨ Week 4 Features",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ Tap avatar to play/pause lip-sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "✅ Show/hide script text button",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "✅ Smooth appear/disappear animations",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "✅ Responsive overlay with status indicators",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "⏸️ Auto-pause when image lost",
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
                    text = "TalkAR needs camera access to scan images and show AR overlays with interactive controls.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        android.util.Log.d("Week4ARScreen", "Manual permission check requested")
                        onPermissionCheck?.invoke()
                    }
                ) {
                    Text("Check Permission Again")
                }
            }
        }
    }
}

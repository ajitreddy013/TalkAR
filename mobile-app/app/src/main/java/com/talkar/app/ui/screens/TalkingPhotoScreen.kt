package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talkar.app.ar.video.models.TalkingPhotoState
import com.talkar.app.ui.components.*
import com.talkar.app.ui.viewmodels.TalkingPhotoViewModel

/**
 * Main screen for the Talking Photo feature with lip-sync.
 * 
 * Displays the AR camera view and handles the complete talking photo flow:
 * 1. Poster detection with human face filter
 * 2. Video generation or cache retrieval
 * 3. Lip-sync video playback with alpha blending
 * 4. Tracking loss/recovery handling
 * 5. Single poster mode with refresh scan
 * 
 * Requirements: 1.1, 1.2, 6.2, 6.3, 8.2, 14.1, 14.2, 14.3, 14.4, 14.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkingPhotoScreen(
    modifier: Modifier = Modifier,
    viewModel: TalkingPhotoViewModel = viewModel()
) {
    android.util.Log.d("TalkingPhotoScreen", "🎬 TalkingPhotoScreen composable entered")
    
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val lipCoordinates by viewModel.lipCoordinates.collectAsState()
    val transform by viewModel.transform.collectAsState()
    
    android.util.Log.d("TalkingPhotoScreen", "📊 State: $state, isTracking: $isTracking")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Talking Photo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AR Camera View
            ArSceneViewComposable(
                modifier = Modifier.fillMaxSize(),
                onPreviewReady = { viewModel.onPreviewReady() },
                onTrackingReady = { viewModel.onTrackingReady() },
                onPosterDetected = { posterId, anchor ->
                    viewModel.onPosterDetected(posterId, anchor)
                },
                onPosterLost = { posterId ->
                    viewModel.onPosterLost(posterId)
                },
                onTrackingUpdate = { trackingData ->
                    viewModel.onTrackingUpdate(trackingData)
                },
                onError = { errorMessage ->
                    viewModel.onError(errorMessage)
                }
            )
            
            // Lip Region Overlay (rendered on top of AR view)
            if (state == TalkingPhotoState.PLAYING && isTracking) {
                LipRegionOverlay(
                    modifier = Modifier.fillMaxSize(),
                    lipCoordinates = lipCoordinates,
                    transform = transform
                )
            }
            
            // UI Overlays
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // State-based UI
                when (state) {
                    TalkingPhotoState.IDLE -> {
                        ScanningInstructionCard()
                    }

                    TalkingPhotoState.PREVIEW_READY,
                    TalkingPhotoState.POSTERS_LOADING,
                    TalkingPhotoState.TRACKING_READY -> {
                        ScanningInstructionCard()
                    }

                    TalkingPhotoState.POSTER_DETECTED -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = "Poster detected. Preparing lip sync...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    TalkingPhotoState.DETECTED,
                    TalkingPhotoState.FETCHING_ARTIFACT -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Checking cache...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    TalkingPhotoState.ARTIFACT_WAITING -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = "Preparing lip sync artifact...",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    TalkingPhotoState.READY -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = "✅ Ready to play",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    
                    TalkingPhotoState.PLAYING -> {
                        if (!isTracking) {
                            AlignPosterMessage()
                        }
                    }

                    TalkingPhotoState.LOST_TRACKING -> {
                        AlignPosterMessage()
                    }
                    
                    TalkingPhotoState.PAUSED -> {
                        if (!isTracking) {
                            AlignPosterMessage()
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Text(
                                    text = "⏸️ Paused",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    TalkingPhotoState.RESUMED -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Text(
                                text = "Tracking resumed",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    
                    TalkingPhotoState.ERROR -> {
                        error?.let { err ->
                            ErrorMessageCard(
                                error = err,
                                onRetry = { viewModel.retry() },
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }

                    TalkingPhotoState.ERROR_RETRYABLE -> {
                        error?.let { err ->
                            ErrorMessageCard(
                                error = err,
                                onRetry = { viewModel.retry() },
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }
                }
            }
            
            // Refresh Scan Button (bottom center)
            if (state == TalkingPhotoState.PLAYING || 
                state == TalkingPhotoState.PAUSED || 
                state == TalkingPhotoState.LOST_TRACKING || 
                state == TalkingPhotoState.READY) {
                RefreshScanButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    onClick = { viewModel.refreshScan() }
                )
            }
        }
    }
}

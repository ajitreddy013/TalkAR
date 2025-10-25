package com.talkar.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.AdContent
import com.talkar.app.data.services.AudioStreamingService
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.FrameLayout
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import android.view.ViewGroup
import androidx.compose.runtime.DisposableEffect

/**
 * Streaming Avatar View that handles partial playback
 * Shows placeholder during buffering and replaces with lipsync video when ready
 */
@Composable
fun StreamingAvatarView(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    adContent: AdContent?,
    isAdContentLoading: Boolean,
    adContentError: String?,
    onAvatarTapped: () -> Unit,
    isTracking: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: EnhancedARViewModel = viewModel()
    
    // State for streaming
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Fade animation for transitions
    val avatarAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    
    // Initialize audio streaming when ad content is available
    LaunchedEffect(adContent?.audioUrl) {
        adContent?.audioUrl?.let { audioUrl ->
            if (audioUrl.isNotEmpty()) {
                Log.d("StreamingAvatarView", "Initializing audio streaming for URL: $audioUrl")
                @Suppress("UnsafeOptInUsageError")
                initializeAudioStreaming(context, audioUrl)
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(avatarAlpha),
        contentAlignment = Alignment.Center
    ) {
        if (isVisible && image != null) {
            // Show different views based on state
            when {
                isAdContentLoading -> {
                    // Loading state
                    LoadingAvatarView()
                }
                adContentError != null -> {
                    // Error state
                    ErrorAvatarView(error = adContentError)
                }
                isBuffering -> {
                    // Buffering state with placeholder
                    BufferingAvatarView(
                        avatar = avatar,
                        image = image
                    )
                }
                adContent?.videoUrl != null && adContent.videoUrl.isNotEmpty() -> {
                    // Show lipsync video when available
                    LipSyncVideoView(
                        videoUrl = adContent.videoUrl,
                        isPlaying = isPlaying,
                        avatar = avatar,
                        image = image
                    )
                }
                adContent?.audioUrl != null && adContent.audioUrl.isNotEmpty() -> {
                    // Show emotional avatar with audio playback
                    EmotionalAvatarView(
                        isVisible = true,
                        avatar = avatar,
                        image = image,
                        emotion = "neutral",
                        isTalking = isPlaying,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(200.dp)
                    )
                }
                else -> {
                    // Default placeholder
                    AvatarPlaceholder(
                        isVisible = true,
                        modifier = Modifier
                            .size(150.dp)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Loading state avatar view
 */
@Composable
private fun LoadingAvatarView() {
    Card(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Buffering state avatar view with placeholder
 */
@Composable
private fun BufferingAvatarView(
    avatar: Avatar?,
    image: BackendImage?
) {
    Card(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Show placeholder avatar while buffering
            AvatarPlaceholder(
                isVisible = true,
                modifier = Modifier
                    .size(150.dp)
                    .padding(16.dp)
            )
            
            // Buffering indicator overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buffering...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Error state avatar view
 */
@Composable
private fun ErrorAvatarView(error: String) {
    Card(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

/**
 * Lip-sync video view
 */
@Composable
private fun LipSyncVideoView(
    videoUrl: String,
    isPlaying: Boolean,
    avatar: Avatar?,
    image: BackendImage?
) {
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    Card(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        exoPlayer = ExoPlayer.Builder(context).build().also { player ->
                            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.playWhenReady = isPlaying
                        }
                        
                        this.player = exoPlayer
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        useController = false // Hide controls for AR overlay
                    }
                },
                update = { playerView ->
                    exoPlayer?.playWhenReady = isPlaying
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Video indicator overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Card(
                    modifier = Modifier.padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎬",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Video",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    
    // Release player when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }
}

/**
 * Initialize audio streaming service for partial playback
 */
@androidx.media3.common.util.UnstableApi
private fun initializeAudioStreaming(context: Context, audioUrl: String) {
    try {
        val streamingService = AudioStreamingService.getInstance(context)
        
        streamingService.startStreaming(audioUrl, object : AudioStreamingService.PlaybackListener {
            override fun onPlaybackStarted() {
                Log.d("StreamingAvatarView", "Audio playback started")
                // Update UI to show playing state
            }
            
            override fun onBufferingStarted() {
                Log.d("StreamingAvatarView", "Audio buffering started")
                // Update UI to show buffering state
            }
            
            override fun onBufferingCompleted() {
                Log.d("StreamingAvatarView", "Audio buffering completed")
                // Update UI to show ready state
            }
            
            override fun onPlaybackCompleted() {
                Log.d("StreamingAvatarView", "Audio playback completed")
                // Update UI to show completion state
            }
            
            override fun onPlaybackError(error: Exception) {
                Log.e("StreamingAvatarView", "Audio playback error", error)
                // Update UI to show error state
            }
        })
    } catch (e: Exception) {
        Log.e("StreamingAvatarView", "Error initializing audio streaming", e)
    }
}
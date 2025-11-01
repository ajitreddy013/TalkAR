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
import com.talkar.app.ui.components.EmotionalAvatarView
import androidx.compose.animation.Crossfade
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Mic
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Enum class for ad content states
 */
enum class AdState {
    DETECTED,
    GENERATING,
    STREAMING_AUDIO,
    PLAYING_VIDEO,
    ERROR
}

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
    var showVideo by remember { mutableStateOf(false) }
    var audioLevel by remember { mutableStateOf(0f) }
    var adState by remember { mutableStateOf(AdState.DETECTED) }
    
    // Feedback state
    var feedbackSent by remember { mutableStateOf(false) }
    
    // Voice recognition state
    var isListening by remember { mutableStateOf(false) }
    var voiceQueryResult by remember { mutableStateOf<String?>(null) }
    
    // Voice recognition launcher
    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = results?.get(0) ?: ""
            voiceQueryResult = recognizedText
            Log.d("StreamingAvatarView", "Voice recognition result: $recognizedText")
            
            // Send the recognized text to the view model for processing
            viewModel.processVoiceQuery(recognizedText, image?.id)
        }
    }
    
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
                adState = AdState.STREAMING_AUDIO
                @Suppress("UnsafeOptInUsageError")
                initializeAudioStreaming(context, audioUrl, isPlaying) { playing ->
                    isPlaying = playing
                    if (playing) {
                        adState = AdState.STREAMING_AUDIO
                    } else {
                        adState = AdState.DETECTED
                    }
                }
            }
        }
    }
    
    // Check if video is ready and switch to video view
    LaunchedEffect(adContent?.videoUrl) {
        if (!adContent?.videoUrl.isNullOrEmpty()) {
            // Add a small delay to ensure smooth transition
            kotlinx.coroutines.delay(500)
            showVideo = true
            adState = AdState.PLAYING_VIDEO
        }
    }
    
    // Handle loading state
    LaunchedEffect(isAdContentLoading) {
        if (isAdContentLoading) {
            adState = AdState.GENERATING
        }
    }
    
    // Handle error state
    LaunchedEffect(adContentError) {
        if (adContentError != null) {
            adState = AdState.ERROR
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
            Crossfade(targetState = showVideo, animationSpec = tween(durationMillis = 500)) { showVideoState ->
                when {
                    adState == AdState.GENERATING -> {
                        // Loading state
                        LoadingAvatarView()
                    }
                    adState == AdState.ERROR -> {
                        // Error state
                        ErrorAvatarView(error = adContentError ?: "Unknown error")
                    }
                    isBuffering -> {
                        // Buffering state with placeholder
                        BufferingAvatarView(
                            avatar = avatar,
                            image = image
                        )
                    }
                    showVideoState && adContent?.videoUrl != null && adContent.videoUrl.isNotEmpty() -> {
                        // Show lipsync video when available with smooth transition
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
                            audioLevel = audioLevel,
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
            
            // Add state indicator overlay
            StateIndicatorOverlay(adState = adState)
            
            // Add feedback buttons when ad content is available and feedback hasn't been sent
            if (adContent != null && !feedbackSent && (adState == AdState.STREAMING_AUDIO || adState == AdState.PLAYING_VIDEO)) {
                FeedbackButtons(
                    imageId = image.id,
                    onFeedbackSent = { feedbackSent = true }
                )
            }
            
            // Add voice recognition button
            if (adContent != null && (adState == AdState.STREAMING_AUDIO || adState == AdState.PLAYING_VIDEO)) {
                VoiceRecognitionButton(
                    isListening = isListening,
                    onVoiceRecognitionRequested = {
                        isListening = true
                        voiceQueryResult = null
                        
                        // Create intent for voice recognition
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask me anything about this product...")
                        }
                        
                        // Launch voice recognition
                        try {
                            voiceRecognitionLauncher.launch(intent)
                        } catch (e: Exception) {
                            Log.e("StreamingAvatarView", "Error launching voice recognition", e)
                            isListening = false
                        }
                    }
                )
            }
        }
    }
}

/**
 * State indicator overlay that shows current ad state
 */
@Composable
private fun StateIndicatorOverlay(adState: AdState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Card(
            modifier = Modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (adState) {
                    AdState.DETECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    AdState.GENERATING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    AdState.STREAMING_AUDIO -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                    AdState.PLAYING_VIDEO -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                    AdState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // State icon
                Text(
                    text = when (adState) {
                        AdState.DETECTED -> "🔍"
                        AdState.GENERATING -> "⚙️"
                        AdState.STREAMING_AUDIO -> "🔊"
                        AdState.PLAYING_VIDEO -> "🎬"
                        AdState.ERROR -> "⚠️"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // State text
                Text(
                    text = when (adState) {
                        AdState.DETECTED -> "Poster detected"
                        AdState.GENERATING -> "Generating ad..."
                        AdState.STREAMING_AUDIO -> "Talking..."
                        AdState.PLAYING_VIDEO -> "Playing video"
                        AdState.ERROR -> "Error occurred"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
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
                            player.playWhenReady = true // Start playing immediately when video is shown
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
                    exoPlayer?.playWhenReady = true // Ensure video keeps playing
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
                            text = "Live",
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
private fun initializeAudioStreaming(context: Context, audioUrl: String, isPlaying: Boolean, onPlayingStateChange: (Boolean) -> Unit) {
    try {
        val streamingService = AudioStreamingService.getInstance(context)
        
        streamingService.startStreaming(audioUrl, object : AudioStreamingService.PlaybackListener {
            override fun onPlaybackStarted() {
                Log.d("StreamingAvatarView", "Audio playback started")
                onPlayingStateChange(true)
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
                onPlayingStateChange(false)
            }
            
            override fun onPlaybackError(error: Exception) {
                Log.e("StreamingAvatarView", "Audio playback error", error)
                onPlayingStateChange(false)
            }
        })
    } catch (e: Exception) {
        Log.e("StreamingAvatarView", "Error initializing audio streaming", e)
    }
}

/**
 * Feedback buttons for like/dislike
 */
@Composable
private fun FeedbackButtons(
    imageId: String,
    onFeedbackSent: () -> Unit
) {
    val viewModel: EnhancedARViewModel = viewModel()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rate this ad:",
                    style = MaterialTheme.typography.bodySmall
                )
                
                IconButton(
                    onClick = {
                        viewModel.sendFeedback(imageId, "like")
                        onFeedbackSent()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbUp,
                        contentDescription = "Like",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(
                    onClick = {
                        viewModel.sendFeedback(imageId, "dislike")
                        onFeedbackSent()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ThumbDown,
                        contentDescription = "Dislike",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Voice recognition button
 */
@Composable
private fun VoiceRecognitionButton(
    isListening: Boolean,
    onVoiceRecognitionRequested: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Card(
            modifier = Modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            IconButton(
                onClick = onVoiceRecognitionRequested,
                enabled = !isListening
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = if (isListening) "Listening..." else "Voice Query",
                    tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

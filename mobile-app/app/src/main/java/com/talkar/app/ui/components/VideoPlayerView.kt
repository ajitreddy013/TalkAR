package com.talkar.app.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

/**
 * Video Player Component for AR Lip-Sync Playback
 * Supports play/pause control and dynamic video URL loading
 */
@Composable
fun VideoPlayerView(
    videoUrl: String?,
    isPlaying: Boolean,
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    showControls: Boolean = false
) {
    val context = LocalContext.current
    var isPlayerReady by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    
    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            
            // Add listener for player state changes
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isPlayerReady = true
                            isBuffering = false
                        }
                        Player.STATE_BUFFERING -> {
                            isBuffering = true
                        }
                        Player.STATE_ENDED -> {
                            onPlaybackStateChanged(false)
                        }
                        Player.STATE_IDLE -> {
                            isPlayerReady = false
                        }
                    }
                }
                
                override fun onIsPlayingChanged(playing: Boolean) {
                    onPlaybackStateChanged(playing)
                }
            })
        }
    }
    
    // Load video URL when it changes
    LaunchedEffect(videoUrl) {
        if (!videoUrl.isNullOrEmpty()) {
            android.util.Log.d("VideoPlayerView", "Loading video: $videoUrl")
            try {
                val mediaItem = MediaItem.fromUri(videoUrl)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                isPlayerReady = false
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerView", "Error loading video", e)
            }
        }
    }
    
    // Control playback based on isPlaying state
    LaunchedEffect(isPlaying, isPlayerReady) {
        if (isPlayerReady) {
            if (isPlaying) {
                exoPlayer.play()
                android.util.Log.d("VideoPlayerView", "Video playing")
            } else {
                exoPlayer.pause()
                android.util.Log.d("VideoPlayerView", "Video paused")
            }
        }
    }
    
    // Cleanup when composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            android.util.Log.d("VideoPlayerView", "ExoPlayer released")
        }
    }
    
    Box(modifier = modifier) {
        // Android PlayerView embedded in Compose
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = showControls
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Buffering indicator
        if (isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        // Not ready placeholder
        if (!isPlayerReady && !isBuffering && videoUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Compact Video Player for Avatar Overlay
 * Circular video player with minimal controls
 */
@Composable
fun CompactVideoPlayer(
    videoUrl: String?,
    isPlaying: Boolean,
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlayerReady by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE // Loop for avatar
            playWhenReady = false
            volume = 1.0f
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isPlayerReady = playbackState == Player.STATE_READY
                }
                
                override fun onIsPlayingChanged(playing: Boolean) {
                    onPlaybackStateChanged(playing)
                }
            })
        }
    }
    
    LaunchedEffect(videoUrl) {
        if (!videoUrl.isNullOrEmpty()) {
            android.util.Log.d("CompactVideoPlayer", "Loading: $videoUrl")
            try {
                exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
                exoPlayer.prepare()
            } catch (e: Exception) {
                android.util.Log.e("CompactVideoPlayer", "Load error", e)
            }
        }
    }
    
    LaunchedEffect(isPlaying, isPlayerReady) {
        if (isPlayerReady) {
            if (isPlaying) exoPlayer.play() else exoPlayer.pause()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

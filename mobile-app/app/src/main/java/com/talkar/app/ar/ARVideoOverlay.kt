package com.talkar.app.ar

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * AR Video Overlay that displays video on top of detected AR images.
 * 
 * This component:
 * - Tracks AR image position in screen space
 * - Positions video overlay to match the image
 * - Updates position as camera moves
 * - Handles video playback with ExoPlayer
 * 
 * @param augmentedImage The AR image to track
 * @param videoUri URI of the video to play
 * @param onVideoCompleted Callback when video completes
 * @param onVideoError Callback for errors
 */
class ARVideoOverlay(
    private val context: Context,
    private val augmentedImage: AugmentedImage
) {
    companion object {
        private const val TAG = "ARVideoOverlay"
    }
    
    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Callbacks
    var onVideoCompleted: (() -> Unit)? = null
    var onVideoError: ((error: String) -> Unit)? = null
    
    // Screen position of the video overlay
    data class OverlayPosition(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val visible: Boolean
    )
    
    private var _position = mutableStateOf(OverlayPosition(0f, 0f, 0f, 0f, false))
    val position: OverlayPosition get() = _position.value
    
    init {
        Log.d(TAG, "ARVideoOverlay created for image: ${augmentedImage.name}")
    }
    
    /**
     * Updates the overlay position based on AR image tracking.
     * Call this every frame.
     * 
     * @param viewWidth Width of the AR view
     * @param viewHeight Height of the AR view
     */
    fun updatePosition(viewWidth: Int, viewHeight: Int) {
        if (augmentedImage.trackingState != TrackingState.TRACKING) {
            _position.value = _position.value.copy(visible = false)
            return
        }
        
        // For now, center the video on screen with fixed size
        // This is a simplified approach - video will be centered regardless of poster position
        val imageWidth = augmentedImage.extentX
        val imageHeight = augmentedImage.extentZ
        
        // Scale to screen size (make it reasonably large)
        val scaleFactor = 400f // pixels per meter
        val overlayWidth = imageWidth * scaleFactor
        val overlayHeight = imageHeight * scaleFactor
        
        // Center on screen
        val screenX = (viewWidth - overlayWidth) / 2f
        val screenY = (viewHeight - overlayHeight) / 2f
        
        _position.value = OverlayPosition(
            x = screenX,
            y = screenY,
            width = overlayWidth,
            height = overlayHeight,
            visible = true
        )
        
        Log.v(TAG, "Position: x=$screenX, y=$screenY, w=$overlayWidth, h=$overlayHeight, visible=true")
    }
    
    /**
     * Loads and plays a video.
     */
    fun loadVideo(videoUri: Uri, autoPlay: Boolean = true) {
        scope.launch {
            try {
                Log.i(TAG, "========================================")
                Log.i(TAG, "📹 Loading video: $videoUri")
                Log.i(TAG, "========================================")
                
                // Release existing player
                exoPlayer?.release()
                
                // Create ExoPlayer
                val player = ExoPlayer.Builder(context).build()
                exoPlayer = player
                
                // Set up listener
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                val videoSize = player.videoSize
                                val hasVideo = videoSize.width > 0 && videoSize.height > 0
                                Log.i(TAG, "✅ Video ready: ${player.duration}ms")
                                Log.i(TAG, "   Video size: ${videoSize.width}x${videoSize.height}")
                                Log.i(TAG, "   Has video track: $hasVideo")
                                Log.i(TAG, "   Audio track count: ${player.currentTracks.groups.count { it.type == androidx.media3.common.C.TRACK_TYPE_AUDIO }}")
                                Log.i(TAG, "   Video track count: ${player.currentTracks.groups.count { it.type == androidx.media3.common.C.TRACK_TYPE_VIDEO }}")
                                
                                if (!hasVideo) {
                                    Log.w(TAG, "⚠️ WARNING: No video track detected! This is audio-only.")
                                }
                            }
                            Player.STATE_ENDED -> {
                                Log.i(TAG, "⏹️ Video completed")
                                onVideoCompleted?.invoke()
                            }
                        }
                    }
                    
                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        Log.i(TAG, "📊 Tracks changed:")
                        tracks.groups.forEach { group ->
                            val trackType = when (group.type) {
                                androidx.media3.common.C.TRACK_TYPE_VIDEO -> "VIDEO"
                                androidx.media3.common.C.TRACK_TYPE_AUDIO -> "AUDIO"
                                else -> "OTHER(${group.type})"
                            }
                            Log.i(TAG, "   - $trackType: ${group.length} track(s)")
                        }
                    }
                    
                    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                        Log.i(TAG, "📐 Video size changed: ${videoSize.width}x${videoSize.height}")
                    }
                    
                    override fun onRenderedFirstFrame() {
                        Log.i(TAG, "🎬 First frame rendered!")
                    }
                    
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "❌ Player error: ${error.message}")
                        Log.e(TAG, "   Error code: ${error.errorCode}")
                        onVideoError?.invoke(error.message ?: "Unknown error")
                    }
                })
                
                // Set media and prepare
                player.setMediaItem(MediaItem.fromUri(videoUri))
                player.prepare()
                
                // Enable video output
                player.volume = 1.0f // Ensure audio is on
                
                if (autoPlay) {
                    player.playWhenReady = true
                    Log.i(TAG, "▶️ Playback started")
                }
                
                Log.i(TAG, "🎉 Video loaded successfully!")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load video: ${e.message}", e)
                onVideoError?.invoke(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Gets the ExoPlayer instance for rendering.
     */
    fun getPlayer(): ExoPlayer? = exoPlayer
    
    /**
     * Cleans up resources.
     */
    fun cleanup() {
        exoPlayer?.release()
        exoPlayer = null
        scope.cancel()
        Log.d(TAG, "ARVideoOverlay cleaned up")
    }
}

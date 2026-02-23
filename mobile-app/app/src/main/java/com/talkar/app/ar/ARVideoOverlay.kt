package com.talkar.app.ar

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.ar.core.AugmentedImage

/**
 * Manages video overlay for an AR tracked image.
 * 
 * This class handles:
 * - Tracking the position of an augmented image
 * - Loading and playing video content
 * - Calculating screen position for overlay rendering
 * - Lifecycle management and cleanup
 */
class ARVideoOverlay(
    private val context: Context,
    private val augmentedImage: AugmentedImage
) {
    /**
     * Current screen position of the overlay.
     */
    var position: OverlayPosition? = null
        private set
    
    /**
     * Callback invoked when video playback completes.
     */
    var onVideoCompleted: (() -> Unit)? = null
    
    /**
     * Callback invoked when video playback encounters an error.
     */
    var onVideoError: ((String) -> Unit)? = null
    
    /**
     * ExoPlayer instance for video playback.
     */
    private var player: ExoPlayer? = null
    
    /**
     * URI of the currently loaded video.
     */
    private var videoUri: Uri? = null
    
    /**
     * Whether video should auto-play when loaded.
     */
    private var autoPlay: Boolean = false
    
    init {
        // Initialize ExoPlayer
        player = ExoPlayer.Builder(context).build().apply {
            // Set up player listener
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_ENDED -> {
                            Log.d(TAG, "Video playback completed")
                            onVideoCompleted?.invoke()
                        }
                        Player.STATE_IDLE -> {
                            Log.d(TAG, "Player idle")
                        }
                        Player.STATE_BUFFERING -> {
                            Log.d(TAG, "Player buffering")
                        }
                        Player.STATE_READY -> {
                            Log.d(TAG, "Player ready")
                        }
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e(TAG, "Player error: ${error.message}", error)
                    onVideoError?.invoke(error.message ?: "Unknown playback error")
                }
            })
        }
    }
    
    /**
     * Returns the ExoPlayer instance for rendering.
     */
    fun getPlayer(): ExoPlayer? = player
    
    /**
     * Updates the screen position of the overlay based on the augmented image's current pose.
     * 
     * @param screenWidth Width of the screen in pixels
     * @param screenHeight Height of the screen in pixels
     */
    fun updatePosition(screenWidth: Int, screenHeight: Int) {
        try {
            // Get the augmented image's pose
            @Suppress("UNUSED_VARIABLE")
            val pose = augmentedImage.centerPose
            
            // Get the image's physical dimensions
            val extentX = augmentedImage.extentX
            val extentZ = augmentedImage.extentZ
            
            // Calculate screen position (simplified - in production would use projection matrices)
            // For now, we'll use a placeholder calculation
            val centerX = screenWidth / 2f
            val centerY = screenHeight / 2f
            
            // Calculate overlay dimensions based on image size
            // Scale factor to convert meters to pixels (approximate)
            val scaleFactor = 500f
            val overlayWidth = extentX * scaleFactor
            val overlayHeight = extentZ * scaleFactor
            
            position = OverlayPosition(
                x = centerX - overlayWidth / 2,
                y = centerY - overlayHeight / 2,
                width = overlayWidth,
                height = overlayHeight,
                isVisible = augmentedImage.trackingState == com.google.ar.core.TrackingState.TRACKING
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay position", e)
            position = null
        }
    }
    
    /**
     * Loads a video for playback on this overlay.
     * 
     * @param uri URI of the video to load
     * @param autoPlay Whether to automatically start playback
     */
    fun loadVideo(uri: Uri, autoPlay: Boolean = false) {
        Log.d(TAG, "Loading video: $uri (autoPlay: $autoPlay)")
        this.videoUri = uri
        this.autoPlay = autoPlay
        
        player?.let { exoPlayer ->
            // Create media item and set it to the player
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            
            if (autoPlay) {
                Log.d(TAG, "Auto-playing video")
                exoPlayer.play()
            }
        }
    }
    
    /**
     * Cleans up resources used by this overlay.
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up ARVideoOverlay")
        player?.release()
        player = null
        videoUri = null
        position = null
        onVideoCompleted = null
        onVideoError = null
    }
    
    /**
     * Represents the screen position and dimensions of the overlay.
     */
    data class OverlayPosition(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val isVisible: Boolean
    ) {
        /**
         * Alias for isVisible to match the expected property name.
         */
        val visible: Boolean get() = isVisible
    }
    
    companion object {
        private const val TAG = "ARVideoOverlay"
    }
}

package com.talkar.app.ar

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.google.ar.core.Anchor
import io.github.sceneview.ar.node.AnchorNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A node that displays video content anchored to an AR image.
 * 
 * This node creates a 3D plane that matches the physical dimensions of the detected image
 * and plays video content on it, creating the "talking poster" effect.
 * 
 * Features:
 * - Video playback synchronized with AR tracking
 * - Automatic aspect ratio handling
 * - Lifecycle management (play/pause/stop)
 * - Completion callbacks
 * - 3D rendering with Sceneview and Filament
 * 
 * @param context Android context for media player
 * @param anchorNode Sceneview anchor node from detected image
 * @param imageWidth Physical width of the image in meters (e.g., 0.8m)
 * @param imageHeight Physical height of the image in meters
 */
class VideoAnchorNode(
    private val context: Context,
    private val anchorNode: AnchorNode,
    private val imageWidth: Float,
    private val imageHeight: Float
) {
    
    companion object {
        private const val TAG = "VideoAnchorNode"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var surfaceTexture: SurfaceTexture? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Callbacks
    var onVideoCompleted: (() -> Unit)? = null
    var onVideoError: ((error: String) -> Unit)? = null
    
    init {
        Log.d(TAG, "VideoAnchorNode created (${imageWidth}m x ${imageHeight}m)")
    }
    
    /**
     * Loads and plays a video from a URI.
     * 
     * @param videoUri URI of the video to play (can be local file or network URL)
     * @param autoPlay Whether to start playing immediately
     */
    fun loadVideo(videoUri: Uri, autoPlay: Boolean = true) {
        scope.launch(Dispatchers.IO) { // Load on background thread
            try {
                Log.d(TAG, "Loading video: $videoUri")
                
                // Release any existing media player
                releaseMediaPlayer()
                
                // Create new media player on background thread
                val player = MediaPlayer().apply {
                    setDataSource(context, videoUri)
                    
                    // Set up callbacks on main thread
                    setOnPreparedListener { preparedPlayer ->
                        scope.launch(Dispatchers.Main) {
                            Log.d(TAG, "Video prepared, duration: ${preparedPlayer.duration}ms")
                            
                            // Create video plane with correct aspect ratio
                            val videoWidth = preparedPlayer.videoWidth
                            val videoHeight = preparedPlayer.videoHeight
                            val aspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
                            
                            createVideoPlane(aspectRatio)
                            
                            if (autoPlay) {
                                preparedPlayer.start()
                                Log.d(TAG, "Video playback started")
                            }
                        }
                    }
                    
                    setOnCompletionListener {
                        scope.launch(Dispatchers.Main) {
                            Log.d(TAG, "Video playback completed")
                            onVideoCompleted?.invoke()
                        }
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        scope.launch(Dispatchers.Main) {
                            val error = "MediaPlayer error: what=$what, extra=$extra"
                            Log.e(TAG, error)
                            onVideoError?.invoke(error)
                        }
                        true
                    }
                    
                    // Prepare asynchronously
                    prepareAsync()
                }
                
                // Store on main thread
                scope.launch(Dispatchers.Main) {
                    mediaPlayer = player
                }
                
            } catch (e: Exception) {
                scope.launch(Dispatchers.Main) {
                    val error = "Failed to load video: ${e.message}"
                    Log.e(TAG, error, e)
                    onVideoError?.invoke(error)
                }
            }
        }
    }
    
    /**
     * Creates a 3D plane to display the video.
     * The plane matches the physical dimensions of the detected image.
     * 
     * For now, this is a placeholder. Full 3D video rendering will be added next.
     */
    private fun createVideoPlane(videoAspectRatio: Float) {
        try {
            // Calculate plane dimensions to match image size
            val planeWidth = imageWidth
            val planeHeight = imageHeight
            
            Log.d(TAG, "Creating video plane: ${planeWidth}m x ${planeHeight}m")
            Log.d(TAG, "Video aspect ratio: $videoAspectRatio")
            
            // TODO: Create actual 3D plane with video texture
            // For now, just log that the video is ready
            Log.i(TAG, "✅ Video plane ready (3D rendering pending)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create video plane", e)
            onVideoError?.invoke("Failed to create video plane: ${e.message}")
        }
    }
    
    /**
     * Starts or resumes video playback.
     */
    fun play() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
                Log.d(TAG, "Video playback started")
            }
        } ?: run {
            Log.w(TAG, "Cannot play: MediaPlayer not initialized")
        }
    }
    
    /**
     * Pauses video playback.
     */
    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                Log.d(TAG, "Video playback paused")
            }
        }
    }
    
    /**
     * Stops video playback and resets to beginning.
     */
    fun stop() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.reset()
            Log.d(TAG, "Video playback stopped")
        }
    }
    
    /**
     * Checks if video is currently playing.
     */
    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false
    
    /**
     * Gets current playback position in milliseconds.
     */
    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0
    
    /**
     * Gets total video duration in milliseconds.
     */
    fun getDuration(): Int = mediaPlayer?.duration ?: 0
    
    /**
     * Seeks to a specific position in the video.
     * 
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        Log.d(TAG, "Seeked to ${positionMs}ms")
    }
    
    /**
     * Sets the volume for video playback.
     * 
     * @param volume Volume level (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clampedVolume, clampedVolume)
        Log.d(TAG, "Volume set to $clampedVolume")
    }
    
    /**
     * Releases media player resources.
     */
    private fun releaseMediaPlayer() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            Log.d(TAG, "MediaPlayer released")
        }
        mediaPlayer = null
        
        surfaceTexture?.release()
        surfaceTexture = null
    }
    
    /**
     * Cleans up all resources.
     * Call this when the node is no longer needed.
     */
    fun cleanup() {
        releaseMediaPlayer()
        scope.cancel()
        Log.d(TAG, "VideoAnchorNode cleaned up")
    }
    
    /**
     * Gets the anchor node for this video.
     */
    fun getAnchorNode(): AnchorNode = anchorNode
}

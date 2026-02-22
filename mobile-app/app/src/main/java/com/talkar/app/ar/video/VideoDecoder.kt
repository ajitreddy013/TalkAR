package com.talkar.app.ar.video

import android.net.Uri
import android.view.Surface
import com.talkar.app.ar.video.models.VideoInfo

/**
 * Interface for video decoding and playback.
 * 
 * Wraps ExoPlayer with proper Surface management and dimension extraction.
 * Provides hardware-accelerated decoding with software fallback.
 */
interface VideoDecoder {
    /**
     * Initializes decoder with video source and output surface.
     * 
     * @param videoUri Video file URI (res/raw or external)
     * @param surface Target surface for rendering
     * @return Result with VideoInfo on success, or error on failure
     */
    suspend fun initialize(videoUri: Uri, surface: Surface): Result<VideoInfo>
    
    /**
     * Starts video decoding and playback.
     */
    fun start()
    
    /**
     * Pauses playback without releasing resources.
     */
    fun pause()
    
    /**
     * Stops playback and resets to beginning.
     */
    fun stop()
    
    /**
     * Seeks to specific timestamp.
     * 
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long)
    
    /**
     * Gets video information.
     * 
     * @return VideoInfo if initialized, null otherwise
     */
    fun getVideoInfo(): VideoInfo?
    
    /**
     * Gets current playback position.
     * 
     * @return Current position in milliseconds
     */
    fun getCurrentPosition(): Long
    
    /**
     * Checks if video is currently playing.
     * 
     * @return True if playing, false otherwise
     */
    fun isPlaying(): Boolean
    
    /**
     * Sets volume (0.0 to 1.0).
     * 
     * @param volume Volume level, clamped to 0.0-1.0 range
     */
    fun setVolume(volume: Float)
    
    /**
     * Enables/disables looping.
     * 
     * @param enabled True to enable looping, false to disable
     */
    fun setLooping(enabled: Boolean)
    
    /**
     * Releases decoder resources.
     * Must be called when decoder is no longer needed.
     */
    fun release()
    
    /**
     * Registers listener for decoder events.
     * 
     * @param listener Listener to receive decoder events
     */
    fun setListener(listener: VideoDecoderListener)
}

/**
 * Listener for video decoder events.
 */
interface VideoDecoderListener {
    /**
     * Called when decoder is ready and video info is available.
     * 
     * @param info Video information extracted from the file
     */
    fun onReady(info: VideoInfo)
    
    /**
     * Called when the first frame is rendered to the surface.
     */
    fun onFirstFrameRendered()
    
    /**
     * Called when playback state changes.
     * 
     * @param state ExoPlayer playback state (STATE_IDLE, STATE_BUFFERING, STATE_READY, STATE_ENDED)
     */
    fun onPlaybackStateChanged(state: Int)
    
    /**
     * Called when video size changes.
     * 
     * @param width New video width in pixels
     * @param height New video height in pixels
     */
    fun onVideoSizeChanged(width: Int, height: Int)
    
    /**
     * Called when a playback error occurs.
     * 
     * @param error The playback exception
     */
    fun onError(error: androidx.media3.common.PlaybackException)
}

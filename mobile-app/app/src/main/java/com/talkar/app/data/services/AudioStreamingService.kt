package com.talkar.app.data.services

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Service for streaming audio with partial playback capability
 * Starts playing voice as soon as 2-3 seconds of audio buffer is ready
 */
class AudioStreamingService private constructor(private val context: Context) {
    
    private val tag = "AudioStreamingService"
    private var player: ExoPlayer? = null
    private var playbackListener: PlaybackListener? = null
    private var bufferingJob: Job? = null
    
    /**
     * Interface for playback events
     */
    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onBufferingStarted()
        fun onBufferingCompleted()
        fun onPlaybackCompleted()
        fun onPlaybackError(error: Exception)
    }
    
    /**
     * Initialize the ExoPlayer instance
     */
    @androidx.media3.common.util.UnstableApi
    private fun initializePlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context)
                .setHandleAudioBecomingNoisy(true)
                .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
                .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
                .build()
                .apply {
                    playWhenReady = false
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    Log.d(tag, "Player buffering")
                                    playbackListener?.onBufferingStarted()
                                }
                                Player.STATE_READY -> {
                                    Log.d(tag, "Player ready")
                                    playbackListener?.onBufferingCompleted()
                                    // Check if we have enough buffer to start playback
                                    checkBufferAndStartPlayback()
                                }
                                Player.STATE_ENDED -> {
                                    Log.d(tag, "Player ended")
                                    playbackListener?.onPlaybackCompleted()
                                }
                            }
                        }
                        
                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            Log.e(tag, "Player error", error)
                            playbackListener?.onPlaybackError(Exception(error))
                        }
                    })
                }
        }
    }
    
    /**
     * Check if we have enough buffer (2-3 seconds) to start playback
     */
    private fun checkBufferAndStartPlayback() {
        player?.let { exoPlayer ->
            val bufferedPosition = exoPlayer.bufferedPosition
            val currentPosition = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            
            // If we have at least 2 seconds of buffer or we've buffered most of the content
            val hasSufficientBuffer = bufferedPosition - currentPosition >= MIN_BUFFER_MS
            val isMostlyBuffered = duration > 0 && bufferedPosition >= duration * 0.8
            
            // Start playback if we have sufficient buffer or if we've reached max buffer time
            if (!exoPlayer.playWhenReady && (hasSufficientBuffer || isMostlyBuffered || 
                    bufferedPosition >= MAX_BUFFER_MS)) {
                Log.d(tag, "Starting playback with ${bufferedPosition - currentPosition}ms buffer")
                exoPlayer.playWhenReady = true
                playbackListener?.onPlaybackStarted()
            }
        }
    }
    
    /**
     * Start streaming audio from URL
     */
    @androidx.media3.common.util.UnstableApi
    fun startStreaming(audioUrl: String, listener: PlaybackListener) {
        try {
            Log.d(tag, "Starting streaming for URL: $audioUrl")
            
            playbackListener = listener
            initializePlayer()
            
            // Create media source for MP3 streaming
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(Uri.parse(audioUrl)))
            
            player?.setMediaSource(mediaSource)
            player?.prepare()
            
            // Start monitoring buffer in background
            bufferingJob = CoroutineScope(Dispatchers.Main).launch {
                while (player?.isPlaying == false && player?.playbackState == Player.STATE_READY) {
                    checkBufferAndStartPlayback()
                    delay(100) // Check every 100ms
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error starting streaming", e)
            listener.onPlaybackError(e)
        }
    }
    
    /**
     * Stop streaming and release resources
     */
    @androidx.media3.common.util.UnstableApi
    fun stopStreaming() {
        Log.d(tag, "Stopping streaming")
        bufferingJob?.cancel()
        player?.stop()
        player?.release()
        player = null
    }
    
    /**
     * Pause streaming
     */
    @androidx.media3.common.util.UnstableApi
    fun pauseStreaming() {
        player?.playWhenReady = false
    }
    
    /**
     * Resume streaming
     */
    @androidx.media3.common.util.UnstableApi
    fun resumeStreaming() {
        player?.playWhenReady = true
    }
    
    /**
     * Check if audio is currently playing
     */
    @androidx.media3.common.util.UnstableApi
    fun isPlaying(): Boolean {
        return player?.isPlaying ?: false
    }
    
    /**
     * Get current playback position in milliseconds
     */
    @androidx.media3.common.util.UnstableApi
    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0
    }
    
    /**
     * Get buffered position in milliseconds
     */
    @androidx.media3.common.util.UnstableApi
    fun getBufferedPosition(): Long {
        return player?.bufferedPosition ?: 0
    }
    
    companion object {
        private const val MIN_BUFFER_MS: Long = 2000 // 2 seconds minimum buffer
        private const val MAX_BUFFER_MS: Long = 3000 // 3 seconds maximum before forced start
        private const val SEEK_INCREMENT_MS: Long = 5000 // 5 seconds seek increment
        
        @Volatile
        private var INSTANCE: AudioStreamingService? = null
        
        fun getInstance(context: Context): AudioStreamingService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioStreamingService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
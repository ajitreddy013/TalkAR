package com.talkar.app.data.services

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.min

/**
 * Ambient Audio Service for playing background environmental sounds
 * with fade-in/fade-out functionality that responds to avatar voice activity
 */
class AmbientAudioService(private val context: Context) {
    
    private val tag = "AmbientAudioService"
    private var mediaPlayer: MediaPlayer? = null
    private var coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Audio state
    private var isPlaying = false
    private var targetVolume = 0.0f
    private var currentVolume = 0.0f
    private var isFading = false
    
    // Performance optimization: Use a single fade job instead of multiple
    private var fadeJob: Job? = null
    
    // Configuration
    private companion object {
        const val FADE_DURATION_MS = 2000L // 2 seconds fade
        const val VOLUME_UPDATE_INTERVAL_MS = 50L // Update volume every 50ms
        const val MAX_VOLUME = 0.3f // Cap ambient volume to 30% to avoid overpowering
        const val VOLUME_EPSILON = 0.01f // Threshold for volume equality
    }
    
    /**
     * Initialize ambient audio with a specific audio resource
     */
    fun initialize(audioResourceId: Int): Boolean {
        return try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context.resources.openRawResourceFd(audioResourceId))
                prepareAsync()
                
                setOnPreparedListener {
                    Log.d(tag, "Ambient audio prepared")
                    // Set initial volume to 0 for fade-in
                    setVolume(0.0f, 0.0f)
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(tag, "Ambient audio error: what=$what, extra=$extra")
                    false
                }
                
                setOnCompletionListener {
                    // Loop the ambient audio
                    if (isPlaying) {
                        start()
                    }
                }
            }
            
            Log.d(tag, "Ambient audio service initialized")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize ambient audio", e)
            false
        }
    }
    
    /**
     * Start playing ambient audio with fade-in
     */
    fun startAmbientAudio() {
        if (mediaPlayer == null) {
            Log.w(tag, "Cannot start ambient audio - not initialized")
            return
        }
        
        try {
            if (!isPlaying) {
                mediaPlayer?.start()
                isPlaying = true
                Log.d(tag, "Ambient audio started")
            }
            
            // Fade in to target volume
            fadeIn()
        } catch (e: Exception) {
            Log.e(tag, "Failed to start ambient audio", e)
        }
    }
    
    /**
     * Fade in ambient audio
     */
    private fun fadeIn() {
        targetVolume = MAX_VOLUME
        startVolumeFade()
    }
    
    /**
     * Fade out ambient audio (when avatar is speaking)
     */
    fun fadeOut() {
        targetVolume = 0.0f
        startVolumeFade()
    }
    
    /**
     * Start the volume fade animation
     * Performance optimized to cancel previous fade jobs
     */
    private fun startVolumeFade() {
        if (mediaPlayer == null) return
        
        // Performance optimization: Cancel previous fade job
        fadeJob?.cancel()
        
        // If already at target volume, no need to fade
        if (kotlin.math.abs(currentVolume - targetVolume) < VOLUME_EPSILON) {
            return
        }
        
        isFading = true
        val startVolume = currentVolume
        val volumeDifference = targetVolume - startVolume
        val steps = (FADE_DURATION_MS / VOLUME_UPDATE_INTERVAL_MS).toInt()
        val volumeStep = volumeDifference / steps
        
        fadeJob = coroutineScope.launch {
            try {
                repeat(steps) { step ->
                    // Check if job is still active
                    if (!isActive) return@launch
                    
                    currentVolume = startVolume + (volumeStep * (step + 1))
                    currentVolume = max(0.0f, min(MAX_VOLUME, currentVolume))
                    
                    try {
                        mediaPlayer?.setVolume(currentVolume, currentVolume)
                    } catch (e: Exception) {
                        Log.e(tag, "Error setting volume", e)
                    }
                    
                    delay(VOLUME_UPDATE_INTERVAL_MS)
                }
                
                // Ensure we reach the exact target volume
                currentVolume = targetVolume
                try {
                    mediaPlayer?.setVolume(currentVolume, currentVolume)
                } catch (e: Exception) {
                    Log.e(tag, "Error setting final volume", e)
                }
                
                isFading = false
                Log.d(tag, "Volume fade completed. Current volume: $currentVolume")
            } catch (e: CancellationException) {
                // Job was cancelled, this is expected
                Log.d(tag, "Volume fade cancelled")
            } catch (e: Exception) {
                Log.e(tag, "Error during volume fade", e)
                isFading = false
            }
        }
    }
    
    /**
     * Pause ambient audio
     */
    fun pause() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
            Log.d(tag, "Ambient audio paused")
        } catch (e: Exception) {
            Log.e(tag, "Failed to pause ambient audio", e)
        }
    }
    
    /**
     * Resume ambient audio
     */
    fun resume() {
        try {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
                isPlaying = true
                Log.d(tag, "Ambient audio resumed")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume ambient audio", e)
        }
    }
    
    /**
     * Stop and cleanup ambient audio
     */
    fun stop() {
        try {
            // Performance optimization: Cancel fade job instead of entire scope
            fadeJob?.cancel()
            
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isPlaying = false
            isFading = false
            currentVolume = 0.0f
            targetVolume = 0.0f
            Log.d(tag, "Ambient audio stopped and cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping ambient audio", e)
        }
    }
    
    /**
     * Check if ambient audio is currently playing
     */
    fun isPlaying(): Boolean {
        return isPlaying && mediaPlayer?.isPlaying == true
    }
    
    /**
     * Get current volume level
     */
    fun getCurrentVolume(): Float {
        return currentVolume
    }
    
    /**
     * Set volume directly (bypassing fade)
     */
    fun setVolume(volume: Float) {
        if (mediaPlayer == null) return
        
        val clampedVolume = max(0.0f, min(MAX_VOLUME, volume))
        currentVolume = clampedVolume
        targetVolume = clampedVolume
        
        // Performance optimization: Cancel fade job when setting volume directly
        fadeJob?.cancel()
        isFading = false
        
        try {
            mediaPlayer?.setVolume(clampedVolume, clampedVolume)
            Log.d(tag, "Volume set directly to: $clampedVolume")
        } catch (e: Exception) {
            Log.e(tag, "Error setting volume directly", e)
        }
    }
}
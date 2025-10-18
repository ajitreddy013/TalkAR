package com.talkar.app.ar

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.talkar.app.data.models.VisemeData
import com.talkar.app.data.models.VisemeDataBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * AudioSyncController - Synchronize Audio Playback with Mouth Animation
 * 
 * Ensures that audio playback and mouth animation start simultaneously
 * and remain synchronized throughout the duration.
 */
class AudioSyncController(context: Context) {
    
    private val TAG = "AudioSyncController"
    private val contextRef = WeakReference(context)
    
    // Media player for audio
    private var mediaPlayer: MediaPlayer? = null
    
    // Mouth animator
    private val mouthAnimator = MouthAnimator()
    private val simpleMouthAnimator = SimpleMouthAnimator()
    
    // Sync state
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    // Current viseme data
    private var currentVisemeData: VisemeData? = null
    
    /**
     * Prepare audio and animation for playback
     */
    fun prepare(audioUrl: String, visemeData: VisemeData? = null, text: String? = null) {
        cleanup()
        
        val context = contextRef.get() ?: return
        
        try {
            _syncState.value = SyncState.Preparing
            
            // Create media player
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, Uri.parse(audioUrl))
                prepareAsync()
                
                setOnPreparedListener {
                    val duration = (it.duration / 1000f)
                    Log.d(TAG, "Audio prepared: duration = ${duration}s")
                    
                    // Generate or use provided viseme data
                    val visemes = visemeData ?: VisemeDataBuilder.generateMockVisemes(
                        duration = duration,
                        text = text,
                        audioUrl = audioUrl
                    )
                    
                    currentVisemeData = visemes
                    _syncState.value = SyncState.Ready(duration, visemes)
                }
                
                setOnCompletionListener {
                    Log.d(TAG, "Audio playback completed")
                    stop()
                }
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _syncState.value = SyncState.Error("Media player error: $what")
                    true
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare audio", e)
            _syncState.value = SyncState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Start synchronized playback
     * Audio and animation start simultaneously
     */
    fun start() {
        val player = mediaPlayer ?: run {
            Log.e(TAG, "Cannot start: MediaPlayer not prepared")
            return
        }
        
        val visemes = currentVisemeData ?: run {
            Log.e(TAG, "Cannot start: No viseme data")
            return
        }
        
        try {
            _syncState.value = SyncState.Playing
            
            // Start audio and animation simultaneously
            player.start()
            
            if (visemes.source == com.talkar.app.data.models.VisemeSource.MOCK && 
                visemes.visemes.size <= 2) {
                // Use simple mouth animation for basic mock data
                simpleMouthAnimator.startAnimation(visemes.totalDuration)
            } else {
                // Use full viseme-based animation
                mouthAnimator.startAnimation(visemes)
            }
            
            Log.d(TAG, "✅ Started synchronized playback (audio + animation)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback", e)
            _syncState.value = SyncState.Error(e.message ?: "Playback failed")
        }
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        mediaPlayer?.pause()
        mouthAnimator.stopAnimation()
        simpleMouthAnimator.stopAnimation()
        _syncState.value = SyncState.Paused
        Log.d(TAG, "Paused playback")
    }
    
    /**
     * Resume playback
     */
    fun resume() {
        val player = mediaPlayer ?: return
        val visemes = currentVisemeData ?: return
        
        player.start()
        
        // TODO: Resume animation from current position
        // For now, restart animation
        if (visemes.source == com.talkar.app.data.models.VisemeSource.MOCK) {
            simpleMouthAnimator.startAnimation(visemes.totalDuration)
        } else {
            mouthAnimator.startAnimation(visemes)
        }
        
        _syncState.value = SyncState.Playing
        Log.d(TAG, "Resumed playback")
    }
    
    /**
     * Stop playback
     */
    fun stop() {
        mediaPlayer?.stop()
        mouthAnimator.stopAnimation()
        simpleMouthAnimator.stopAnimation()
        _syncState.value = SyncState.Stopped
        Log.d(TAG, "Stopped playback")
    }
    
    /**
     * Get current mouth blend shapes
     */
    fun getCurrentBlendShapes(): StateFlow<MouthBlendShapes> {
        return mouthAnimator.blendShapes
    }
    
    /**
     * Get simple jaw open amount (for simple animation)
     */
    fun getJawOpenAmount(): StateFlow<Float> {
        return simpleMouthAnimator.jawOpenAmount
    }
    
    /**
     * Check if currently playing
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }
    
    /**
     * Get current playback position
     */
    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }
    
    /**
     * Get duration
     */
    fun getDuration(): Int {
        return mediaPlayer?.duration ?: 0
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        mediaPlayer?.release()
        mediaPlayer = null
        mouthAnimator.stopAnimation()
        simpleMouthAnimator.stopAnimation()
        _syncState.value = SyncState.Idle
        currentVisemeData = null
        Log.d(TAG, "Cleaned up resources")
    }
}

/**
 * Audio-Animation Sync States
 */
sealed class SyncState {
    object Idle : SyncState()
    object Preparing : SyncState()
    data class Ready(val duration: Float, val visemeData: VisemeData) : SyncState()
    object Playing : SyncState()
    object Paused : SyncState()
    object Stopped : SyncState()
    data class Error(val message: String) : SyncState()
}

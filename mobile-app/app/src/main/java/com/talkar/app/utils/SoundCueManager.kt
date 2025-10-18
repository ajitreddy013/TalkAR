package com.talkar.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes

/**
 * Sound Cue Manager
 * 
 * Manages sound effects for AR interactions:
 * - Avatar appearance
 * - Scan detection
 * - UI interactions
 */
class SoundCueManager(private val context: Context) {
    
    private val TAG = "SoundCueManager"
    
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundType, Int>()
    private var isInitialized = false
    
    init {
        initialize()
    }
    
    /**
     * Initialize SoundPool
     */
    private fun initialize() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()
            
            // Load sound effects (if available in res/raw)
            // For now, we'll use system sounds as fallback
            
            isInitialized = true
            Log.d(TAG, "SoundCueManager initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SoundPool", e)
        }
    }
    
    /**
     * Play sound cue
     */
    fun playSoundCue(soundType: SoundType, volume: Float = 1.0f) {
        if (!isInitialized) {
            Log.w(TAG, "SoundCueManager not initialized, cannot play sound")
            return
        }
        
        try {
            when (soundType) {
                SoundType.AVATAR_APPEAR -> playAvatarAppearSound(volume)
                SoundType.SCAN_DETECTED -> playScanDetectedSound(volume)
                SoundType.BUTTON_CLICK -> playButtonClickSound(volume)
                SoundType.SUCCESS -> playSuccessSound(volume)
                SoundType.ERROR -> playErrorSound(volume)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound cue: ${soundType.name}", e)
        }
    }
    
    /**
     * Play avatar appearance sound
     * Soft, magical "whoosh" or chime sound
     */
    private fun playAvatarAppearSound(volume: Float) {
        // Use system notification sound as fallback
        try {
            val notification = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            val ringtone = android.media.RingtoneManager.getRingtone(context, notification)
            ringtone?.play()
            
            Log.d(TAG, "Avatar appear sound played")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play avatar appear sound", e)
        }
    }
    
    /**
     * Play scan detected sound
     * Short, affirmative beep
     */
    private fun playScanDetectedSound(volume: Float) {
        // Generate a simple beep tone
        generateBeep(440.0, 100, volume)
    }
    
    /**
     * Play button click sound
     * Subtle click
     */
    private fun playButtonClickSound(volume: Float) {
        // Use system click sound
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, volume)
    }
    
    /**
     * Play success sound
     * Positive, uplifting tone
     */
    private fun playSuccessSound(volume: Float) {
        generateBeep(523.25, 150, volume) // C note
    }
    
    /**
     * Play error sound
     * Gentle error tone
     */
    private fun playErrorSound(volume: Float) {
        generateBeep(200.0, 100, volume) // Low tone
    }
    
    /**
     * Generate simple beep tone
     */
    private fun generateBeep(frequency: Double, durationMs: Int, volume: Float) {
        try {
            val toneGen = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION,
                (volume * 100).toInt()
            )
            toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, durationMs)
            
            // Release after playing
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, durationMs.toLong() + 50)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate beep", e)
        }
    }
    
    /**
     * Load custom sound from raw resources
     */
    fun loadSound(soundType: SoundType, @RawRes resourceId: Int) {
        soundPool?.let { pool ->
            val soundId = pool.load(context, resourceId, 1)
            soundIds[soundType] = soundId
            Log.d(TAG, "Loaded custom sound for $soundType")
        }
    }
    
    /**
     * Play loaded sound
     */
    private fun playLoadedSound(soundType: SoundType, volume: Float) {
        soundPool?.let { pool ->
            soundIds[soundType]?.let { soundId ->
                pool.play(soundId, volume, volume, 1, 0, 1.0f)
            }
        }
    }
    
    /**
     * Release resources
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        isInitialized = false
        Log.d(TAG, "SoundCueManager released")
    }
    
    /**
     * Sound Types
     */
    enum class SoundType {
        AVATAR_APPEAR,
        SCAN_DETECTED,
        BUTTON_CLICK,
        SUCCESS,
        ERROR
    }
}

/**
 * Extension function for easy access
 */
fun Context.getSoundCueManager(): SoundCueManager {
    return SoundCueManager(this)
}

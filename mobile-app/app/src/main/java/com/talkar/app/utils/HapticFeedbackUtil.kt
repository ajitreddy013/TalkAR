package com.talkar.app.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Haptic Feedback Utility for TalkAR
 * Provides tactile feedback for AR interactions
 */
object HapticFeedbackUtil {
    
    private const val TAG = "HapticFeedback"
    
    /**
     * Trigger haptic feedback when image is detected by ARCore
     */
    fun onImageDetected(context: Context) {
        Log.d(TAG, "Image detected - triggering haptic feedback")
        vibrateShort(context, amplitude = 128) // Medium intensity
    }
    
    /**
     * Trigger haptic feedback when avatar is tapped
     */
    fun onAvatarTapped(context: Context) {
        Log.d(TAG, "Avatar tapped - triggering haptic feedback")
        vibrateShort(context, amplitude = 64) // Light tap
    }
    
    /**
     * Trigger haptic feedback when lip-sync video starts playing
     */
    fun onVideoPlaybackStart(context: Context) {
        Log.d(TAG, "Video playback started - triggering haptic feedback")
        vibratePattern(context, longArrayOf(0, 50, 50, 100), intArrayOf(0, 80, 0, 120))
    }
    
    /**
     * Trigger haptic feedback when image is lost
     */
    fun onImageLost(context: Context) {
        Log.d(TAG, "Image lost - triggering haptic feedback")
        vibrateShort(context, amplitude = 40, duration = 30) // Very light, brief
    }
    
    /**
     * Trigger error haptic feedback
     */
    fun onError(context: Context) {
        Log.d(TAG, "Error occurred - triggering haptic feedback")
        vibratePattern(
            context,
            longArrayOf(0, 100, 100, 100),
            intArrayOf(0, 200, 0, 255)
        )
    }
    
    /**
     * Short vibration with configurable amplitude
     */
    private fun vibrateShort(
        context: Context,
        duration: Long = 50,
        amplitude: Int = 100
    ) {
        val vibrator = getVibrator(context) ?: run {
            Log.w(TAG, "Vibrator not available")
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(
                duration,
                amplitude.coerceIn(1, 255)
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
    
    /**
     * Pattern vibration with configurable timing and amplitude
     */
    private fun vibratePattern(
        context: Context,
        timings: LongArray,
        amplitudes: IntArray
    ) {
        val vibrator = getVibrator(context) ?: run {
            Log.w(TAG, "Vibrator not available")
            return
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                timings,
                amplitudes,
                -1 // No repeat
            )
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }
    
    /**
     * Get vibrator service (compatible with Android 12+)
     */
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    /**
     * Check if device has vibrator capability
     */
    fun hasVibrator(context: Context): Boolean {
        val vibrator = getVibrator(context)
        return vibrator?.hasVibrator() == true
    }
}

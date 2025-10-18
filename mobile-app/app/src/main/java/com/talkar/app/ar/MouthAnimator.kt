package com.talkar.app.ar

import android.util.Log
import com.talkar.app.data.models.Phoneme
import com.talkar.app.data.models.Viseme
import com.talkar.app.data.models.VisemeData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * MouthAnimator - Blend Shape Mapping for Lip-Sync
 * 
 * Maps phonemes to avatar mouth blend shapes and animates
 * the avatar's mouth to match the audio phonemes.
 */
class MouthAnimator {
    
    private val TAG = "MouthAnimator"
    
    // Current mouth blend shapes (0.0 - 1.0 for each phoneme)
    private val _blendShapes = MutableStateFlow(MouthBlendShapes())
    val blendShapes: StateFlow<MouthBlendShapes> = _blendShapes.asStateFlow()
    
    // Animation state
    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()
    
    // Current viseme data
    private var currentVisemeData: VisemeData? = null
    
    // Animation job
    private var animationJob: Job? = null
    
    // Animation start time
    private var animationStartTime = 0L
    
    /**
     * Start mouth animation with viseme data
     */
    fun startAnimation(visemeData: VisemeData) {
        stopAnimation()
        
        currentVisemeData = visemeData
        animationStartTime = System.currentTimeMillis()
        _isAnimating.value = true
        
        animationJob = CoroutineScope(Dispatchers.Main).launch {
            animateVisemes(visemeData)
        }
        
        Log.d(TAG, "Started mouth animation with ${visemeData.visemes.size} visemes")
    }
    
    /**
     * Stop mouth animation
     */
    fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        _isAnimating.value = false
        resetBlendShapes()
        Log.d(TAG, "Stopped mouth animation")
    }
    
    /**
     * Animate through visemes
     */
    private suspend fun animateVisemes(visemeData: VisemeData) {
        val visemes = visemeData.visemes
        var currentVisemeIndex = 0
        
        while (currentVisemeIndex < visemes.size && isActive) {
            val currentTime = (System.currentTimeMillis() - animationStartTime) / 1000f
            
            // Find current viseme based on time
            val currentViseme = visemes.getOrNull(currentVisemeIndex)
            
            if (currentViseme != null) {
                if (currentTime >= currentViseme.startTime && currentTime <= currentViseme.endTime) {
                    // We're in this viseme's time range
                    val progress = (currentTime - currentViseme.startTime) / currentViseme.duration
                    
                    // Get next viseme for smooth transition
                    val nextViseme = visemes.getOrNull(currentVisemeIndex + 1)
                    
                    // Interpolate blend shapes
                    updateBlendShapes(currentViseme, nextViseme, progress)
                    
                } else if (currentTime > currentViseme.endTime) {
                    // Move to next viseme
                    currentVisemeIndex++
                }
            } else {
                // No more visemes
                break
            }
            
            // Update at ~60 FPS
            delay(16)
        }
        
        // Animation complete
        _isAnimating.value = false
        resetBlendShapes()
    }
    
    /**
     * Update blend shapes based on current and next viseme
     */
    private fun updateBlendShapes(
        current: Viseme,
        next: Viseme?,
        progress: Float
    ) {
        val blendShapes = MutableBlendShapes()
        
        // Set current phoneme blend shape
        val currentWeight = if (progress < 0.8f) {
            // Full weight for most of the duration
            current.weight
        } else {
            // Fade out in last 20%
            current.weight * (1.0f - ((progress - 0.8f) / 0.2f))
        }
        
        blendShapes.setBlendShape(current.phoneme, currentWeight)
        
        // Blend to next phoneme if available
        if (next != null && progress > 0.7f) {
            // Start blending to next in last 30%
            val blendProgress = (progress - 0.7f) / 0.3f
            val nextWeight = next.weight * blendProgress
            
            blendShapes.setBlendShape(next.phoneme, nextWeight)
        }
        
        _blendShapes.value = blendShapes.toImmutable()
    }
    
    /**
     * Reset all blend shapes to neutral
     */
    private fun resetBlendShapes() {
        _blendShapes.value = MouthBlendShapes()
    }
    
    /**
     * Get current animation progress (0.0 - 1.0)
     */
    fun getAnimationProgress(): Float {
        val visemeData = currentVisemeData ?: return 0f
        if (!_isAnimating.value) return 0f
        
        val currentTime = (System.currentTimeMillis() - animationStartTime) / 1000f
        return (currentTime / visemeData.totalDuration).coerceIn(0f, 1f)
    }
}

/**
 * Mouth Blend Shapes - Immutable state of all mouth shapes
 */
data class MouthBlendShapes(
    val neutral: Float = 1.0f,
    val a: Float = 0.0f,
    val e: Float = 0.0f,
    val i: Float = 0.0f,
    val o: Float = 0.0f,
    val u: Float = 0.0f,
    val m: Float = 0.0f,
    val f: Float = 0.0f,
    val th: Float = 0.0f,
    val s: Float = 0.0f,
    val l: Float = 0.0f,
    val r: Float = 0.0f
) {
    /**
     * Get blend shape value for a phoneme
     */
    fun getBlendShape(phoneme: Phoneme): Float {
        return when (phoneme) {
            Phoneme.NEUTRAL, Phoneme.SILENCE -> neutral
            Phoneme.A -> a
            Phoneme.E -> e
            Phoneme.I -> i
            Phoneme.O -> o
            Phoneme.U -> u
            Phoneme.M -> m
            Phoneme.F -> f
            Phoneme.TH -> th
            Phoneme.S -> s
            Phoneme.L -> l
            Phoneme.R -> r
        }
    }
    
    /**
     * Get all non-zero blend shapes
     */
    fun getActiveBlendShapes(): Map<Phoneme, Float> {
        val active = mutableMapOf<Phoneme, Float>()
        
        if (neutral > 0.01f) active[Phoneme.NEUTRAL] = neutral
        if (a > 0.01f) active[Phoneme.A] = a
        if (e > 0.01f) active[Phoneme.E] = e
        if (i > 0.01f) active[Phoneme.I] = i
        if (o > 0.01f) active[Phoneme.O] = o
        if (u > 0.01f) active[Phoneme.U] = u
        if (m > 0.01f) active[Phoneme.M] = m
        if (f > 0.01f) active[Phoneme.F] = f
        if (th > 0.01f) active[Phoneme.TH] = th
        if (s > 0.01f) active[Phoneme.S] = s
        if (l > 0.01f) active[Phoneme.L] = l
        if (r > 0.01f) active[Phoneme.R] = r
        
        return active
    }
}

/**
 * Mutable blend shapes builder
 */
private class MutableBlendShapes {
    private val shapes = mutableMapOf<Phoneme, Float>()
    
    fun setBlendShape(phoneme: Phoneme, weight: Float) {
        shapes[phoneme] = weight.coerceIn(0f, 1f)
    }
    
    fun toImmutable(): MouthBlendShapes {
        return MouthBlendShapes(
            neutral = shapes[Phoneme.NEUTRAL] ?: 1.0f,
            a = shapes[Phoneme.A] ?: 0.0f,
            e = shapes[Phoneme.E] ?: 0.0f,
            i = shapes[Phoneme.I] ?: 0.0f,
            o = shapes[Phoneme.O] ?: 0.0f,
            u = shapes[Phoneme.U] ?: 0.0f,
            m = shapes[Phoneme.M] ?: 0.0f,
            f = shapes[Phoneme.F] ?: 0.0f,
            th = shapes[Phoneme.TH] ?: 0.0f,
            s = shapes[Phoneme.S] ?: 0.0f,
            l = shapes[Phoneme.L] ?: 0.0f,
            r = shapes[Phoneme.R] ?: 0.0f
        )
    }
}

/**
 * Simple Mouth Animator - For basic open/close animation
 * 
 * Use when full viseme data is not available.
 * Creates simple jaw open/close animation synced to audio duration.
 */
class SimpleMouthAnimator {
    
    private val TAG = "SimpleMouthAnimator"
    
    private val _jawOpenAmount = MutableStateFlow(0f)
    val jawOpenAmount: StateFlow<Float> = _jawOpenAmount.asStateFlow()
    
    private var animationJob: Job? = null
    
    /**
     * Start simple open/close animation
     */
    fun startAnimation(duration: Float, syllablesPerSecond: Float = 4.0f) {
        stopAnimation()
        
        animationJob = CoroutineScope(Dispatchers.Main).launch {
            animateJaw(duration, syllablesPerSecond)
        }
        
        Log.d(TAG, "Started simple mouth animation ($duration sec, ${syllablesPerSecond}sps)")
    }
    
    /**
     * Stop animation
     */
    fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        _jawOpenAmount.value = 0f
    }
    
    /**
     * Animate jaw open/close
     */
    private suspend fun animateJaw(duration: Float, syllablesPerSecond: Float) {
        val startTime = System.currentTimeMillis()
        val durationMs = (duration * 1000).toLong()
        
        while (isActive) {
            val elapsed = System.currentTimeMillis() - startTime
            
            if (elapsed > durationMs) {
                break
            }
            
            val currentTime = elapsed / 1000f
            
            // Calculate jaw open amount using sine wave
            // Frequency = syllablesPerSecond
            val frequency = syllablesPerSecond * 2 * Math.PI.toFloat()
            val jawOpen = (sin(currentTime * frequency) + 1f) / 2f // 0.0 to 1.0
            
            // Apply smoothing
            val smoothedJawOpen = jawOpen * 0.7f // Max 70% open
            
            _jawOpenAmount.value = smoothedJawOpen
            
            delay(16) // ~60 FPS
        }
        
        _jawOpenAmount.value = 0f
    }
}

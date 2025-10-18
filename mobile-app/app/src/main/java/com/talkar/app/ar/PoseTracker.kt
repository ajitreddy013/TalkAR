package com.talkar.app.ar

import android.util.Log
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * PoseTracker - Stable AR Pose Tracking with Smoothing
 * 
 * Provides smooth, stable pose tracking for AR avatars by:
 * - Smoothing position changes with exponential moving average
 * - Smoothing rotation with quaternion slerp
 * - Tracking confidence levels
 * - Predicting pose during brief tracking losses
 */
class PoseTracker(
    private val smoothingFactor: Float = 0.3f,  // Lower = smoother but more lag
    private val confidenceThreshold: Float = 0.5f
) {
    
    private val TAG = "PoseTracker"
    
    // Current smoothed pose
    private var smoothedPose: Pose? = null
    
    // Previous raw pose for interpolation
    private var previousRawPose: Pose? = null
    
    // Tracking confidence (0.0 - 1.0)
    private val _trackingConfidence = MutableStateFlow(0f)
    val trackingConfidence: StateFlow<Float> = _trackingConfidence.asStateFlow()
    
    // Tracking state
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    // Stability metrics
    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable.asStateFlow()
    
    // Position velocity (for prediction)
    private var positionVelocity = FloatArray(3) { 0f }
    
    // Rotation velocity (for prediction)
    private var rotationVelocity = FloatArray(4) { 0f }
    
    // Time of last update
    private var lastUpdateTime = 0L
    
    /**
     * Update pose with new tracking data
     * Returns smoothed pose
     */
    fun updatePose(
        rawPose: Pose,
        trackingState: TrackingState,
        trackingConfidence: Float = 1.0f
    ): Pose {
        val currentTime = System.currentTimeMillis()
        val deltaTime = if (lastUpdateTime > 0) {
            (currentTime - lastUpdateTime) / 1000f // Convert to seconds
        } else {
            0.016f // Default to ~60 FPS
        }
        lastUpdateTime = currentTime
        
        // Update tracking state
        _trackingState.value = trackingState
        _trackingConfidence.value = trackingConfidence
        
        // Handle different tracking states
        when (trackingState) {
            TrackingState.TRACKING -> {
                smoothedPose = if (smoothedPose == null) {
                    // First pose - no smoothing
                    rawPose
                } else {
                    // Smooth the pose
                    smoothPose(rawPose, deltaTime)
                }
                
                // Update velocity for prediction
                updateVelocity(rawPose, deltaTime)
                
                // Check stability
                _isStable.value = isTrackingStable(trackingConfidence)
            }
            
            TrackingState.PAUSED -> {
                // Predict pose based on velocity
                smoothedPose = predictPose(deltaTime)
                _isStable.value = false
            }
            
            TrackingState.STOPPED -> {
                // Keep last known pose
                _isStable.value = false
            }
        }
        
        previousRawPose = rawPose
        
        return smoothedPose ?: rawPose
    }
    
    /**
     * Smooth pose using exponential moving average
     */
    private fun smoothPose(rawPose: Pose, deltaTime: Float): Pose {
        val current = smoothedPose ?: return rawPose
        
        // Smooth position (exponential moving average)
        val smoothedTx = lerp(current.tx(), rawPose.tx(), smoothingFactor)
        val smoothedTy = lerp(current.ty(), rawPose.ty(), smoothingFactor)
        val smoothedTz = lerp(current.tz(), rawPose.tz(), smoothingFactor)
        
        // Smooth rotation (quaternion slerp)
        val currentQuat = floatArrayOf(current.qx(), current.qy(), current.qz(), current.qw())
        val rawQuat = floatArrayOf(rawPose.qx(), rawPose.qy(), rawPose.qz(), rawPose.qw())
        val smoothedQuat = slerp(currentQuat, rawQuat, smoothingFactor)
        
        // Create smoothed pose
        val translation = floatArrayOf(smoothedTx, smoothedTy, smoothedTz)
        val rotation = smoothedQuat
        
        return Pose(translation, rotation)
    }
    
    /**
     * Update velocity vectors for prediction
     */
    private fun updateVelocity(rawPose: Pose, deltaTime: Float) {
        val previous = previousRawPose ?: return
        
        if (deltaTime <= 0) return
        
        // Position velocity
        positionVelocity[0] = (rawPose.tx() - previous.tx()) / deltaTime
        positionVelocity[1] = (rawPose.ty() - previous.ty()) / deltaTime
        positionVelocity[2] = (rawPose.tz() - previous.tz()) / deltaTime
        
        // Rotation velocity (simplified - using quaternion difference)
        rotationVelocity[0] = (rawPose.qx() - previous.qx()) / deltaTime
        rotationVelocity[1] = (rawPose.qy() - previous.qy()) / deltaTime
        rotationVelocity[2] = (rawPose.qz() - previous.qz()) / deltaTime
        rotationVelocity[3] = (rawPose.qw() - previous.qw()) / deltaTime
    }
    
    /**
     * Predict pose based on velocity when tracking is lost briefly
     */
    private fun predictPose(deltaTime: Float): Pose {
        val current = smoothedPose ?: return smoothedPose!!
        
        // Predict position
        val predictedTx = current.tx() + positionVelocity[0] * deltaTime
        val predictedTy = current.ty() + positionVelocity[1] * deltaTime
        val predictedTz = current.tz() + positionVelocity[2] * deltaTime
        
        // Predict rotation
        val predictedQx = current.qx() + rotationVelocity[0] * deltaTime
        val predictedQy = current.qy() + rotationVelocity[1] * deltaTime
        val predictedQz = current.qz() + rotationVelocity[2] * deltaTime
        val predictedQw = current.qw() + rotationVelocity[3] * deltaTime
        
        // Normalize quaternion
        val quat = floatArrayOf(predictedQx, predictedQy, predictedQz, predictedQw)
        val normalizedQuat = normalizeQuaternion(quat)
        
        val translation = floatArrayOf(predictedTx, predictedTy, predictedTz)
        
        // Dampen velocity over time (decay)
        dampVelocity(0.95f)
        
        return Pose(translation, normalizedQuat)
    }
    
    /**
     * Check if tracking is stable
     */
    private fun isTrackingStable(confidence: Float): Boolean {
        return confidence >= confidenceThreshold
    }
    
    /**
     * Dampen velocity (decay over time)
     */
    private fun dampVelocity(dampingFactor: Float) {
        for (i in positionVelocity.indices) {
            positionVelocity[i] *= dampingFactor
        }
        for (i in rotationVelocity.indices) {
            rotationVelocity[i] *= dampingFactor
        }
    }
    
    /**
     * Get current smoothed pose
     */
    fun getCurrentPose(): Pose? {
        return smoothedPose
    }
    
    /**
     * Reset tracker
     */
    fun reset() {
        smoothedPose = null
        previousRawPose = null
        positionVelocity = FloatArray(3) { 0f }
        rotationVelocity = FloatArray(4) { 0f }
        _trackingConfidence.value = 0f
        _trackingState.value = TrackingState.STOPPED
        _isStable.value = false
        lastUpdateTime = 0L
        Log.d(TAG, "PoseTracker reset")
    }
    
    // ===== Math Utilities =====
    
    /**
     * Linear interpolation
     */
    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }
    
    /**
     * Spherical linear interpolation for quaternions
     */
    private fun slerp(q1: FloatArray, q2: FloatArray, t: Float): FloatArray {
        var dot = q1[0] * q2[0] + q1[1] * q2[1] + q1[2] * q2[2] + q1[3] * q2[3]
        
        // If quaternions are pointing in opposite directions, negate one
        val q2Copy = if (dot < 0) {
            dot = -dot
            floatArrayOf(-q2[0], -q2[1], -q2[2], -q2[3])
        } else {
            q2
        }
        
        // If quaternions are very close, use linear interpolation
        if (dot > 0.9995f) {
            val result = FloatArray(4)
            result[0] = lerp(q1[0], q2Copy[0], t)
            result[1] = lerp(q1[1], q2Copy[1], t)
            result[2] = lerp(q1[2], q2Copy[2], t)
            result[3] = lerp(q1[3], q2Copy[3], t)
            return normalizeQuaternion(result)
        }
        
        // Spherical interpolation
        val theta = acos(dot)
        val sinTheta = sin(theta)
        val w1 = sin((1 - t) * theta) / sinTheta
        val w2 = sin(t * theta) / sinTheta
        
        return floatArrayOf(
            w1 * q1[0] + w2 * q2Copy[0],
            w1 * q1[1] + w2 * q2Copy[1],
            w1 * q1[2] + w2 * q2Copy[2],
            w1 * q1[3] + w2 * q2Copy[3]
        )
    }
    
    /**
     * Normalize quaternion
     */
    private fun normalizeQuaternion(q: FloatArray): FloatArray {
        val magnitude = sqrt(q[0] * q[0] + q[1] * q[1] + q[2] * q[2] + q[3] * q[3])
        return if (magnitude > 0) {
            floatArrayOf(
                q[0] / magnitude,
                q[1] / magnitude,
                q[2] / magnitude,
                q[3] / magnitude
            )
        } else {
            floatArrayOf(0f, 0f, 0f, 1f) // Identity quaternion
        }
    }
}

/**
 * Confidence-based alpha calculator for smooth fading
 */
object ConfidenceFader {
    
    /**
     * Calculate alpha (opacity) based on tracking confidence
     * 
     * @param confidence Tracking confidence (0.0 - 1.0)
     * @param minAlpha Minimum alpha when confidence is 0
     * @param maxAlpha Maximum alpha when confidence is 1
     * @return Alpha value for rendering
     */
    fun calculateAlpha(
        confidence: Float,
        minAlpha: Float = 0.3f,
        maxAlpha: Float = 1.0f
    ): Float {
        // Smooth curve: alpha = minAlpha + (maxAlpha - minAlpha) * smoothstep(confidence)
        val smoothConfidence = smoothstep(confidence)
        return minAlpha + (maxAlpha - minAlpha) * smoothConfidence
    }
    
    /**
     * Smoothstep function for smooth interpolation
     */
    private fun smoothstep(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    
    /**
     * Check if avatar should be visible based on confidence
     */
    fun shouldBeVisible(confidence: Float, threshold: Float = 0.2f): Boolean {
        return confidence >= threshold
    }
}

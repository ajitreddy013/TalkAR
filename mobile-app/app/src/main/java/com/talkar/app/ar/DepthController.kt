package com.talkar.app.ar

import android.util.Log
import com.google.ar.core.Pose
import com.google.ar.sceneform.math.Vector3
import kotlin.math.*

/**
 * DepthController - Z-axis Depth Management for AR Avatars
 * 
 * Ensures avatars appear above posters, not inside them, with proper depth control.
 * Manages Z-offset, scale-based depth adjustment, and collision avoidance.
 */
class DepthController(
    private val baseDepthOffset: Float = 0.05f,  // 5cm above poster by default
    private val minDepthOffset: Float = 0.02f,   // Minimum 2cm
    private val maxDepthOffset: Float = 0.15f    // Maximum 15cm
) {
    
    private val TAG = "DepthController"
    
    /**
     * Calculate optimal Z-offset based on avatar scale and distance
     * 
     * @param avatarScale Scale of the avatar (larger avatars need more offset)
     * @param distanceToCamera Distance from camera to image anchor
     * @return Z-offset in meters
     */
    fun calculateZOffset(
        avatarScale: Float = 1.0f,
        distanceToCamera: Float = 1.0f
    ): Float {
        // Base offset scaled by avatar size
        var offset = baseDepthOffset * avatarScale
        
        // Increase offset for distant objects (perspective correction)
        val distanceFactor = (distanceToCamera / 2.0f).coerceIn(0.5f, 2.0f)
        offset *= distanceFactor
        
        // Clamp to safe range
        return offset.coerceIn(minDepthOffset, maxDepthOffset)
    }
    
    /**
     * Apply Z-offset to a pose
     * Moves the pose forward along its local Z-axis
     */
    fun applyZOffset(pose: Pose, offset: Float): Pose {
        // Get pose rotation matrix
        val rotationMatrix = FloatArray(16)
        pose.getRotationMatrix(rotationMatrix, 0)
        
        // Extract local Z-axis (forward direction)
        val forwardX = rotationMatrix[8]
        val forwardY = rotationMatrix[9]
        val forwardZ = rotationMatrix[10]
        
        // Apply offset along local Z-axis
        val newTranslation = floatArrayOf(
            pose.tx() + forwardX * offset,
            pose.ty() + forwardY * offset,
            pose.tz() + forwardZ * offset
        )
        
        val rotation = floatArrayOf(pose.qx(), pose.qy(), pose.qz(), pose.qw())
        
        return Pose(newTranslation, rotation)
    }
    
    /**
     * Calculate distance from camera to pose
     */
    fun calculateDistance(pose: Pose): Float {
        val tx = pose.tx()
        val ty = pose.ty()
        val tz = pose.tz()
        return sqrt(tx * tx + ty * ty + tz * tz)
    }
    
    /**
     * Check if avatar is at safe depth (not too close or far)
     */
    fun isAtSafeDepth(pose: Pose): Boolean {
        val distance = calculateDistance(pose)
        return distance in 0.2f..5.0f // 20cm to 5m
    }
    
    /**
     * Convert Sceneform Vector3 to position offset
     */
    fun toVector3(zOffset: Float): Vector3 {
        return Vector3(0f, 0f, zOffset)
    }
}

/**
 * AnchorStabilizer - Maintains stable anchoring during camera movement
 * 
 * Prevents jitter and ensures smooth tracking when camera moves.
 */
class AnchorStabilizer {
    
    private val TAG = "AnchorStabilizer"
    
    // Position history for averaging
    private val positionHistory = mutableListOf<FloatArray>()
    private val maxHistorySize = 5
    
    // Rotation history for averaging
    private val rotationHistory = mutableListOf<FloatArray>()
    
    /**
     * Stabilize pose using moving average
     */
    fun stabilizePose(pose: Pose): Pose {
        // Add to history
        val position = floatArrayOf(pose.tx(), pose.ty(), pose.tz())
        val rotation = floatArrayOf(pose.qx(), pose.qy(), pose.qz(), pose.qw())
        
        positionHistory.add(position)
        rotationHistory.add(rotation)
        
        // Limit history size
        if (positionHistory.size > maxHistorySize) {
            positionHistory.removeAt(0)
        }
        if (rotationHistory.size > maxHistorySize) {
            rotationHistory.removeAt(0)
        }
        
        // Calculate averaged position
        val avgPosition = averagePositions()
        
        // Calculate averaged rotation (quaternion average)
        val avgRotation = averageQuaternions()
        
        return Pose(avgPosition, avgRotation)
    }
    
    /**
     * Average positions using arithmetic mean
     */
    private fun averagePositions(): FloatArray {
        val sum = FloatArray(3) { 0f }
        positionHistory.forEach { pos ->
            sum[0] += pos[0]
            sum[1] += pos[1]
            sum[2] += pos[2]
        }
        val count = positionHistory.size.toFloat()
        return floatArrayOf(
            sum[0] / count,
            sum[1] / count,
            sum[2] / count
        )
    }
    
    /**
     * Average quaternions using weighted average
     */
    private fun averageQuaternions(): FloatArray {
        if (rotationHistory.isEmpty()) {
            return floatArrayOf(0f, 0f, 0f, 1f)
        }
        
        // Simple average (for small rotations, this works well)
        val sum = FloatArray(4) { 0f }
        rotationHistory.forEach { quat ->
            sum[0] += quat[0]
            sum[1] += quat[1]
            sum[2] += quat[2]
            sum[3] += quat[3]
        }
        
        val count = rotationHistory.size.toFloat()
        val avg = floatArrayOf(
            sum[0] / count,
            sum[1] / count,
            sum[2] / count,
            sum[3] / count
        )
        
        // Normalize
        return normalizeQuaternion(avg)
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
            floatArrayOf(0f, 0f, 0f, 1f)
        }
    }
    
    /**
     * Reset stabilizer
     */
    fun reset() {
        positionHistory.clear()
        rotationHistory.clear()
        Log.d(TAG, "AnchorStabilizer reset")
    }
    
    /**
     * Get stabilization quality (0.0 - 1.0)
     * Higher means more stable
     */
    fun getStabilityQuality(): Float {
        return (positionHistory.size.toFloat() / maxHistorySize.toFloat()).coerceIn(0f, 1f)
    }
}

/**
 * TransitionManager - Manages smooth transitions for confidence changes
 * 
 * Handles fading in/out based on tracking confidence with smooth animations.
 */
class TransitionManager(
    private val fadeInDuration: Float = 0.3f,   // 300ms fade in
    private val fadeOutDuration: Float = 0.5f   // 500ms fade out
) {
    
    private val TAG = "TransitionManager"
    
    // Current transition state
    private var currentAlpha = 0f
    private var targetAlpha = 1f
    private var transitionStartTime = 0L
    private var transitionDuration = 0f
    
    /**
     * Update transition based on tracking confidence
     * 
     * @param confidence Current tracking confidence (0.0 - 1.0)
     * @param deltaTime Time since last update (seconds)
     * @return Current alpha for rendering
     */
    fun updateTransition(confidence: Float, deltaTime: Float): Float {
        // Calculate target alpha from confidence
        val newTargetAlpha = ConfidenceFader.calculateAlpha(confidence)
        
        // Check if target changed
        if (abs(newTargetAlpha - targetAlpha) > 0.01f) {
            // Start new transition
            targetAlpha = newTargetAlpha
            transitionStartTime = System.currentTimeMillis()
            transitionDuration = if (newTargetAlpha > currentAlpha) {
                fadeInDuration
            } else {
                fadeOutDuration
            }
        }
        
        // Interpolate current alpha towards target
        val progress = if (transitionDuration > 0) {
            val elapsed = (System.currentTimeMillis() - transitionStartTime) / 1000f
            (elapsed / transitionDuration).coerceIn(0f, 1f)
        } else {
            1f
        }
        
        // Smooth interpolation
        currentAlpha = lerp(currentAlpha, targetAlpha, smoothstep(progress))
        
        return currentAlpha.coerceIn(0f, 1f)
    }
    
    /**
     * Get current alpha value
     */
    fun getCurrentAlpha(): Float {
        return currentAlpha
    }
    
    /**
     * Check if transition is complete
     */
    fun isTransitionComplete(): Boolean {
        return abs(currentAlpha - targetAlpha) < 0.01f
    }
    
    /**
     * Reset transition
     */
    fun reset() {
        currentAlpha = 0f
        targetAlpha = 1f
        transitionStartTime = 0L
        Log.d(TAG, "TransitionManager reset")
    }
    
    // Math utilities
    
    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t
    }
    
    private fun smoothstep(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}

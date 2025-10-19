package com.talkar.app.ar

import android.util.Log
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * Camera Angle Tracker for AR Overlays with Smooth Movement
 * 
 * Tracks camera angle changes and provides scale/rotation values for overlays
 * with enhanced smoothing and stability features.
 */
class CameraAngleTracker {
    
    private val TAG = "CameraAngleTracker"
    
    // Pose tracker for smooth camera movement
    private val poseTracker = PoseTracker(smoothingFactor = 0.4f)
    
    // Overlay transformation states
    private val _overlayScale = MutableStateFlow(1.0f)
    val overlayScale: StateFlow<Float> = _overlayScale.asStateFlow()
    
    private val _overlayRotation = MutableStateFlow(0.0f)
    val overlayRotation: StateFlow<Float> = _overlayRotation.asStateFlow()
    
    private val _overlayAlpha = MutableStateFlow(1.0f)
    val overlayAlpha: StateFlow<Float> = _overlayAlpha.asStateFlow()
    
    /**
     * Update overlay transform based on augmented image pose
     * Now with smooth tracking and confidence-based fading
     */
    fun updateTransform(augmentedImage: AugmentedImage) {
        try {
            val pose = augmentedImage.centerPose
            val trackingState = augmentedImage.trackingState
            
            // Calculate tracking confidence based on tracking method
            val trackingConfidence = when (augmentedImage.trackingMethod) {
                AugmentedImage.TrackingMethod.FULL_TRACKING -> 1.0f
                AugmentedImage.TrackingMethod.LAST_KNOWN_POSE -> 0.5f
                AugmentedImage.TrackingMethod.NOT_TRACKING -> 0.1f
                else -> 0.5f
            }
            
            // Use pose tracker for smooth updates
            val smoothedPose = poseTracker.updatePose(pose, trackingState, trackingConfidence)
            
            // Calculate distance for scale adjustment
            val distance = calculateDistance(smoothedPose)
            val scale = calculateScale(distance)
            _overlayScale.value = scale
            
            // Calculate rotation from pose quaternion
            val rotation = calculateRotation(smoothedPose)
            _overlayRotation.value = rotation
            
            // Calculate alpha based on tracking confidence (smooth fading)
            val alpha = ConfidenceFader.calculateAlpha(trackingConfidence)
            _overlayAlpha.value = alpha
            
            Log.d(TAG, "Transform updated (smooth) - Distance: $distance, Scale: $scale, Rotation: $rotation, Alpha: $alpha, Confidence: $trackingConfidence")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transform", e)
        }
    }
    
    /**
     * Calculate distance from camera to image
     */
    private fun calculateDistance(pose: Pose): Float {
        val tx = pose.tx()
        val ty = pose.ty()
        val tz = pose.tz()
        return sqrt(tx * tx + ty * ty + tz * tz)
    }
    
    /**
     * Calculate scale based on distance
     * Closer images appear larger, farther images appear smaller
     */
    private fun calculateScale(distance: Float): Float {
        // Base scale at 1 meter
        val baseDistance = 1.0f
        val scale = baseDistance / max(distance, 0.3f)
        
        // Clamp scale to reasonable range
        return scale.coerceIn(0.5f, 2.0f)
    }
    
    /**
     * Calculate rotation from quaternion
     * Converts quaternion to Euler angle (Z-axis rotation)
     */
    private fun calculateRotation(pose: Pose): Float {
        val qx = pose.qx()
        val qy = pose.qy()
        val qz = pose.qz()
        val qw = pose.qw()
        
        // Convert quaternion to Euler angle (yaw/Z-axis rotation)
        val siny_cosp = 2.0 * (qw * qz + qx * qy)
        val cosy_cosp = 1.0 - 2.0 * (qy * qy + qz * qz)
        val yaw = atan2(siny_cosp, cosy_cosp)
        
        // Convert to degrees
        val degrees = Math.toDegrees(yaw).toFloat()
        
        // Normalize to -180 to 180
        return when {
            degrees > 180 -> degrees - 360
            degrees < -180 -> degrees + 360
            else -> degrees
        }
    }
    
    /**
     * Reset all transforms to default
     */
    fun reset() {
        _overlayScale.value = 1.0f
        _overlayRotation.value = 0.0f
        _overlayAlpha.value = 1.0f
        Log.d(TAG, "Transforms reset to default")
    }
    
    /**
     * Get current transform values as map
     */
    fun getTransformValues(): Map<String, Float> {
        return mapOf(
            "scale" to _overlayScale.value,
            "rotation" to _overlayRotation.value,
            "alpha" to _overlayAlpha.value
        )
    }
}
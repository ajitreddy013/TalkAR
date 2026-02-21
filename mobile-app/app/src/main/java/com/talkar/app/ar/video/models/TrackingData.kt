package com.talkar.app.ar.video.models

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3

/**
 * Tracking data from ARCore for a detected image anchor.
 *
 * @property position 3D position of the anchor in world space
 * @property rotation Rotation of the anchor as a quaternion
 * @property scale 2D scale of the image (width, height in meters)
 * @property isTracking Whether the anchor is currently being tracked
 * @property timestamp Timestamp of this tracking update in milliseconds
 */
data class TrackingData(
    val position: Vector3,
    val rotation: Quaternion,
    val scale: Vector2,
    val isTracking: Boolean,
    val timestamp: Long
)

/**
 * 2D vector for scale dimensions.
 */
data class Vector2(
    val x: Float,
    val y: Float
) {
    companion object {
        fun zero() = Vector2(0f, 0f)
    }
}

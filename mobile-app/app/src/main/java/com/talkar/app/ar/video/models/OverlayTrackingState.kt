package com.talkar.app.ar.video.models

/**
 * Tracking state for a video overlay with timeout logic.
 *
 * @property imageId ID of the tracked image
 * @property anchorId ID of the ARCore anchor
 * @property isTracking Whether the image is currently being tracked
 * @property lastUpdateTimestamp Timestamp of last tracking update
 * @property consecutiveTrackingFrames Number of consecutive frames with tracking
 * @property consecutiveLostFrames Number of consecutive frames without tracking
 */
data class OverlayTrackingState(
    val imageId: String,
    val anchorId: String,
    val isTracking: Boolean,
    val lastUpdateTimestamp: Long,
    val consecutiveTrackingFrames: Int,
    val consecutiveLostFrames: Int
) {
    companion object {
        /** 5 seconds at 60fps = 300 frames */
        const val TRACKING_LOST_THRESHOLD = 300
        
        /** 0.5 seconds at 60fps = 30 frames */
        const val TRACKING_STABLE_THRESHOLD = 30
    }

    /**
     * Checks if the overlay should be deactivated due to prolonged tracking loss.
     */
    fun shouldDeactivate(): Boolean {
        return !isTracking && consecutiveLostFrames >= TRACKING_LOST_THRESHOLD
    }

    /**
     * Checks if tracking is stable (has been tracking for sufficient frames).
     */
    fun isStable(): Boolean {
        return isTracking && consecutiveTrackingFrames >= TRACKING_STABLE_THRESHOLD
    }

    /**
     * Creates a new state with updated tracking status.
     */
    fun withTrackingUpdate(nowTracking: Boolean, timestamp: Long): OverlayTrackingState {
        return if (nowTracking) {
            copy(
                isTracking = true,
                lastUpdateTimestamp = timestamp,
                consecutiveTrackingFrames = consecutiveTrackingFrames + 1,
                consecutiveLostFrames = 0
            )
        } else {
            copy(
                isTracking = false,
                lastUpdateTimestamp = timestamp,
                consecutiveTrackingFrames = 0,
                consecutiveLostFrames = consecutiveLostFrames + 1
            )
        }
    }
}

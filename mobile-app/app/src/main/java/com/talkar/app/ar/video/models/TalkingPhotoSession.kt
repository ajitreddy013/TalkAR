package com.talkar.app.ar.video.models

/**
 * Represents the current state of a talking photo session.
 * 
 * Tracks all information needed to manage a single poster's lip-sync playback,
 * including tracking state, video information, and playback position.
 * 
 * @property posterId Unique identifier for the poster
 * @property anchorId ARCore anchor identifier
 * @property state Current state of the talking photo
 * @property lipCoordinates Normalized lip region coordinates
 * @property videoPath Local file path to the lip-sync video
 * @property isTracking Whether the poster is currently being tracked by ARCore
 * @property lastUpdateTimestamp Timestamp of the last tracking update (milliseconds)
 * @property consecutiveTrackingFrames Number of consecutive frames with successful tracking
 * @property consecutiveLostFrames Number of consecutive frames with lost tracking
 */
data class TalkingPhotoSession(
    val posterId: String,
    val anchorId: String,
    val state: TalkingPhotoState,
    val lipCoordinates: LipCoordinates? = null,
    val videoPath: String? = null,
    val isTracking: Boolean = false,
    val lastUpdateTimestamp: Long = 0L,
    val consecutiveTrackingFrames: Int = 0,
    val consecutiveLostFrames: Int = 0
) {
    companion object {
        /**
         * Number of consecutive lost frames before considering tracking truly lost.
         * At 60fps, this represents approximately 5 seconds.
         */
        const val TRACKING_LOST_THRESHOLD = 300
        
        /**
         * Number of consecutive tracking frames before considering tracking stable.
         * At 60fps, this represents approximately 0.5 seconds.
         */
        const val TRACKING_STABLE_THRESHOLD = 30
    }
    
    /**
     * Determines if video playback should be paused due to tracking loss.
     * 
     * @return true if tracking is lost and playback should pause
     */
    fun shouldPause(): Boolean {
        return !isTracking
    }
    
    /**
     * Determines if video playback should resume after tracking recovery.
     * 
     * @return true if tracking is active and playback should resume
     */
    fun shouldResume(): Boolean {
        return isTracking && consecutiveTrackingFrames > 0
    }
    
    /**
     * Determines if the session should be deactivated due to prolonged tracking loss.
     * 
     * @return true if tracking has been lost for more than TRACKING_LOST_THRESHOLD frames
     */
    fun shouldDeactivate(): Boolean {
        return !isTracking && consecutiveLostFrames >= TRACKING_LOST_THRESHOLD
    }
    
    /**
     * Determines if tracking is stable enough for smooth playback.
     * 
     * @return true if tracking has been stable for TRACKING_STABLE_THRESHOLD frames
     */
    fun isTrackingStable(): Boolean {
        return isTracking && consecutiveTrackingFrames >= TRACKING_STABLE_THRESHOLD
    }
    
    /**
     * Updates the session with new tracking data.
     * 
     * @param isCurrentlyTracking Whether tracking is active in the current frame
     * @param timestamp Current frame timestamp
     * @return Updated session with new tracking state
     */
    fun updateTracking(isCurrentlyTracking: Boolean, timestamp: Long): TalkingPhotoSession {
        return if (isCurrentlyTracking) {
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

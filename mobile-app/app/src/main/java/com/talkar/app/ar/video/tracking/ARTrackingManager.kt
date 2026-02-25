package com.talkar.app.ar.video.tracking

import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.talkar.app.ar.video.models.TrackedImage

/**
 * Manages ARCore poster detection and tracking with single poster mode.
 *
 * Responsibilities:
 * - Initialize AR session with poster database
 * - Detect and track posters with human faces
 * - Enforce single poster mode (track only one at a time)
 * - Provide "Refresh Scan" functionality
 * - Handle out-of-frame detection
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 6.1, 6.2, 6.3, 6.4
 */
interface ARTrackingManager {
    
    /**
     * Initializes AR session with poster database.
     *
     * @param posters List of reference posters to detect
     * @return Result indicating success or failure with error message
     */
    suspend fun initialize(posters: List<ReferencePoster>): Result<Unit>
    
    /**
     * Processes AR frame and detects posters.
     * Called every frame (60fps).
     *
     * @param frame Current AR frame from ARCore
     * @return Detected poster if any, null otherwise
     */
    fun processFrame(frame: Frame): TrackedPoster?
    
    /**
     * Gets anchor for the currently tracked poster.
     *
     * @return Anchor if poster is being tracked, null otherwise
     */
    fun getCurrentAnchor(): Anchor?
    
    /**
     * Clears current poster and allows scanning a new one.
     * Implements "Refresh Scan" functionality.
     */
    fun refreshScan()
    
    /**
     * Registers listener for tracking events.
     *
     * @param listener Callback interface for tracking events
     */
    fun setListener(listener: TrackingListener)
    
    /**
     * Releases AR resources.
     * Should be called when tracking is no longer needed.
     */
    fun release()
}

/**
 * Reference poster data for AR detection.
 *
 * @property id Unique identifier for the poster
 * @property name Display name of the poster
 * @property imageData Image bitmap data for AR detection
 * @property physicalWidthMeters Physical width of the poster in meters
 * @property hasHumanFace Whether this poster contains a human face
 */
data class ReferencePoster(
    val id: String,
    val name: String,
    val imageData: ByteArray,
    val physicalWidthMeters: Float,
    val hasHumanFace: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ReferencePoster
        
        if (id != other.id) return false
        if (name != other.name) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (physicalWidthMeters != other.physicalWidthMeters) return false
        if (hasHumanFace != other.hasHumanFace) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + physicalWidthMeters.hashCode()
        result = 31 * result + hasHumanFace.hashCode()
        return result
    }
}

/**
 * Tracked poster with current state.
 *
 * @property id Poster identifier
 * @property name Poster name
 * @property anchor ARCore anchor for the poster
 * @property trackingState Current tracking state
 * @property extentX Physical width in meters
 * @property extentZ Physical height in meters
 */
data class TrackedPoster(
    val id: String,
    val name: String,
    val anchor: Anchor,
    val trackingState: TrackingState,
    val extentX: Float,
    val extentZ: Float
)

/**
 * ARCore tracking state.
 */
enum class TrackingState {
    TRACKING,
    PAUSED,
    STOPPED
}

/**
 * Listener interface for tracking events.
 */
interface TrackingListener {
    /**
     * Called when a poster is first detected.
     *
     * @param poster The detected poster with anchor
     */
    fun onPosterDetected(poster: TrackedPoster)
    
    /**
     * Called every frame while poster is being tracked.
     *
     * @param poster The tracked poster with updated state
     */
    fun onPosterTracking(poster: TrackedPoster)
    
    /**
     * Called when poster tracking is lost (out of frame).
     *
     * @param posterId ID of the lost poster
     */
    fun onPosterLost(posterId: String)
    
    /**
     * Called when poster detection times out after 10 seconds.
     * Requirement: 14.1
     */
    fun onDetectionTimeout()
}

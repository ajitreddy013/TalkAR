package com.talkar.app.ar.video

import com.google.ar.core.Anchor
import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.LipCoordinates
import com.talkar.app.ar.video.models.TalkingPhotoState
import com.talkar.app.ar.video.models.TrackingData

/**
 * Orchestrates the complete talking photo lifecycle from poster detection
 * through video generation to playback.
 * 
 * Coordinates between BackendVideoFetcher, VideoCache, VideoDecoder,
 * ARTrackingManager, RenderCoordinator, and LipRegionRenderer.
 */
interface TalkingPhotoController {
    /**
     * Initializes talking photo for a detected poster.
     * 
     * @param anchor ARCore image anchor for the poster
     * @param posterId Unique identifier for the poster
     * @return Result indicating success or failure
     */
    suspend fun initialize(anchor: Anchor?, posterId: String): Result<Unit>
    
    /**
     * Updates overlay position based on latest tracking data.
     * Called every frame (60fps).
     * 
     * @param trackingData Current position and orientation from ARCore
     */
    fun updateTracking(trackingData: TrackingData)
    
    /**
     * Controls playback state.
     */
    fun play()
    fun pause()
    fun stop()
    
    /**
     * Gets current state.
     */
    fun getState(): TalkingPhotoState
    
    /**
     * Gets current playback position in milliseconds.
     */
    fun getCurrentPosition(): Long
    
    /**
     * Seeks to a specific position in the video.
     * 
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long)
    
    /**
     * Releases all resources.
     */
    fun release()
    
    /**
     * Registers callbacks for lifecycle events.
     */
    fun setCallbacks(callbacks: TalkingPhotoCallbacks)

    /**
     * Gets the path of the downloaded video file.
     */
    fun getVideoPath(): String? = null
}

/**
 * Callbacks for talking photo lifecycle events.
 */
interface TalkingPhotoCallbacks {
    fun onGenerationStarted() {}
    fun onGenerationProgress(progress: Float) {}
    fun onArtifactStatus(status: String) {}
    fun onVideoReady() {}
    fun onFirstFrameRendered() {}
    fun onPlaybackComplete() {}
    fun onError(error: TalkingPhotoError) {}
    fun onTrackingLost() {}
    fun onTrackingResumed() {}
    fun onLipCoordinatesReady(lipCoordinates: LipCoordinates) {}
    /** Screen-fraction bounds [left, top, right, bottom] of the poster, 0..1. */
    fun onPosterScreenBounds(left: Float, top: Float, right: Float, bottom: Float) {}
    /**
     * Directly-projected lip region screen fractions [left, top, right, bottom].
     * More accurate than computing from poster bounds × lip UV fractions, especially
     * when the poster is viewed from an angle (projected as a trapezoid, not a rect).
     */
    fun onLipScreenBounds(left: Float, top: Float, right: Float, bottom: Float) {}
}

package com.talkar.app.ar.video.models

/**
 * Represents the current state of the talking photo feature.
 * 
 * Canonical state transitions:
 * IDLE → DETECTED → FETCHING_ARTIFACT → READY → PLAYING → LOST_TRACKING/PAUSED → RESUMED → PLAYING
 *                                                                  ↓
 *                                                                 ERROR
 */
enum class TalkingPhotoState {
    /**
     * Initial state, waiting for poster detection.
     */
    IDLE,

    PREVIEW_READY,

    POSTERS_LOADING,

    TRACKING_READY,

    POSTER_DETECTED,
    
    /**
     * Poster detected, checking cache for existing video.
     */
    DETECTED,

    FETCHING_ARTIFACT,

    ARTIFACT_WAITING,
    
    /**
     * Video downloaded and ready for playback.
     */
    READY,
    
    /**
     * Video is currently playing.
     */
    PLAYING,

    LOST_TRACKING,
    
    /**
     * Video playback is paused (e.g., poster out of frame).
     */
    PAUSED,

    RESUMED,

    ERROR_RETRYABLE,
    
    /**
     * An error occurred during any stage.
     */
    ERROR
}

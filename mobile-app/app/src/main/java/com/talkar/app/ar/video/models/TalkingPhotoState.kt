package com.talkar.app.ar.video.models

/**
 * Represents the current state of the talking photo feature.
 * 
 * State transitions:
 * IDLE → FETCHING_VIDEO → GENERATING → DOWNLOADING → READY → PLAYING → PAUSED
 *                                                                    ↓
 *                                                                  ERROR
 */
enum class TalkingPhotoState {
    /**
     * Initial state, waiting for poster detection.
     */
    IDLE,
    
    /**
     * Poster detected, checking cache for existing video.
     */
    FETCHING_VIDEO,
    
    /**
     * Video not in cache, backend is generating lip-sync video.
     */
    GENERATING,
    
    /**
     * Backend generation complete, downloading video.
     */
    DOWNLOADING,
    
    /**
     * Video downloaded and ready for playback.
     */
    READY,
    
    /**
     * Video is currently playing.
     */
    PLAYING,
    
    /**
     * Video playback is paused (e.g., poster out of frame).
     */
    PAUSED,
    
    /**
     * An error occurred during any stage.
     */
    ERROR
}

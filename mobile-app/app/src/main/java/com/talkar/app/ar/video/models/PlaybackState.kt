package com.talkar.app.ar.video.models

/**
 * Playback state of a video overlay.
 */
enum class PlaybackState {
    /** Initial state, no video loaded */
    IDLE,
    
    /** Video is being initialized and loaded */
    INITIALIZING,
    
    /** Video is loaded and ready to play */
    READY,
    
    /** Video is currently playing */
    PLAYING,
    
    /** Video is paused */
    PAUSED,
    
    /** Video is stopped */
    STOPPED,
    
    /** An error occurred */
    ERROR
}

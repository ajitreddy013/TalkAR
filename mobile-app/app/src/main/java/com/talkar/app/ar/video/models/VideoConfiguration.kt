package com.talkar.app.ar.video.models

import android.net.Uri

/**
 * Configuration for video playback in AR overlay.
 *
 * @property uri URI of the video file to play (res/raw or external)
 * @property autoPlay Whether to start playback automatically when ready
 * @property looping Whether to loop the video when it reaches the end
 * @property volume Playback volume (0.0 to 1.0)
 * @property startPositionMs Starting position in milliseconds
 */
data class VideoConfiguration(
    val uri: Uri,
    val autoPlay: Boolean = true,
    val looping: Boolean = true,
    val volume: Float = 1.0f,
    val startPositionMs: Long = 0L
) {
    init {
        require(volume in 0.0f..1.0f) { "Volume must be between 0.0 and 1.0" }
        require(startPositionMs >= 0) { "Start position must be non-negative" }
    }
}

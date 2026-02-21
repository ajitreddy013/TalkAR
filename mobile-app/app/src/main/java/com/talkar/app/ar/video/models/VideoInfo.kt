package com.talkar.app.ar.video.models

/**
 * Information about a video file extracted from the decoder.
 *
 * @property width Video width in pixels
 * @property height Video height in pixels
 * @property durationMs Video duration in milliseconds
 * @property frameRate Video frame rate (fps)
 * @property codec Video codec name (e.g., "h264", "h265")
 * @property hasAudioTrack Whether the video has an audio track
 * @property hasVideoTrack Whether the video has a video track
 */
data class VideoInfo(
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val frameRate: Float,
    val codec: String,
    val hasAudioTrack: Boolean,
    val hasVideoTrack: Boolean
) {
    /**
     * Gets the aspect ratio of the video.
     */
    fun getAspectRatio(): Float {
        return if (height > 0) width.toFloat() / height.toFloat() else 0f
    }

    /**
     * Checks if the video has valid dimensions.
     */
    fun hasValidDimensions(): Boolean {
        return width > 0 && height > 0
    }
}

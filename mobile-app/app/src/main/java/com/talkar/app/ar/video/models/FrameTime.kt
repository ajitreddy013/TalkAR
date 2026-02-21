package com.talkar.app.ar.video.models

/**
 * Frame timing information for synchronized rendering.
 *
 * @property timestampNs Frame timestamp in nanoseconds
 * @property deltaTimeMs Time since last frame in milliseconds
 */
data class FrameTime(
    val timestampNs: Long,
    val deltaTimeMs: Float
)

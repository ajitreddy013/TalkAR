package com.talkar.app.ar.video.models

/**
 * Performance metrics for video playback session.
 *
 * @property sessionId Unique identifier for this playback session
 * @property videoUri URI of the video being played
 * @property initializationTimeMs Time taken to initialize decoder in milliseconds
 * @property firstFrameTimeMs Time taken to render first frame in milliseconds
 * @property totalFramesRendered Total number of frames successfully rendered
 * @property droppedFrames Number of frames that were dropped
 * @property averageFps Average frames per second achieved
 * @property trackingUpdateLatencyMs Rolling window of tracking update latencies
 * @property playbackDurationMs Total duration of playback in milliseconds
 */
data class VideoMetrics(
    val sessionId: String,
    val videoUri: String,
    val initializationTimeMs: Long,
    val firstFrameTimeMs: Long,
    val totalFramesRendered: Long,
    val droppedFrames: Long,
    val averageFps: Float,
    val trackingUpdateLatencyMs: FloatArray,
    val playbackDurationMs: Long
) {
    /**
     * Calculates the percentage of dropped frames.
     */
    fun getDroppedFramePercentage(): Float {
        val total = totalFramesRendered + droppedFrames
        return if (total > 0) {
            (droppedFrames.toFloat() / total) * 100f
        } else {
            0f
        }
    }

    /**
     * Calculates the average tracking update latency.
     */
    fun getAverageLatency(): Float {
        return if (trackingUpdateLatencyMs.isNotEmpty()) {
            trackingUpdateLatencyMs.average().toFloat()
        } else {
            0f
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VideoMetrics

        if (sessionId != other.sessionId) return false
        if (videoUri != other.videoUri) return false
        if (initializationTimeMs != other.initializationTimeMs) return false
        if (firstFrameTimeMs != other.firstFrameTimeMs) return false
        if (totalFramesRendered != other.totalFramesRendered) return false
        if (droppedFrames != other.droppedFrames) return false
        if (averageFps != other.averageFps) return false
        if (!trackingUpdateLatencyMs.contentEquals(other.trackingUpdateLatencyMs)) return false
        if (playbackDurationMs != other.playbackDurationMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + videoUri.hashCode()
        result = 31 * result + initializationTimeMs.hashCode()
        result = 31 * result + firstFrameTimeMs.hashCode()
        result = 31 * result + totalFramesRendered.hashCode()
        result = 31 * result + droppedFrames.hashCode()
        result = 31 * result + averageFps.hashCode()
        result = 31 * result + trackingUpdateLatencyMs.contentHashCode()
        result = 31 * result + playbackDurationMs.hashCode()
        return result
    }
}

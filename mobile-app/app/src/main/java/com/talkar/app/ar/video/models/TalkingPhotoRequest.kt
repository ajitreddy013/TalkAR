package com.talkar.app.ar.video.models

/**
 * Request model for generating a talking photo lip-sync video.
 * 
 * @property posterId Unique identifier for the poster/image
 * @property text Text content to be spoken (for TTS generation)
 * @property voiceId Voice identifier for TTS (e.g., "en-US-male-1")
 */
data class TalkingPhotoRequest(
    val posterId: String,
    val text: String,
    val voiceId: String = "default"
)

/**
 * Response from the backend when initiating video generation.
 * 
 * @property videoId Unique identifier for the video generation job
 * @property status Current status ("processing", "complete", "failed")
 * @property estimatedTimeSeconds Estimated time for generation to complete
 */
data class GenerateResponse(
    val videoId: String,
    val status: String,
    val estimatedTimeSeconds: Int? = null
)

/**
 * Response from the backend when checking generation status.
 * 
 * @property videoId Unique identifier for the video generation job
 * @property status Current status ("processing", "complete", "failed")
 * @property progress Generation progress (0.0 to 1.0)
 * @property videoUrl URL to download the completed video (only when status is "complete")
 * @property lipCoordinates Lip region coordinates (only when status is "complete")
 * @property checksum SHA-256 checksum for video integrity validation
 * @property durationMs Video duration in milliseconds
 * @property sizeBytes Video file size in bytes
 * @property errorMessage Error description (only when status is "failed")
 * @property estimatedTimeRemaining Estimated seconds until completion
 */
data class StatusResponse(
    val videoId: String,
    val status: String,
    val progress: Float = 0f,
    val videoUrl: String? = null,
    val lipCoordinates: LipCoordinatesDto? = null,
    val checksum: String? = null,
    val durationMs: Long? = null,
    val sizeBytes: Long? = null,
    val errorMessage: String? = null,
    val estimatedTimeRemaining: Int? = null
)

/**
 * DTO for lip coordinates from backend API.
 * 
 * @property lipX Normalized X coordinate (0-1 range)
 * @property lipY Normalized Y coordinate (0-1 range)
 * @property lipWidth Normalized width (0-1 range)
 * @property lipHeight Normalized height (0-1 range)
 */
data class LipCoordinatesDto(
    val lipX: Float,
    val lipY: Float,
    val lipWidth: Float,
    val lipHeight: Float
) {
    /**
     * Converts DTO to domain model with validation.
     */
    fun toLipCoordinates(): LipCoordinates {
        return LipCoordinates(
            lipX = lipX,
            lipY = lipY,
            lipWidth = lipWidth,
            lipHeight = lipHeight
        )
    }
}

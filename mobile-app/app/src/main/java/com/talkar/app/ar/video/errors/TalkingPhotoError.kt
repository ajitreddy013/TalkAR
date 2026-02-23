package com.talkar.app.ar.video.errors

/**
 * Sealed class hierarchy representing all possible errors in the talking photo feature.
 * 
 * Each error includes a descriptive message and a unique error code for logging and debugging.
 */
sealed class TalkingPhotoError(
    val message: String,
    val code: Int
) {
    /**
     * Poster was not detected within the timeout period (10 seconds).
     */
    class PosterNotDetected(message: String = "No poster detected within timeout") :
        TalkingPhotoError(message, ERROR_POSTER_NOT_DETECTED)
    
    /**
     * Backend service is unavailable or unreachable.
     */
    class BackendUnavailable(message: String = "Backend service unavailable") :
        TalkingPhotoError(message, ERROR_BACKEND_UNAVAILABLE)
    
    /**
     * Lip-sync video generation failed on the backend.
     */
    class GenerationFailed(message: String, val videoId: String? = null) :
        TalkingPhotoError(message, ERROR_GENERATION_FAILED)
    
    /**
     * Video download failed after all retry attempts.
     */
    class DownloadFailed(message: String, val url: String) :
        TalkingPhotoError(message, ERROR_DOWNLOAD_FAILED)
    
    /**
     * Lip coordinates received from backend are invalid (not in 0-1 range).
     */
    class InvalidCoordinates(message: String, val coordinates: String) :
        TalkingPhotoError(message, ERROR_INVALID_COORDINATES)
    
    /**
     * Cached video file is corrupted (checksum validation failed).
     */
    class CacheCorrupted(val posterId: String) :
        TalkingPhotoError("Cached video corrupted for poster: $posterId", ERROR_CACHE_CORRUPTED)
    
    /**
     * Detected poster does not contain a human face.
     */
    class NoHumanFace(message: String = "Poster does not contain a human face") :
        TalkingPhotoError(message, ERROR_NO_HUMAN_FACE)
    
    companion object {
        const val ERROR_POSTER_NOT_DETECTED = 2001
        const val ERROR_BACKEND_UNAVAILABLE = 2002
        const val ERROR_GENERATION_FAILED = 2003
        const val ERROR_DOWNLOAD_FAILED = 2004
        const val ERROR_INVALID_COORDINATES = 2005
        const val ERROR_CACHE_CORRUPTED = 2006
        const val ERROR_NO_HUMAN_FACE = 2007
    }
    
    /**
     * Returns a user-friendly error message suitable for display.
     */
    fun getUserMessage(): String {
        return when (this) {
            is PosterNotDetected -> "No poster detected. Try better lighting."
            is BackendUnavailable -> "Service unavailable. Please try again later."
            is GenerationFailed -> "Failed to generate video. Please try again."
            is DownloadFailed -> "Download failed. Please check your connection."
            is InvalidCoordinates -> "Invalid video data received. Please try again."
            is CacheCorrupted -> "Video corrupted. Re-downloading..."
            is NoHumanFace -> "Please scan a poster with a human face"
        }
    }
}

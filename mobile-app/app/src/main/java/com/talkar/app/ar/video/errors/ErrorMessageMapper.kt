package com.talkar.app.ar.video.errors

/**
 * Maps error codes to user-friendly messages with actionable advice.
 * 
 * Provides:
 * - User-friendly error messages
 * - Suggested actions
 * - Technical details for debugging
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.5
 */
object ErrorMessageMapper {
    
    /**
     * Error message data class.
     */
    data class ErrorMessage(
        val title: String,
        val message: String,
        val action: String,
        val isRetryable: Boolean,
        val technicalDetails: String? = null
    )
    
    /**
     * Maps a TalkingPhotoError to a user-friendly ErrorMessage.
     * 
     * Requirements: 14.1, 14.2, 14.3, 14.5
     */
    fun mapError(error: TalkingPhotoError): ErrorMessage {
        return when (error) {
            is TalkingPhotoError.PosterNotDetected -> ErrorMessage(
                title = "No Poster Detected",
                message = "We couldn't find a poster in the camera view.",
                action = "Try better lighting and ensure the poster is flat and visible.",
                isRetryable = false,
                technicalDetails = "Error ${error.code}: ${error.message}"
            )
            
            is TalkingPhotoError.NoHumanFace -> ErrorMessage(
                title = "No Human Face",
                message = "This poster doesn't contain a human face.",
                action = "Please scan a poster with a human face (not products or mascots).",
                isRetryable = false,
                technicalDetails = "Error ${error.code}: ${error.message}"
            )
            
            is TalkingPhotoError.BackendUnavailable -> ErrorMessage(
                title = "Service Unavailable",
                message = "The video generation service is currently unavailable.",
                action = "Check your internet connection and try again in a moment.",
                isRetryable = true,
                technicalDetails = "Error ${error.code}: ${error.message}"
            )
            
            is TalkingPhotoError.GenerationFailed -> ErrorMessage(
                title = "Generation Failed",
                message = "Failed to generate the lip-sync video.",
                action = "Please try again. If the problem persists, try a different poster.",
                isRetryable = true,
                technicalDetails = "Error ${error.code}: ${error.message}" + 
                    if (error.videoId != null) " (Video ID: ${error.videoId})" else ""
            )
            
            is TalkingPhotoError.DownloadFailed -> ErrorMessage(
                title = "Download Failed",
                message = "Failed to download the video after multiple attempts.",
                action = "Check your internet connection and try again.",
                isRetryable = true,
                technicalDetails = "Error ${error.code}: ${error.message}\nURL: ${error.url}"
            )
            
            is TalkingPhotoError.InvalidCoordinates -> ErrorMessage(
                title = "Invalid Video Data",
                message = "The video data received is invalid.",
                action = "Try scanning the poster again.",
                isRetryable = false,
                technicalDetails = "Error ${error.code}: ${error.message}\nCoordinates: ${error.coordinates}"
            )
            
            is TalkingPhotoError.CacheCorrupted -> ErrorMessage(
                title = "Video Corrupted",
                message = "The cached video file is corrupted.",
                action = "The video will be re-downloaded automatically.",
                isRetryable = true,
                technicalDetails = "Error ${error.code}: ${error.message}\nPoster ID: ${error.posterId}"
            )
        }
    }
    
    /**
     * Gets a short user message for display in UI.
     */
    fun getShortMessage(error: TalkingPhotoError): String {
        return mapError(error).message
    }
    
    /**
     * Gets the suggested action for an error.
     */
    fun getSuggestedAction(error: TalkingPhotoError): String {
        return mapError(error).action
    }
    
    /**
     * Checks if an error is retryable.
     */
    fun isRetryable(error: TalkingPhotoError): Boolean {
        return mapError(error).isRetryable
    }
    
    /**
     * Gets technical details for logging/debugging.
     */
    fun getTechnicalDetails(error: TalkingPhotoError): String {
        return mapError(error).technicalDetails ?: "No technical details available"
    }
    
    /**
     * Formats a complete error report for logging.
     */
    fun formatErrorReport(
        error: TalkingPhotoError,
        component: String,
        context: Map<String, Any?>
    ): String {
        val errorMsg = mapError(error)
        val contextStr = context.entries.joinToString("\n") { "  ${it.key}: ${it.value}" }
        
        return """
            |=== Error Report ===
            |Component: $component
            |Error Code: ${error.code}
            |Title: ${errorMsg.title}
            |Message: ${errorMsg.message}
            |Action: ${errorMsg.action}
            |Retryable: ${errorMsg.isRetryable}
            |Technical Details: ${errorMsg.technicalDetails}
            |Context:
            |$contextStr
            |Timestamp: ${System.currentTimeMillis()}
            |==================
        """.trimMargin()
    }
}

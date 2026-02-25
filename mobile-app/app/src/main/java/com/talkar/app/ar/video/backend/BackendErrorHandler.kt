package com.talkar.app.ar.video.backend

import android.util.Log
import com.talkar.app.ar.video.errors.ErrorHandler
import com.talkar.app.ar.video.errors.TalkingPhotoError
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Error handling for backend communication operations.
 * 
 * Handles:
 * - Network errors (timeout, unreachable)
 * - HTTP errors (4xx, 5xx)
 * - API rate limiting
 * - Video generation failures
 * - Download failures
 * 
 * Requirements: 14.2, 14.3
 */
object BackendErrorHandler {
    
    private const val TAG = "BackendErrorHandler"
    
    /**
     * Handles backend communication errors.
     * 
     * Maps HTTP status codes and network exceptions to appropriate errors.
     * 
     * Requirement: 14.2
     */
    fun handleBackendError(
        exception: Exception,
        operation: String,
        context: Map<String, Any?> = emptyMap()
    ): TalkingPhotoError {
        val error = when (exception) {
            is UnknownHostException -> {
                TalkingPhotoError.BackendUnavailable(
                    "Cannot reach backend service. Check your internet connection."
                )
            }
            
            is SocketTimeoutException -> {
                TalkingPhotoError.BackendUnavailable(
                    "Backend request timed out. Service may be overloaded."
                )
            }
            
            is HttpException -> {
                when (exception.code()) {
                    429 -> {
                        // Rate limiting
                        val retryAfter = exception.response()?.headers()?.get("Retry-After")
                        TalkingPhotoError.BackendUnavailable(
                            "Rate limit exceeded. Please wait ${retryAfter ?: "a moment"} before trying again."
                        )
                    }
                    
                    in 500..599 -> {
                        // Server errors
                        TalkingPhotoError.BackendUnavailable(
                            "Backend service error (${exception.code()}). Please try again later."
                        )
                    }
                    
                    in 400..499 -> {
                        // Client errors
                        TalkingPhotoError.GenerationFailed(
                            "Invalid request (${exception.code()}): ${exception.message()}"
                        )
                    }
                    
                    else -> {
                        TalkingPhotoError.BackendUnavailable(
                            "Unexpected HTTP error: ${exception.code()}"
                        )
                    }
                }
            }
            
            is IOException -> {
                TalkingPhotoError.BackendUnavailable(
                    "Network I/O error: ${exception.message}"
                )
            }
            
            else -> {
                TalkingPhotoError.GenerationFailed(
                    "Unexpected error during $operation: ${exception.message}"
                )
            }
        }
        
        ErrorHandler.logError(
            error = error,
            component = "BackendVideoFetcher",
            context = context + mapOf("operation" to operation)
        )
        
        return error
    }
    
    /**
     * Handles video generation failures.
     * 
     * Requirement: 14.3
     */
    fun handleGenerationError(
        videoId: String,
        errorMessage: String?
    ): TalkingPhotoError {
        val error = TalkingPhotoError.GenerationFailed(
            message = errorMessage ?: "Video generation failed",
            videoId = videoId
        )
        
        ErrorHandler.logError(
            error = error,
            component = "BackendVideoFetcher",
            context = mapOf(
                "video_id" to videoId,
                "error_message" to errorMessage
            )
        )
        
        return error
    }
    
    /**
     * Handles video download failures with retry logic.
     * 
     * Requirement: 14.3
     */
    suspend fun downloadWithRetry(
        videoUrl: String,
        destinationPath: String,
        onProgress: (Float) -> Unit,
        downloadBlock: suspend (String, String, (Float) -> Unit) -> Result<String>
    ): Result<String> {
        return ErrorHandler.withRetry(maxRetries = 3) { attempt ->
            Log.d(TAG, "Download attempt $attempt for URL: $videoUrl")
            
            try {
                val result = downloadBlock(videoUrl, destinationPath, onProgress)
                
                if (result.isFailure) {
                    val exception = result.exceptionOrNull()
                    if (exception is IOException) {
                        Log.w(TAG, "Download failed (attempt $attempt): ${exception.message}")
                        throw exception // Will trigger retry
                    }
                }
                
                result
            } catch (e: Exception) {
                if (attempt == 3) {
                    // Last attempt failed
                    val error = TalkingPhotoError.DownloadFailed(
                        "Failed to download video after 3 attempts: ${e.message}",
                        url = videoUrl
                    )
                    ErrorHandler.logError(
                        error = error,
                        component = "BackendVideoFetcher",
                        context = mapOf(
                            "url" to videoUrl,
                            "destination" to destinationPath,
                            "attempts" to attempt
                        )
                    )
                    Result.failure(error)
                } else {
                    throw e // Will trigger retry
                }
            }
        }
    }
    
    /**
     * Validates lip coordinates from backend response.
     * 
     * Requirement: 4.5
     */
    fun validateCoordinates(
        lipX: Float,
        lipY: Float,
        lipWidth: Float,
        lipHeight: Float
    ): Result<Unit> {
        val isValid = lipX in 0f..1f &&
                     lipY in 0f..1f &&
                     lipWidth in 0f..1f &&
                     lipHeight in 0f..1f
        
        return if (isValid) {
            Result.success(Unit)
        } else {
            val error = TalkingPhotoError.InvalidCoordinates(
                "Lip coordinates out of range (0-1)",
                coordinates = "($lipX, $lipY, $lipWidth, $lipHeight)"
            )
            ErrorHandler.logError(
                error = error,
                component = "BackendVideoFetcher",
                context = mapOf(
                    "lipX" to lipX,
                    "lipY" to lipY,
                    "lipWidth" to lipWidth,
                    "lipHeight" to lipHeight
                )
            )
            Result.failure(error)
        }
    }
}

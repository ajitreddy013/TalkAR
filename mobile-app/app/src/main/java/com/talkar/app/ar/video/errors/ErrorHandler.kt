package com.talkar.app.ar.video.errors

import android.util.Log
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handling utilities for the talking photo feature.
 * 
 * Provides:
 * - Retry logic with exponential backoff
 * - Error logging with structured format
 * - Error classification and mapping
 * - User message generation
 * 
 * Requirements: 11.5, 14.1, 14.2, 14.3, 14.5
 */
object ErrorHandler {
    
    private const val TAG = "TalkingPhotoErrorHandler"
    
    /**
     * Executes a block with retry logic using exponential backoff.
     * 
     * Retries up to 3 times with delays: 1s, 2s, 4s
     * 
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialDelayMs Initial delay in milliseconds (default: 1000)
     * @param block The block to execute
     * @return Result of the block execution
     * 
     * Requirement: 11.5
     */
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        block: suspend (attempt: Int) -> Result<T>
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = block(attempt + 1)
                if (result.isSuccess) {
                    if (attempt > 0) {
                        Log.i(TAG, "Operation succeeded after ${attempt + 1} attempts")
                    }
                    return result
                }
                lastException = result.exceptionOrNull() as? Exception
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Attempt ${attempt + 1}/$maxRetries failed: ${e.message}", e)
            }
            
            if (attempt < maxRetries - 1) {
                Log.d(TAG, "Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay *= 2 // Exponential backoff
            }
        }
        
        Log.e(TAG, "All $maxRetries attempts failed", lastException)
        return Result.failure(lastException ?: Exception("Operation failed after $maxRetries attempts"))
    }
    
    /**
     * Logs an error with structured format.
     * 
     * Includes:
     * - Error code
     * - Timestamp
     * - Component name
     * - Context information
     * - Stack trace
     * 
     * Requirement: 14.1, 14.2, 14.3
     */
    fun logError(
        error: TalkingPhotoError,
        component: String,
        context: Map<String, Any?> = emptyMap()
    ) {
        val timestamp = System.currentTimeMillis()
        val contextStr = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
        
        Log.e(
            TAG,
            """
            |Error occurred in $component
            |Code: ${error.code}
            |Message: ${error.message}
            |Timestamp: $timestamp
            |Context: $contextStr
            """.trimMargin(),
            error
        )
    }
    
    /**
     * Classifies an exception into a TalkingPhotoError.
     * 
     * Maps common exceptions to appropriate error types.
     * 
     * Requirement: 14.2, 14.3
     */
    fun classifyException(
        exception: Exception,
        context: String = ""
    ): TalkingPhotoError {
        return when (exception) {
            is UnknownHostException, is SocketTimeoutException -> {
                TalkingPhotoError.BackendUnavailable(
                    "Network error: ${exception.message}"
                )
            }
            
            is IOException -> {
                TalkingPhotoError.DownloadFailed(
                    "I/O error: ${exception.message}",
                    url = context
                )
            }
            
            is TalkingPhotoError -> exception
            
            else -> {
                TalkingPhotoError.GenerationFailed(
                    "Unexpected error: ${exception.message}"
                )
            }
        }
    }
    
    /**
     * Determines if an error is retryable.
     * 
     * @param error The error to check
     * @return true if the error can be retried
     */
    fun isRetryable(error: TalkingPhotoError): Boolean {
        return when (error) {
            is TalkingPhotoError.BackendUnavailable -> true
            is TalkingPhotoError.DownloadFailed -> true
            is TalkingPhotoError.CacheCorrupted -> true
            is TalkingPhotoError.GenerationFailed -> true
            is TalkingPhotoError.PosterNotDetected -> false
            is TalkingPhotoError.NoHumanFace -> false
            is TalkingPhotoError.InvalidCoordinates -> false
        }
    }
    
    /**
     * Gets a user-friendly error message with actionable advice.
     * 
     * Requirement: 14.1, 14.2, 14.3, 14.5
     */
    fun getUserMessage(error: TalkingPhotoError): String {
        return error.getUserMessage()
    }
    
    /**
     * Gets suggested action for an error.
     * 
     * @param error The error
     * @return Suggested action text
     */
    fun getSuggestedAction(error: TalkingPhotoError): String {
        return when (error) {
            is TalkingPhotoError.PosterNotDetected -> 
                "Ensure the poster is well-lit and flat"
            
            is TalkingPhotoError.NoHumanFace -> 
                "Scan a different poster with a human face"
            
            is TalkingPhotoError.BackendUnavailable -> 
                "Check your internet connection and try again"
            
            is TalkingPhotoError.GenerationFailed -> 
                "Try again or scan a different poster"
            
            is TalkingPhotoError.DownloadFailed -> 
                "Check your internet connection"
            
            is TalkingPhotoError.InvalidCoordinates -> 
                "Try scanning the poster again"
            
            is TalkingPhotoError.CacheCorrupted -> 
                "Video will be re-downloaded automatically"
        }
    }
}

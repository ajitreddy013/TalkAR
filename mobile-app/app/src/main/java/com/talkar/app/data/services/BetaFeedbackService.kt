package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.BetaFeedbackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BetaFeedbackService {
    private val apiService = ApiClient.create()
    
    suspend fun submitFeedback(
        userId: String?,
        posterId: String,
        rating: Int,
        comment: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate inputs
            if (posterId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Poster ID cannot be blank"))
            }
            
            if (rating < 1 || rating > 5) {
                return@withContext Result.failure(IllegalArgumentException("Rating must be between 1 and 5"))
            }
            
            val request = BetaFeedbackRequest(
                user_id = userId,
                poster_id = posterId,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Submitting beta feedback for poster: $posterId with rating: $rating")
            
            val response = apiService.sendBetaFeedback(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val feedbackId = response.body()?.feedbackId ?: "unknown"
                Log.d(TAG, "Beta feedback submitted successfully: $feedbackId")
                Result.success(feedbackId)
            } else {
                val errorMsg = response.body()?.message ?: "Failed to submit feedback"
                val statusCode = response.code()
                Log.e(TAG, "Failed to submit beta feedback. Status: $statusCode, Message: $errorMsg")
                Result.failure(Exception("HTTP $statusCode: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting beta feedback", e)
            Result.failure(e)
        }
    }
    
    /**
     * Submit feedback with retry logic
     */
    suspend fun submitFeedbackWithRetry(
        userId: String?,
        posterId: String,
        rating: Int,
        comment: String?,
        maxRetries: Int = 3
    ): Result<String> {
        var lastError: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                val result = submitFeedback(userId, posterId, rating, comment)
                if (result.isSuccess) {
                    return result
                } else {
                    lastError = result.exceptionOrNull() as? Exception ?: Exception("Unknown error")
                    Log.w(TAG, "Feedback submission attempt $attempt failed: ${lastError.message}")
                    
                    // Wait before retrying (exponential backoff)
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay((1000 * attempt).toLong())
                    }
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Feedback submission attempt $attempt failed with exception", e)
                
                // Wait before retrying (exponential backoff)
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay((1000 * attempt).toLong())
                }
            }
        }
        
        return Result.failure(lastError ?: Exception("Failed to submit feedback after $maxRetries attempts"))
    }
    
    companion object {
        private const val TAG = "BetaFeedbackService"
    }
}
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
            val request = BetaFeedbackRequest(
                user_id = userId,
                poster_id = posterId,
                rating = rating,
                comment = comment,
                timestamp = System.currentTimeMillis()
            )
            
            val response = apiService.sendBetaFeedback(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                Log.d(TAG, "Beta feedback submitted successfully: ${response.body()?.feedbackId}")
                Result.success(response.body()?.feedbackId ?: "unknown")
            } else {
                val errorMsg = response.body()?.message ?: "Failed to submit feedback"
                Log.e(TAG, "Failed to submit beta feedback: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting beta feedback", e)
            Result.failure(e)
        }
    }
    
    companion object {
        private const val TAG = "BetaFeedbackService"
    }
}

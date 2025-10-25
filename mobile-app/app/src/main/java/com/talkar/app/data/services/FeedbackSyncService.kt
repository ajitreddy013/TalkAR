package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.TalkARApplication
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.FeedbackRequest
import com.talkar.app.data.local.ImageDatabase
import com.talkar.app.data.models.Feedback
import kotlinx.coroutines.flow.first

/**
 * Service for syncing local feedback with the backend
 */
class FeedbackSyncService private constructor() {
    
    private val tag = "FeedbackSyncService"
    private val apiClient = ApiClient.create()
    
    /**
     * Sync all unsynced feedback with the backend
     */
    suspend fun syncFeedback() {
        try {
            Log.d(tag, "Starting feedback sync...")
            
            // Get database instance
            val database = ImageDatabase.getDatabase(TalkARApplication.instance)
            
            // Get all unsynced feedback
            val unsyncedFeedback = database.feedbackDao().getUnsyncedFeedback().first()
            
            if (unsyncedFeedback.isEmpty()) {
                Log.d(tag, "No unsynced feedback found")
                return
            }
            
            Log.d(tag, "Found ${unsyncedFeedback.size} unsynced feedback items")
            
            // Sync each feedback item
            var syncedCount = 0
            for (feedback in unsyncedFeedback) {
                try {
                    // Create feedback request
                    val request = FeedbackRequest(
                        adContentId = feedback.adContentId,
                        productName = feedback.productName,
                        isPositive = feedback.isPositive,
                        timestamp = feedback.timestamp
                    )
                    
                    // Send feedback to backend
                    val response = apiClient.sendFeedback(request)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Mark feedback as synced
                        database.feedbackDao().markAsSynced(feedback.id)
                        syncedCount++
                        Log.d(tag, "Successfully synced feedback: ${feedback.id}")
                    } else {
                        Log.e(tag, "Failed to sync feedback: ${feedback.id}. Response: ${response.code()} ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error syncing feedback: ${feedback.id}", e)
                }
            }
            
            Log.d(tag, "Feedback sync completed. Synced $syncedCount/${unsyncedFeedback.size} items")
            
        } catch (e: Exception) {
            Log.e(tag, "Error during feedback sync", e)
        }
    }
    
    /**
     * Sync a single feedback item immediately
     */
    suspend fun syncFeedbackItem(feedback: Feedback): Boolean {
        return try {
            Log.d(tag, "Syncing single feedback item: ${feedback.id}")
            
            // Create feedback request
            val request = FeedbackRequest(
                adContentId = feedback.adContentId,
                productName = feedback.productName,
                isPositive = feedback.isPositive,
                timestamp = feedback.timestamp
            )
            
            // Send feedback to backend
            val response = apiClient.sendFeedback(request)
            
            if (response.isSuccessful && response.body()?.success == true) {
                // Mark feedback as synced
                val database = ImageDatabase.getDatabase(TalkARApplication.instance)
                database.feedbackDao().markAsSynced(feedback.id)
                Log.d(tag, "Successfully synced feedback item: ${feedback.id}")
                true
            } else {
                Log.e(tag, "Failed to sync feedback item: ${feedback.id}. Response: ${response.code()} ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing feedback item: ${feedback.id}", e)
            false
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: FeedbackSyncService? = null
        
        fun getInstance(): FeedbackSyncService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeedbackSyncService().also { INSTANCE = it }
            }
        }
    }
}
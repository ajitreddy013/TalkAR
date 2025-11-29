package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.talkar.app.data.local.ImageDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Service to periodically clean up old data from the database to reduce storage costs
 */
class DatabaseCleanupService(private val context: Context) {
    private val tag = "DatabaseCleanupService"
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Clean up old data from the database
     * - Delete feedback entries older than 30 days
     * - Delete scanned product entries older than 90 days
     */
    fun cleanupOldData() {
        coroutineScope.launch {
            try {
                val database = ImageDatabase.getDatabase(context)
                
                // Clean up old feedback entries (older than 30 days)
                val feedbackThreshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
                val deletedFeedbackCount = database.feedbackDao().deleteOldFeedback(feedbackThreshold)
                Log.d(tag, "Deleted $deletedFeedbackCount old feedback entries")
                
                // Clean up old scanned product entries (older than 90 days)
                val productThreshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90)
                val deletedProductCount = database.scannedProductDao().deleteOldProducts(productThreshold)
                Log.d(tag, "Deleted $deletedProductCount old scanned product entries")
                
                Log.d(tag, "Database cleanup completed successfully")
            } catch (e: Exception) {
                Log.e(tag, "Error during database cleanup", e)
            }
        }
    }
    
    /**
     * Schedule periodic cleanup (should be called once at app startup)
     */
    fun schedulePeriodicCleanup() {
        // Run cleanup immediately
        cleanupOldData()
        
        // TODO: Implement periodic scheduling using WorkManager or AlarmManager
        // For now, cleanup will be triggered manually or at app startup
    }
}
package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.data.api.FeedbackRequest

/**
 * Test class to demonstrate the feedback capture functionality
 */
class FeedbackCaptureTest {
    
    private val tag = "FeedbackCaptureTest"
    
    /**
     * Test the FeedbackRequest structure
     */
    fun testFeedbackRequestStructure() {
        Log.d(tag, "Testing FeedbackRequest structure")
        
        // Create a feedback request
        val request = FeedbackRequest(
            adContentId = "product123",
            productName = "Coca-Cola",
            isPositive = true,
            timestamp = System.currentTimeMillis()
        )
        
        // Log the request details
        Log.d(tag, "Ad Content ID: ${request.adContentId}")
        Log.d(tag, "Product Name: ${request.productName}")
        Log.d(tag, "Is Positive: ${request.isPositive}")
        Log.d(tag, "Timestamp: ${request.timestamp}")
        
        // Verify the request structure
        assert(request.adContentId == "product123") { "Ad Content ID should be product123" }
        assert(request.productName == "Coca-Cola") { "Product name should be Coca-Cola" }
        assert(request.isPositive) { "Feedback should be positive" }
        
        Log.d(tag, "FeedbackRequest test passed!")
    }
    
    /**
     * Test the Feedback entity structure
     */
    fun testFeedbackEntityStructure() {
        Log.d(tag, "Testing Feedback entity structure")
        
        // Create a feedback entity
        val feedback = com.talkar.app.data.models.Feedback(
            id = "feedback123",
            adContentId = "product123",
            productName = "Coca-Cola",
            isPositive = false,
            timestamp = System.currentTimeMillis(),
            synced = false
        )
        
        // Log the feedback details
        Log.d(tag, "Feedback ID: ${feedback.id}")
        Log.d(tag, "Ad Content ID: ${feedback.adContentId}")
        Log.d(tag, "Product Name: ${feedback.productName}")
        Log.d(tag, "Is Positive: ${feedback.isPositive}")
        Log.d(tag, "Timestamp: ${feedback.timestamp}")
        Log.d(tag, "Synced: ${feedback.synced}")
        
        // Verify the feedback structure
        assert(feedback.id == "feedback123") { "Feedback ID should be feedback123" }
        assert(feedback.adContentId == "product123") { "Ad Content ID should be product123" }
        assert(feedback.productName == "Coca-Cola") { "Product name should be Coca-Cola" }
        assert(!feedback.isPositive) { "Feedback should be negative" }
        assert(!feedback.synced) { "Feedback should not be synced" }
        
        Log.d(tag, "Feedback entity test passed!")
    }
}
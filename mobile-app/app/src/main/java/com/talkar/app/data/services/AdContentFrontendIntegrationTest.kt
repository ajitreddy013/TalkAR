package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.data.api.AdContentGenerationRequest
import com.talkar.app.data.models.AdContent
import kotlinx.coroutines.runBlocking

/**
 * Frontend integration test for Ad Content Generation functionality
 * This test can be run directly on a device to verify the complete workflow
 */
class AdContentFrontendIntegrationTest {
    
    private val tag = "AdContentFrontendIntegrationTest"
    
    /**
     * Test the complete ad content generation workflow
     * This simulates what happens in the frontend when an image is detected
     */
    fun testCompleteAdContentWorkflow() {
        Log.d(tag, "Starting complete ad content workflow test")
        
        // Run in background thread to avoid blocking UI
        Thread {
            try {
                runBlocking {
                    // Initialize services
                    val adContentService = AdContentGenerationService.getInstance()
                    
                    // Simulate image detection - in real app, this would come from AR detection
                    val detectedImageId = "poster_01"
                    val detectedProductName = "Sunrich Water Bottle"
                    
                    Log.d(tag, "Image detected: $detectedImageId, Product: $detectedProductName")
                    
                    // Extract image ID (in real app, this would be from the detected image)
                    val imageId = detectedImageId
                    
                    // Send request to /generate_ad_content
                    Log.d(tag, "Sending request to generate ad content for product: $detectedProductName")
                    val result = adContentService.generateAdContent(detectedProductName)
                    
                    if (result.isSuccess) {
                        val response = result.getOrNull()!!
                        Log.d(tag, "Ad content generated successfully!")
                        
                        // Receive response (script, audio/video URL)
                        val adContent = AdContent(
                            script = response.script ?: "No script available",
                            audioUrl = response.audio_url,
                            videoUrl = response.video_url,
                            productName = detectedProductName
                        )
                        
                        Log.d(tag, "Ad Content Details:")
                        Log.d(tag, "  Script: ${adContent.script}")
                        Log.d(tag, "  Audio URL: ${adContent.audioUrl}")
                        Log.d(tag, "  Video URL: ${adContent.videoUrl}")
                        Log.d(tag, "  Product Name: ${adContent.productName}")
                        
                        // Display overlay (in real app, this would update the UI)
                        displayAdContentOverlay(adContent)
                        
                        Log.d(tag, "Complete ad content workflow test finished successfully!")
                    } else {
                        val error = result.exceptionOrNull()
                        Log.e(tag, "Failed to generate ad content: ${error?.message}")
                        Log.e(tag, "Complete ad content workflow test failed!")
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception in ad content workflow test: ${e.message}", e)
            }
        }.start()
    }
    
    /**
     * Simulate displaying the ad content overlay
     * In a real app, this would update the UI components
     */
    private fun displayAdContentOverlay(adContent: AdContent) {
        Log.d(tag, "Displaying ad content overlay:")
        Log.d(tag, "  🎯 Overlay displayed with ad content for ${adContent.productName}")
        Log.d(tag, "  📜 Script: ${adContent.script}")
        Log.d(tag, "  🔊 Audio URL: ${adContent.audioUrl}")
        Log.d(tag, "  🎥 Video URL: ${adContent.videoUrl}")
        Log.d(tag, "  🕐 Timestamp: ${adContent.timestamp}")
        
        // In a real implementation, this would:
        // 1. Update UI state to show the avatar overlay
        // 2. Display the ad script in a card or text view
        // 3. Prepare audio/video playback if URLs are available
        // 4. Handle user interactions (tap to replay, etc.)
    }
    
    /**
     * Test multiple product scenarios
     */
    fun testMultipleProducts() {
        Log.d(tag, "Starting multiple products test")
        
        Thread {
            try {
                runBlocking {
                    val adContentService = AdContentGenerationService.getInstance()
                    
                    val testProducts = listOf(
                        "Eco-Friendly Backpack" to "poster_02",
                        "Professional Wireless Headphones" to "poster_03",
                        "Smart Fitness Watch" to "poster_04"
                    )
                    
                    for ((productName, imageId) in testProducts) {
                        Log.d(tag, "Testing product: $productName")
                        
                        val result = adContentService.generateAdContent(productName)
                        
                        if (result.isSuccess) {
                            val response = result.getOrNull()!!
                            val adContent = AdContent(
                                script = response.script ?: "No script available",
                                audioUrl = response.audio_url,
                                videoUrl = response.video_url,
                                productName = productName
                            )
                            
                            Log.d(tag, "✓ Successfully generated ad content for $productName")
                            displayAdContentOverlay(adContent)
                        } else {
                            Log.e(tag, "✗ Failed to generate ad content for $productName")
                        }
                        
                        // Small delay between requests
                        Thread.sleep(1000)
                    }
                    
                    Log.d(tag, "Multiple products test completed!")
                }
            } catch (e: Exception) {
                Log.e(tag, "Exception in multiple products test: ${e.message}", e)
            }
        }.start()
    }
}
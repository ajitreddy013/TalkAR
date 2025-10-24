package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.AdContentGenerationRequest
import com.talkar.app.data.api.AdContentGenerationResponse

/**
 * Service for generating ad content for products
 */
class AdContentGenerationService {
    
    private val tag = "AdContentGenerationService"
    private val apiClient = ApiClient.create()
    
    /**
     * Generate complete ad content for a product
     */
    suspend fun generateAdContent(productName: String): Result<AdContentGenerationResponse> {
        return try {
            Log.d(tag, "Generating ad content for product: $productName")
            
            // Create request
            val request = AdContentGenerationRequest(
                product = productName
            )
            
            // Make API call
            val response = apiClient.generateAdContent(request)
            
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                if (result.success) {
                    Log.d(tag, "Ad content generated successfully")
                    Result.success(result)
                } else {
                    Log.e(tag, "Ad content generation failed")
                    Result.failure(Exception("Ad content generation failed"))
                }
            } else {
                Log.e(tag, "Ad content generation failed: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to generate ad content: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error generating ad content for product: $productName", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate complete ad content for a product with streaming optimization
     * This allows for partial playback as audio is being generated
     */
    suspend fun generateAdContentStreaming(productName: String): Result<AdContentGenerationResponse> {
        return try {
            Log.d(tag, "Generating streaming ad content for product: $productName")
            
            // Create request
            val request = AdContentGenerationRequest(
                product = productName
            )
            
            // Make API call to streaming endpoint
            val response = apiClient.generateAdContentStreaming(request)
            
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                if (result.success) {
                    Log.d(tag, "Streaming ad content generated successfully")
                    Result.success(result)
                } else {
                    Log.e(tag, "Streaming ad content generation failed")
                    Result.failure(Exception("Streaming ad content generation failed"))
                }
            } else {
                Log.e(tag, "Streaming ad content generation failed: ${response.code()} ${response.message()}")
                Result.failure(Exception("Failed to generate streaming ad content: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error generating streaming ad content for product: $productName", e)
            Result.failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AdContentGenerationService? = null
        
        fun getInstance(): AdContentGenerationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdContentGenerationService().also { INSTANCE = it }
            }
        }
    }
}
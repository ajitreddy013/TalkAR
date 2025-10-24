package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.AdContentGenerationRequest
import com.talkar.app.data.api.AdContentGenerationResponse

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
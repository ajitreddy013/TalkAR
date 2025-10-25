package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.TalkARApplication
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.AdContentGenerationRequest
import com.talkar.app.data.api.AdContentGenerationResponse
import com.talkar.app.data.local.ImageDatabase
import kotlinx.coroutines.flow.first

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
            
            // Get recent products from database
            val database = ImageDatabase.getDatabase(TalkARApplication.instance)
            val recentProducts = database.scannedProductDao().getRecentProducts().first()
            val previousProductNames = recentProducts.map { it.name }
            
            // Create request with previous products context
            val request = AdContentGenerationRequest(
                product = productName,
                previous_products = previousProductNames.ifEmpty { null }
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
            
            // Get recent products from database
            val database = ImageDatabase.getDatabase(TalkARApplication.instance)
            val recentProducts = database.scannedProductDao().getRecentProducts().first()
            val previousProductNames = recentProducts.map { it.name }
            
            // Create request with previous products context
            val request = AdContentGenerationRequest(
                product = productName,
                previous_products = previousProductNames.ifEmpty { null }
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
    
    /**
     * Save a scanned product to the database
     */
    suspend fun saveScannedProduct(productId: String, productName: String) {
        try {
            val database = ImageDatabase.getDatabase(TalkARApplication.instance)
            val scannedProduct = com.talkar.app.data.models.ScannedProduct(
                id = productId,
                name = productName,
                scannedAt = System.currentTimeMillis()
            )
            database.scannedProductDao().insert(scannedProduct)
            Log.d(tag, "Saved scanned product: $productName")
        } catch (e: Exception) {
            Log.e(tag, "Error saving scanned product: $productName", e)
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
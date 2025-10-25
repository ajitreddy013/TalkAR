package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.data.api.AdContentGenerationRequest

/**
 * Test class to demonstrate the short-term memory functionality
 */
class ShortTermMemoryTest {
    
    private val tag = "ShortTermMemoryTest"
    
    /**
     * Test the AdContentGenerationRequest with previous products
     */
    fun testAdContentGenerationRequestWithPreviousProducts() {
        Log.d(tag, "Testing AdContentGenerationRequest with previous products")
        
        // Create a request with previous products
        val request = AdContentGenerationRequest(
            product = "Pepsi",
            previous_products = listOf("Coca-Cola", "Sprite")
        )
        
        // Log the request details
        Log.d(tag, "Product: ${request.product}")
        Log.d(tag, "Previous products: ${request.previous_products}")
        
        // Verify the request structure
        assert(request.product == "Pepsi") { "Product should be Pepsi" }
        assert(request.previous_products?.size == 2) { "Should have 2 previous products" }
        assert(request.previous_products?.contains("Coca-Cola") == true) { "Should contain Coca-Cola" }
        assert(request.previous_products?.contains("Sprite") == true) { "Should contain Sprite" }
        
        Log.d(tag, "AdContentGenerationRequest test passed!")
    }
    
    /**
     * Test the AdContentGenerationRequest without previous products
     */
    fun testAdContentGenerationRequestWithoutPreviousProducts() {
        Log.d(tag, "Testing AdContentGenerationRequest without previous products")
        
        // Create a request without previous products
        val request = AdContentGenerationRequest(
            product = "Pepsi"
        )
        
        // Log the request details
        Log.d(tag, "Product: ${request.product}")
        Log.d(tag, "Previous products: ${request.previous_products}")
        
        // Verify the request structure
        assert(request.product == "Pepsi") { "Product should be Pepsi" }
        assert(request.previous_products == null) { "Should have no previous products" }
        
        Log.d(tag, "AdContentGenerationRequest without previous products test passed!")
    }
}
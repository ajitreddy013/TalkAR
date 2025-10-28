package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service to handle dynamic script generation and poster-based ad content creation
 */
class DynamicScriptService(private val context: Context) {
    
    private val tag = "DynamicScriptService"
    private val apiService = ApiClient.create()
    
    // State management
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _lastGeneratedScript = MutableStateFlow<DynamicScriptResponse?>(null)
    val lastGeneratedScript: StateFlow<DynamicScriptResponse?> = _lastGeneratedScript.asStateFlow()
    
    private val _lastAdContent = MutableStateFlow<PosterAdContentResponse?>(null)
    val lastAdContent: StateFlow<PosterAdContentResponse?> = _lastAdContent.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Generate dynamic script for a poster
     */
    suspend fun generateDynamicScript(imageId: String, userId: String? = null): DynamicScriptResponse? {
        return withContext(Dispatchers.IO) {
            try {
                _isGenerating.value = true
                _error.value = null
                
                Log.d(tag, "Generating dynamic script for image: $imageId")
                
                val request = DynamicScriptRequest(
                    image_id = imageId,
                    user_id = userId
                )
                
                val response = apiService.generateDynamicScript(request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val scriptResponse = response.body()!!
                    _lastGeneratedScript.value = scriptResponse
                    
                    Log.d(tag, "Successfully generated script: ${scriptResponse.script?.substring(0, 50)}...")
                    return@withContext scriptResponse
                } else {
                    val errorMsg = "Failed to generate dynamic script: ${response.code()} ${response.message()}"
                    Log.e(tag, errorMsg)
                    _error.value = errorMsg
                    return@withContext null
                }
                
            } catch (e: Exception) {
                val errorMsg = "Error generating dynamic script: ${e.message}"
                Log.e(tag, errorMsg, e)
                _error.value = errorMsg
                return@withContext null
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * Generate complete ad content from poster (script + audio + video)
     */
    suspend fun generateAdContentFromPoster(imageId: String, userId: String? = null): PosterAdContentResponse? {
        return withContext(Dispatchers.IO) {
            try {
                _isGenerating.value = true
                _error.value = null
                
                Log.d(tag, "Generating complete ad content for poster: $imageId")
                
                val request = PosterAdContentRequest(
                    image_id = imageId,
                    user_id = userId
                )
                
                val response = apiService.generateAdContentFromPoster(request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val adContentResponse = response.body()!!
                    _lastAdContent.value = adContentResponse
                    
                    Log.d(tag, "Successfully generated ad content - Script: ${adContentResponse.script?.substring(0, 50)}..., Video: ${adContentResponse.video_url}")
                    return@withContext adContentResponse
                } else {
                    val errorMsg = "Failed to generate ad content from poster: ${response.code()} ${response.message()}"
                    Log.e(tag, errorMsg)
                    _error.value = errorMsg
                    return@withContext null
                }
                
            } catch (e: Exception) {
                val errorMsg = "Error generating ad content from poster: ${e.message}"
                Log.e(tag, errorMsg, e)
                _error.value = errorMsg
                return@withContext null
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * Get all available posters
     */
    suspend fun getAllPosters(): List<PosterInfo>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Fetching all available posters")
                
                val response = apiService.getAllPosters()
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val posters = response.body()!!.posters
                    Log.d(tag, "Successfully fetched ${posters.size} posters")
                    return@withContext posters
                } else {
                    val errorMsg = "Failed to fetch posters: ${response.code()} ${response.message()}"
                    Log.e(tag, errorMsg)
                    _error.value = errorMsg
                    return@withContext null
                }
                
            } catch (e: Exception) {
                val errorMsg = "Error fetching posters: ${e.message}"
                Log.e(tag, errorMsg, e)
                _error.value = errorMsg
                return@withContext null
            }
        }
    }
    
    /**
     * Get specific poster details by image ID
     */
    suspend fun getPosterById(imageId: String): PosterDetails? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Fetching poster details for: $imageId")
                
                val response = apiService.getPosterById(imageId)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val poster = response.body()!!.poster
                    Log.d(tag, "Successfully fetched poster: ${poster.product_name}")
                    return@withContext poster
                } else {
                    val errorMsg = "Failed to fetch poster: ${response.code()} ${response.message()}"
                    Log.e(tag, errorMsg)
                    _error.value = errorMsg
                    return@withContext null
                }
                
            } catch (e: Exception) {
                val errorMsg = "Error fetching poster: ${e.message}"
                Log.e(tag, errorMsg, e)
                _error.value = errorMsg
                return@withContext null
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear last generated content
     */
    fun clearLastGenerated() {
        _lastGeneratedScript.value = null
        _lastAdContent.value = null
    }
}

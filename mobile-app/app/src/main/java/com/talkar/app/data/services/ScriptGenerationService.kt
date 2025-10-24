package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.TalkARApplication
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.ScriptGenerationRequest
import com.talkar.app.data.api.ScriptGenerationResponse
import com.talkar.app.data.models.UserPreferences

class ScriptGenerationService {
    
    private val tag = "ScriptGenerationService"
    private val apiClient = ApiClient.create()
    
    /**
     * Generate script with user preferences
     */
    suspend fun generateScriptWithPreferences(
        imageId: String,
        language: String? = null,
        emotion: String? = null
    ): Result<ScriptGenerationResponse> {
        return try {
            // Load user preferences
            val userPreferences = TalkARApplication.instance.userPreferencesService.loadUserPreferences()
            
            // Create request with user preferences
            val request = ScriptGenerationRequest(
                imageId = imageId,
                language = language,
                emotion = emotion,
                userPreferences = com.talkar.app.data.api.UserPreferences(
                    language = userPreferences.language,
                    preferredTone = userPreferences.preferredTone
                )
            )
            
            Log.d(tag, "Generating script with user preferences: $request")
            
            // Make API call
            val response = apiClient.generateScript(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to generate script: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error generating script", e)
            Result.failure(e)
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ScriptGenerationService? = null
        
        fun getInstance(): ScriptGenerationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ScriptGenerationService().also { INSTANCE = it }
            }
        }
    }
}
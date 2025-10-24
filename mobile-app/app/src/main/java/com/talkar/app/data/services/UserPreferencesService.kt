package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.talkar.app.data.models.UserPreferences
import java.io.InputStream
import java.io.InputStreamReader

class UserPreferencesService(private val context: Context) {
    
    private val tag = "UserPreferencesService"
    private val gson = Gson()
    
    /**
     * Load user preferences from assets
     */
    fun loadUserPreferences(): UserPreferences {
        return try {
            val inputStream: InputStream = context.assets.open("user_preferences.json")
            val reader = InputStreamReader(inputStream)
            val preferences = gson.fromJson(reader, UserPreferences::class.java)
            reader.close()
            inputStream.close()
            
            Log.d(tag, "User preferences loaded successfully: $preferences")
            preferences
        } catch (e: Exception) {
            Log.e(tag, "Failed to load user preferences, using defaults", e)
            // Return default preferences if loading fails
            UserPreferences()
        }
    }
    
    /**
     * Get user preferences as a map for API requests
     */
    fun getUserPreferencesAsMap(): Map<String, String> {
        val preferences = loadUserPreferences()
        return mapOf(
            "language" to preferences.language,
            "preferred_tone" to preferences.preferredTone
        )
    }
    
    companion object {
        @Volatile
        private var INSTANCE: UserPreferencesService? = null
        
        fun getInstance(context: Context): UserPreferencesService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
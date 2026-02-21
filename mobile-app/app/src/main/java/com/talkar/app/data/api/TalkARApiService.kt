package com.talkar.app.data.api

import android.net.Uri
import com.talkar.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * API service for TalkAR backend communication.
 * 
 * Handles all network requests to the backend including:
 * - Getting initial video for detected image
 * - Sending speech query and getting response
 * - Fetching image metadata
 */
class TalkARApiService {
    
    companion object {
        private const val TAG = "TalkARApiService"
        
        // API Configuration from BuildConfig
        private val API_PROTOCOL = BuildConfig.API_PROTOCOL
        private val API_HOST = BuildConfig.API_HOST
        private val API_PORT = BuildConfig.API_PORT
        private val API_VERSION = BuildConfig.API_VERSION
        
        // Build base URL
        private val BASE_URL = if (API_PORT == 443 || API_PORT == 80) {
            "$API_PROTOCOL://$API_HOST/api/$API_VERSION"
        } else {
            "$API_PROTOCOL://$API_HOST:$API_PORT/api/$API_VERSION"
        }
        
        private const val TIMEOUT_MS = 30000 // 30 seconds
    }
    
    /**
     * Get initial talking head video for a detected image.
     * 
     * @param imageName Name of the detected image (e.g., "sunrich", "tony")
     * @param language Language code (e.g., "en", "es")
     * @param emotion Emotion for the video (e.g., "excited", "happy")
     * @return VideoResponse with video URL and metadata
     */
    suspend fun getInitialVideo(
        imageName: String,
        language: String = "en",
        emotion: String = "excited"
    ): VideoResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/sync/talking-head/$imageName?language=$language&emotion=$emotion")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseVideoResponse(response)
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw ApiException("Failed to get video: $error", responseCode)
            }
        } catch (e: Exception) {
            throw ApiException("Network error: ${e.message}", 0, e)
        }
    }
    
    /**
     * Send voice query and get conversational response.
     * 
     * @param query User's speech text
     * @return VoiceQueryResponse with response text, audio URL, and emotion
     */
    suspend fun sendVoiceQuery(query: String): VoiceQueryResponse = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/ai-pipeline/voice_query")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            
            // Send request body
            val requestBody = JSONObject().apply {
                put("query", query)
            }
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseVoiceQueryResponse(response)
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw ApiException("Failed to process voice query: $error", responseCode)
            }
        } catch (e: Exception) {
            throw ApiException("Network error: ${e.message}", 0, e)
        }
    }
    
    /**
     * Get image metadata by name.
     * 
     * @param imageName Name of the image
     * @return ImageMetadata with image details
     */
    suspend fun getImageMetadata(imageName: String): ImageMetadata = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/images")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Content-Type", "application/json")
            }
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseImageMetadata(response, imageName)
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw ApiException("Failed to get image metadata: $error", responseCode)
            }
        } catch (e: Exception) {
            throw ApiException("Network error: ${e.message}", 0, e)
        }
    }
    
    /**
     * Parse video response from JSON.
     */
    private fun parseVideoResponse(json: String): VideoResponse {
        val obj = JSONObject(json)
        return VideoResponse(
            videoUrl = obj.optString("videoUrl", ""),
            duration = obj.optInt("duration", 0),
            emotion = obj.optString("emotion", "neutral"),
            script = obj.optString("script", "")
        )
    }
    
    /**
     * Parse voice query response from JSON.
     */
    private fun parseVoiceQueryResponse(json: String): VoiceQueryResponse {
        val obj = JSONObject(json)
        return VoiceQueryResponse(
            success = obj.optBoolean("success", false),
            response = obj.optString("response", ""),
            audioUrl = obj.optString("audioUrl", ""),
            emotion = obj.optString("emotion", "neutral")
        )
    }
    
    /**
     * Parse image metadata from JSON array.
     */
    private fun parseImageMetadata(json: String, imageName: String): ImageMetadata {
        val array = org.json.JSONArray(json)
        
        // Find image by name (case-insensitive)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("name", "")
            
            if (name.equals(imageName, ignoreCase = true)) {
                return ImageMetadata(
                    id = obj.optString("id", ""),
                    name = name,
                    description = obj.optString("description", ""),
                    imageUrl = obj.optString("imageUrl", ""),
                    thumbnailUrl = obj.optString("thumbnailUrl", ""),
                    script = obj.optString("script", "")
                )
            }
        }
        
        // If not found, return default
        return ImageMetadata(
            id = "",
            name = imageName,
            description = "",
            imageUrl = "",
            thumbnailUrl = "",
            script = ""
        )
    }
}

/**
 * Response from getting initial video.
 */
data class VideoResponse(
    val videoUrl: String,
    val duration: Int,
    val emotion: String,
    val script: String
)

/**
 * Response from voice query.
 */
data class VoiceQueryResponse(
    val success: Boolean,
    val response: String,
    val audioUrl: String,
    val emotion: String
)

/**
 * Image metadata.
 */
data class ImageMetadata(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val script: String
)

/**
 * API exception with HTTP status code.
 */
class ApiException(
    message: String,
    val statusCode: Int,
    cause: Throwable? = null
) : Exception(message, cause)

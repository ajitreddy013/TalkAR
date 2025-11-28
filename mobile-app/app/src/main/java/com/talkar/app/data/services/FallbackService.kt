package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.talkar.app.data.models.*
import com.talkar.app.data.local.ImageDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service that provides fallback functionality when API calls fail
 */
class FallbackService(private val context: Context) {
    private val tag = "FallbackService"
    
    /**
     * Get images from local database as fallback
     */
    suspend fun getImagesFallback(): List<BackendImage> {
        return withContext(Dispatchers.IO) {
            try {
                val dao = ImageDatabase.getDatabase(context).imageDao()
                val localImagesFlow = dao.getAllImages()
                val localImages = mutableListOf<ImageRecognition>()
                localImagesFlow.collect { images ->
                    localImages.addAll(images)
                }
                
                localImages.map { entity ->
                    BackendImage(
                        id = entity.id,
                        name = entity.name,
                        description = entity.description ?: "",
                        imageUrl = entity.imageUrl,
                        thumbnailUrl = "",
                        isActive = true,
                        createdAt = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis().toString(),
                        dialogues = entity.dialogues
                    )
                }.toList()
            } catch (e: Exception) {
                Log.e(tag, "Failed to get images from local database", e)
                emptyList()
            }
        }
    }
    
    /**
     * Get avatar for image as fallback
     */
    suspend fun getAvatarForImageFallback(imageId: String): Avatar? {
        return withContext(Dispatchers.IO) {
            try {
                // Return a default avatar as fallback
                Avatar(
                    id = "default_avatar_$imageId",
                    name = "Default Avatar",
                    avatarImageUrl = "", // Empty URL for default avatar
                    description = "Default avatar for fallback",
                    voiceId = "default_voice",
                    avatarVideoUrl = null,
                    isActive = true
                )
            } catch (e: Exception) {
                Log.e(tag, "Failed to get default avatar", e)
                null
            }
        }
    }
    
    /**
     * Generate fallback script when API fails
     */
    fun generateFallbackScript(imageId: String): String {
        return when (imageId) {
            "product_1" -> "Welcome to our amazing product! This innovative solution offers great value and quality."
            "product_2" -> "Discover the power of our latest offering. Experience excellence with every use."
            "product_3" -> "Introducing our premium selection. Crafted for your satisfaction and convenience."
            else -> "Thank you for your interest in our product. We're excited to share more with you soon."
        }
    }
    
    /**
     * Get fallback audio URL when API fails
     */
    fun getFallbackAudioUrl(imageId: String): String {
        // In a real implementation, this would return a URL to a pre-recorded fallback audio
        return "https://example.com/fallback-audio-$imageId.mp3"
    }
    
    /**
     * Get fallback video URL when API fails
     */
    fun getFallbackVideoUrl(imageId: String): String {
        // In a real implementation, this would return a URL to a pre-recorded fallback video
        return "https://example.com/fallback-video-$imageId.mp3"
    }
}
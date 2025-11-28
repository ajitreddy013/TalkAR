package com.talkar.app.data.repository

import android.content.Context
import com.talkar.app.data.api.ApiService
import com.talkar.app.data.local.ImageDatabase
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.TalkingHeadVideo
import com.talkar.app.data.services.FallbackService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ImageRepository(
    private val apiClient: ApiService,
    private val database: ImageDatabase,
    private val context: Context
) {
    
    private val fallbackService = FallbackService(context)
    
    fun getAllImages(): Flow<List<ImageRecognition>> = flow {
        try {
            android.util.Log.d("ImageRepository", "Fetching images from API...")
            // First try to get from API
            val response = apiClient.getImages()
            android.util.Log.d("ImageRepository", "API response: ${response.code()}")
            
            if (response.isSuccessful) {
                val backendImages = response.body() ?: emptyList()
                android.util.Log.d("ImageRepository", "Loaded ${backendImages.size} images from API")
                
                // Convert BackendImage to ImageRecognition for local storage
                val imageRecognitions = backendImages.map { backendImage ->
                    ImageRecognition(
                        id = backendImage.id,
                        imageUrl = backendImage.imageUrl,
                        name = backendImage.name,
                        description = backendImage.description,
                        dialogues = backendImage.dialogues,
                        createdAt = System.currentTimeMillis().toString(),
                        updatedAt = System.currentTimeMillis().toString()
                    )
                }
                
                // Cache the images locally
                imageRecognitions.forEach { image ->
                    database.imageDao().insert(image)
                }
                emit(imageRecognitions)
            } else {
                android.util.Log.e("ImageRepository", "API failed: ${response.code()}")
                // Fallback to local cache if API fails
                database.imageDao().getAllImages().collect { localImages ->
                    emit(localImages)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching images", e)
            // Fallback to local cache on error
            database.imageDao().getAllImages().collect { localImages ->
                emit(localImages)
            }
        }
    }
    
    fun getImageById(id: String): Flow<ImageRecognition?> {
        return try {
            database.imageDao().getImageById(id)
        } catch (e: Exception) {
            flow { emit(null) }
        }
    }
    
    fun searchImages(query: String): Flow<List<ImageRecognition>> {
        return try {
            database.imageDao().searchImages("%$query%")
        } catch (e: Exception) {
            flow { emit(emptyList()) }
        }
    }
    
    /**
     * Get all avatars from API with fallback
     */
    suspend fun getAvatars(): Result<List<Avatar>> {
        return try {
            android.util.Log.d("ImageRepository", "Fetching avatars from API...")
            val response = apiClient.getAvatars()
            android.util.Log.d("ImageRepository", "Avatars API response: ${response.code()}")
            
            if (response.isSuccessful) {
                val avatars = response.body() ?: emptyList()
                android.util.Log.d("ImageRepository", "Loaded ${avatars.size} avatars from API")
                Result.success(avatars)
            } else {
                android.util.Log.e("ImageRepository", "Avatars API failed: ${response.code()}")
                // Try fallback
                val fallbackAvatar = fallbackService.getAvatarForImageFallback("default")
                val fallbackAvatars = if (fallbackAvatar != null) {
                    listOf(fallbackAvatar)
                } else {
                    listOf(Avatar(
                        id = "fallback_avatar",
                        name = "Fallback Avatar",
                        avatarImageUrl = "",
                        description = "Fallback avatar when API is unavailable",
                        voiceId = "default_voice",
                        avatarVideoUrl = null,
                        isActive = true
                    ))
                }
                Result.success(fallbackAvatars)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching avatars", e)
            // Try fallback
            val fallbackAvatar = fallbackService.getAvatarForImageFallback("default")
            val fallbackAvatars = if (fallbackAvatar != null) {
                listOf(fallbackAvatar)
            } else {
                listOf(Avatar(
                    id = "fallback_avatar",
                    name = "Fallback Avatar",
                    avatarImageUrl = "",
                    description = "Fallback avatar when API is unavailable",
                    voiceId = "default_voice",
                    avatarVideoUrl = null,
                    isActive = true
                ))
            }
            Result.success(fallbackAvatars)
        }
    }
    
    /**
     * Get avatar for specific image with fallback
     */
    suspend fun getAvatarForImage(imageId: String): Result<Avatar?> {
        return try {
            android.util.Log.d("ImageRepository", "Fetching avatar for image: $imageId")
            val response = apiClient.getAvatarForImage(imageId)
            android.util.Log.d("ImageRepository", "Avatar for image API response: ${response.code()}")
            
            if (response.isSuccessful) {
                val avatar = response.body()
                android.util.Log.d("ImageRepository", "Loaded avatar for image: ${avatar?.name}")
                Result.success(avatar)
            } else {
                android.util.Log.e("ImageRepository", "Avatar for image API failed: ${response.code()}")
                // Try fallback
                val fallbackAvatar = fallbackService.getAvatarForImageFallback(imageId)
                Result.success(fallbackAvatar)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching avatar for image", e)
            // Try fallback
            val fallbackAvatar = fallbackService.getAvatarForImageFallback(imageId)
            Result.success(fallbackAvatar)
        }
    }
    
    /**
     * Get complete image data with avatar with fallback
     */
    suspend fun getCompleteImageData(imageId: String): Result<Pair<BackendImage, Avatar?>?> {
        return try {
            android.util.Log.d("ImageRepository", "Fetching complete data for image: $imageId")
            val response = apiClient.getCompleteImageData(imageId)
            android.util.Log.d("ImageRepository", "Complete data API response: ${response.code()}")
            
            if (response.isSuccessful) {
                val data = response.body()
                if (data != null) {
                    val image = data.image
                    val avatar = data.avatar
                    android.util.Log.d("ImageRepository", "Loaded complete data: ${image.name} with avatar: ${avatar?.name}")
                    Result.success(Pair(image, avatar))
                } else {
                    Result.success(null)
                }
            } else {
                android.util.Log.e("ImageRepository", "Complete data API failed: ${response.code()}")
                // Try fallback - get image from local database and use fallback avatar
                val localImage = database.imageDao().getImageByIdSync(imageId)
                if (localImage != null) {
                    val backendImage = BackendImage(
                        id = localImage.id,
                        name = localImage.name,
                        description = localImage.description ?: "",
                        imageUrl = localImage.imageUrl,
                        thumbnailUrl = "",
                        isActive = true,
                        createdAt = localImage.createdAt,
                        updatedAt = localImage.updatedAt,
                        dialogues = localImage.dialogues
                    )
                    val fallbackAvatar = fallbackService.getAvatarForImageFallback(imageId)
                    Result.success(Pair(backendImage, fallbackAvatar))
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching complete data", e)
            // Try fallback - get image from local database and use fallback avatar
            try {
                val localImage = database.imageDao().getImageByIdSync(imageId)
                if (localImage != null) {
                    val backendImage = BackendImage(
                        id = localImage.id,
                        name = localImage.name,
                        description = localImage.description ?: "",
                        imageUrl = localImage.imageUrl,
                        thumbnailUrl = "",
                        isActive = true,
                        createdAt = localImage.createdAt,
                        updatedAt = localImage.updatedAt,
                        dialogues = localImage.dialogues
                    )
                    val fallbackAvatar = fallbackService.getAvatarForImageFallback(imageId)
                    Result.success(Pair(backendImage, fallbackAvatar))
                } else {
                    Result.success(null)
                }
            } catch (fallbackException: Exception) {
                android.util.Log.e("ImageRepository", "Error in fallback", fallbackException)
                Result.success(null)
            }
        }
    }
    
    /**
     * Get talking head video for specific image with language support and fallback
     */
    suspend fun getTalkingHeadVideo(imageId: String, language: String? = null): Result<TalkingHeadVideo?> {
        return try {
            android.util.Log.d("ImageRepository", "Fetching talking head video for image: $imageId with language: $language")
            val response = apiClient.getTalkingHeadVideo(imageId, language)
            android.util.Log.d("ImageRepository", "Talking head video API response: ${response.code()}")
            
            if (response.isSuccessful) {
                val video = response.body()
                android.util.Log.d("ImageRepository", "Loaded talking head video: ${video?.title}")
                return Result.success(video)
            } else {
                android.util.Log.e("ImageRepository", "Talking head video API failed: ${response.code()}")
                // Try fallback
                val fallbackVideo = TalkingHeadVideo(
                    imageId = imageId,
                    title = "Fallback Video",
                    description = "Fallback video when API is unavailable",
                    videoUrl = fallbackService.getFallbackVideoUrl(imageId),
                    duration = 30,
                    language = "en",
                    emotion = null,
                    voiceId = "default_voice",
                    createdAt = System.currentTimeMillis().toString()
                )
                Result.success(fallbackVideo)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching talking head video", e)
            // Try fallback
            val fallbackVideo = TalkingHeadVideo(
                imageId = imageId,
                title = "Fallback Video",
                description = "Fallback video when API is unavailable",
                videoUrl = fallbackService.getFallbackVideoUrl(imageId),
                duration = 30,
                language = "en",
                emotion = null,
                voiceId = "default_voice",
                createdAt = System.currentTimeMillis().toString()
            )
            Result.success(fallbackVideo)
        }
    }
}
package com.talkar.app.data.repository

import com.talkar.app.data.api.ApiService
import com.talkar.app.data.local.ImageDatabase
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.TalkingHeadVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ImageRepository(
    private val apiClient: ApiService,
    private val database: ImageDatabase
) {
    
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
     * Get all avatars from API
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
                Result.failure(Exception("API failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching avatars", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get avatar for specific image
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
                Result.failure(Exception("API failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching avatar for image", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get complete image data with avatar
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
                Result.failure(Exception("API failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching complete data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get talking head video for specific image with language support
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
                Result.failure(Exception("API failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageRepository", "Error fetching talking head video", e)
            return Result.failure(e)
        }
    }
}


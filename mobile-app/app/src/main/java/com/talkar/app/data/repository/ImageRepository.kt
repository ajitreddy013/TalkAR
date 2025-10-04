package com.talkar.app.data.repository

import com.talkar.app.data.api.ApiService
import com.talkar.app.data.local.ImageDatabase
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.BackendImage
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
                        createdAt = backendImage.createdAt,
                        updatedAt = backendImage.updatedAt
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
}


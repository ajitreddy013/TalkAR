package com.talkar.app.data.repository

import com.talkar.app.data.api.ApiService
import com.talkar.app.data.local.ImageDatabase
import com.talkar.app.data.models.ImageRecognition
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
                val images = response.body() ?: emptyList()
                android.util.Log.d("ImageRepository", "Loaded ${images.size} images from API")
                
                // Cache the images locally
                images.forEach { image ->
                    database.imageDao().insert(image)
                }
                emit(images)
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


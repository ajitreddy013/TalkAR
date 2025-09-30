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
    
    suspend fun getAllImages(): Flow<List<ImageRecognition>> = flow {
        try {
            val response = apiClient.getImages()
            if (response.isSuccessful) {
                val images = response.body() ?: emptyList()
                // Cache images locally
                database.imageDao().insertAll(images)
                emit(images)
            } else {
                // Fallback to local cache
                emit(database.imageDao().getAllImages())
            }
        } catch (e: Exception) {
            // Fallback to local cache
            emit(database.imageDao().getAllImages())
        }
    }
    
    suspend fun getImageById(id: String): Flow<ImageRecognition?> = flow {
        try {
            val response = apiClient.getImageById(id)
            if (response.isSuccessful) {
                val image = response.body()
                if (image != null) {
                    database.imageDao().insert(image)
                    emit(image)
                } else {
                    emit(database.imageDao().getImageById(id))
                }
            } else {
                emit(database.imageDao().getImageById(id))
            }
        } catch (e: Exception) {
            emit(database.imageDao().getImageById(id))
        }
    }
    
    suspend fun searchImages(query: String): Flow<List<ImageRecognition>> = flow {
        emit(database.imageDao().searchImages("%$query%"))
    }
}


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
    
    fun getAllImages(): Flow<List<ImageRecognition>> {
        return try {
            // Try to get from API first, then fallback to local cache
            // For now, return local cache directly
            database.imageDao().getAllImages()
        } catch (e: Exception) {
            // Return empty flow on error
            flow { emit(emptyList()) }
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


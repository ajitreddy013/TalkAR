package com.talkar.app.data.repository

import com.talkar.app.data.api.ApiService
import com.talkar.app.data.models.SyncRequest
import com.talkar.app.data.models.SyncResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SyncRepository(
    private val apiClient: ApiService
) {
    
    suspend fun generateSyncVideo(request: SyncRequest): Flow<SyncResponse> = flow {
        try {
            val response = apiClient.generateSyncVideo(request)
            if (response.isSuccessful) {
                val syncResponse = response.body()
                if (syncResponse != null) {
                    emit(syncResponse)
                } else {
                    throw Exception("Failed to generate sync video")
                }
            } else {
                throw Exception("API error: ${response.code()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun getSyncStatus(jobId: String): Flow<SyncResponse> = flow {
        try {
            val response = apiClient.getSyncStatus(jobId)
            if (response.isSuccessful) {
                val syncResponse = response.body()
                if (syncResponse != null) {
                    emit(syncResponse)
                } else {
                    throw Exception("Failed to get sync status")
                }
            } else {
                throw Exception("API error: ${response.code()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }
}


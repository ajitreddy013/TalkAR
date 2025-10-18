package com.talkar.app.data.api

import com.talkar.app.data.models.*
import com.talkar.app.data.config.ApiConfig
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    @GET("images")
    suspend fun getImages(): Response<List<BackendImage>>
    
    @GET("images/{id}")
    suspend fun getImageById(@Path("id") id: String): Response<BackendImage>
    
    @POST("sync/generate")
    suspend fun generateSyncVideo(@Body request: SyncRequest): Response<SyncResponse>
    
    @GET("sync/status/{jobId}")
    suspend fun getSyncStatus(@Path("jobId") jobId: String): Response<SyncResponse>
    
    @GET("sync/talking-head/{imageId}")
    suspend fun getTalkingHeadVideo(@Path("imageId") imageId: String): Response<TalkingHeadVideo>
    
    // Avatar endpoints
    @GET("avatars")
    suspend fun getAvatars(): Response<List<Avatar>>
    
    @GET("avatars/{id}")
    suspend fun getAvatarById(@Path("id") id: String): Response<Avatar>
    
    @GET("avatars/image/{imageId}")
    suspend fun getAvatarForImage(@Path("imageId") imageId: String): Response<Avatar>
    
    @GET("avatars/complete/{imageId}")
    suspend fun getCompleteImageData(@Path("imageId") imageId: String): Response<CompleteImageData>
    
    // Update avatar mapping with script and generated media URLs
    @PUT("avatars/mapping/{mappingId}")
    suspend fun updateAvatarMapping(
        @Path("mappingId") mappingId: String,
        @Body request: UpdateMappingRequest
    ): Response<MappingUpdateResponse>
    
    // Get all avatar-image mappings
    @GET("avatars/mappings")
    suspend fun getAllMappings(): Response<List<ImageAvatarMappingWithDetails>>
    
    // Map avatar to image with script
    @POST("avatars/{avatarId}/map/{imageId}")
    suspend fun mapAvatarToImage(
        @Path("avatarId") avatarId: String,
        @Path("imageId") imageId: String,
        @Body request: MapAvatarRequest
    ): Response<MappingUpdateResponse>
    
    // Lip-sync endpoints
    @POST("lipsync/generate")
    suspend fun generateLipSyncVideo(@Body request: LipSyncRequest): Response<LipSyncResponse>
    
    @GET("lipsync/status/{videoId}")
    suspend fun getLipSyncStatus(@Path("videoId") videoId: String): Response<LipSyncResponse>
    
    @GET("lipsync/voices")
    suspend fun getAvailableVoices(): Response<VoicesResponse>
    
    @POST("lipsync/talking-head")
    suspend fun generateTalkingHeadVideo(@Body request: TalkingHeadRequest): Response<LipSyncResponse>
}

object ApiClient {
    fun create(): ApiService {
        return retrofit2.Retrofit.Builder()
            .baseUrl(ApiConfig.API_V1_URL + "/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// Additional data models for API
data class CompleteImageData(
    val image: BackendImage,
    val avatar: Avatar?,
    val mapping: ImageAvatarMapping?
)

data class ImageAvatarMapping(
    val id: String,
    val script: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val visemeDataUrl: String? = null,
    val isActive: Boolean
)

data class LipSyncRequest(
    val imageId: String,
    val text: String,
    val voiceId: String? = null,
    val language: String? = null
)

data class LipSyncResponse(
    val success: Boolean,
    val videoUrl: String? = null,
    val status: String,
    val message: String? = null,
    val processingTime: Long? = null
)

data class VoicesResponse(
    val voices: List<Voice>
)

data class Voice(
    val id: String,
    val name: String,
    val language: String,
    val gender: String
)

data class TalkingHeadRequest(
    val imageId: String,
    val text: String,
    val voiceId: String? = null
)

// Update mapping request
data class UpdateMappingRequest(
    val script: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val visemeDataUrl: String? = null
)

// Map avatar request
data class MapAvatarRequest(
    val script: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val visemeDataUrl: String? = null
)

// Mapping update response
data class MappingUpdateResponse(
    val message: String,
    val mapping: ImageAvatarMapping
)

// Image-avatar mapping with full details
data class ImageAvatarMappingWithDetails(
    val id: String,
    val imageId: String,
    val avatarId: String,
    val script: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val visemeDataUrl: String? = null,
    val isActive: Boolean,
    val avatar: Avatar? = null,
    val image: BackendImage? = null
)


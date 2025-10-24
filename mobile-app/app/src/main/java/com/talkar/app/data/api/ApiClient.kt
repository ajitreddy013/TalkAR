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
    suspend fun getTalkingHeadVideo(@Path("imageId") imageId: String, @Query("language") language: String?): Response<TalkingHeadVideo>
    
    // Avatar endpoints
    @GET("avatars")
    suspend fun getAvatars(): Response<List<Avatar>>
    
    @GET("avatars/{id}")
    suspend fun getAvatarById(@Path("id") id: String): Response<Avatar>
    
    @GET("avatars/image/{imageId}")
    suspend fun getAvatarForImage(@Path("imageId") imageId: String): Response<Avatar>
    
    @GET("avatars/complete/{imageId}")
    suspend fun getCompleteImageData(@Path("imageId") imageId: String): Response<CompleteImageData>
    
    // Lip-sync endpoints
    @POST("lipsync/generate")
    suspend fun generateLipSyncVideo(@Body request: LipSyncRequest): Response<LipSyncResponse>
    
    @GET("lipsync/status/{videoId}")
    suspend fun getLipSyncStatus(@Path("videoId") videoId: String): Response<LipSyncResponse>
    
    @GET("lipsync/voices")
    suspend fun getAvailableVoices(): Response<VoicesResponse>
    
    @POST("lipsync/talking-head")
    suspend fun generateTalkingHeadVideo(@Body request: TalkingHeadRequest): Response<LipSyncResponse>
    
    // AI Pipeline endpoints
    @POST("ai-pipeline/generate_script")
    suspend fun generateScript(@Body request: ScriptGenerationRequest): Response<ScriptGenerationResponse>
    
    @POST("ai-pipeline/generate_ad_content")
    suspend fun generateAdContent(@Body request: AdContentGenerationRequest): Response<AdContentGenerationResponse>
    
    @POST("ai-pipeline/generate_ad_content_streaming")
    suspend fun generateAdContentStreaming(@Body request: AdContentGenerationRequest): Response<AdContentGenerationResponse>
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

// AI Pipeline data models
data class ScriptGenerationRequest(
    val imageId: String,
    val language: String? = null,
    val emotion: String? = null,
    val userPreferences: UserPreferences? = null
)

data class ScriptGenerationResponse(
    val success: Boolean,
    val script: String? = null,
    val language: String? = null,
    val emotion: String? = null
)

data class UserPreferences(
    val language: String? = null,
    val preferredTone: String? = null
)

// Ad Content Generation models
data class AdContentGenerationRequest(
    val product: String
)

data class AdContentGenerationResponse(
    val success: Boolean,
    val script: String? = null,
    val audio_url: String? = null,
    val video_url: String? = null
)
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
    
    // Dynamic Script Generation endpoints
    @POST("generate-dynamic-script")
    suspend fun generateDynamicScript(@Body request: DynamicScriptRequest): Response<DynamicScriptResponse>
    
    @POST("ai-pipeline/generate_ad_content_from_poster")
    suspend fun generateAdContentFromPoster(@Body request: PosterAdContentRequest): Response<PosterAdContentResponse>
    
    @GET("generate-dynamic-script/posters")
    suspend fun getAllPosters(): Response<PostersResponse>
    
    @GET("generate-dynamic-script/poster/{image_id}")
    suspend fun getPosterById(@Path("image_id") imageId: String): Response<PosterResponse>
    
    // Conversational Context endpoint
    @POST("ai-pipeline/conversational_query")
    suspend fun processConversationalQuery(@Body request: ConversationalQueryRequest): Response<ConversationalQueryResponse>
    
    // Feedback endpoint
    @POST("feedback")
    suspend fun sendFeedback(@Body request: FeedbackRequest): Response<FeedbackResponse>
    
    // User Context Feedback endpoint
    @POST("feedback/user-context")
    suspend fun sendUserContextFeedback(@Body request: FeedbackRequest): Response<FeedbackResponse>
    
    // AI Config endpoints
    @GET("ai-config/defaults/tone")
    suspend fun getDefaultTone(): Response<DefaultToneResponse>
    
    @GET("ai-config/prompt-template")
    suspend fun getPromptTemplate(): Response<PromptTemplateResponse>
}

object ApiClient {
    private const val AUTH_TOKEN = "demo-token-123" // TODO: Replace with actual token management

    fun create(): ApiService {
        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $AUTH_TOKEN")
                    .method(original.method(), original.body())
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return retrofit2.Retrofit.Builder()
            .baseUrl(ApiConfig.API_V1_URL + "/")
            .client(okHttpClient)
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

// Conversational Context models
data class ConversationalQueryRequest(
    val query: String,
    val imageId: String? = null,
    val context: Map<String, Any>? = null
)

data class ConversationalQueryResponse(
    val success: Boolean,
    val response: String,
    val audioUrl: String? = null,
    val emotion: String? = null
)

// Ad Content Generation models
data class AdContentGenerationRequest(
    val product: String,
    val previous_products: List<String>? = null
)

data class AdContentGenerationResponse(
    val success: Boolean,
    val script: String? = null,
    val audio_url: String? = null,
    val video_url: String? = null
)

// Feedback models
data class FeedbackRequest(
    val adContentId: String,
    val productName: String,
    val isPositive: Boolean,
    val timestamp: Long
)

data class FeedbackResponse(
    val success: Boolean,
    val message: String?
)

data class DefaultToneResponse(
    val success: Boolean,
    val tone: String
)

data class PromptTemplateResponse(
    val success: Boolean,
    val template: String
)

// Dynamic Script Generation models
data class DynamicScriptRequest(
    val image_id: String,
    val user_id: String? = null
)

data class DynamicScriptResponse(
    val success: Boolean,
    val image_id: String? = null,
    val product_name: String? = null,
    val category: String? = null,
    val tone: String? = null,
    val language: String? = null,
    val image_url: String? = null,
    val brand: String? = null,
    val script: String? = null,
    val metadata: ScriptMetadata? = null
)

data class ScriptMetadata(
    val generated_at: String,
    val user_id: String,
    val model_used: String,
    val word_count: Int
)

// Poster Ad Content Generation models
data class PosterAdContentRequest(
    val image_id: String,
    val user_id: String? = null
)

data class PosterAdContentResponse(
    val success: Boolean,
    val script: String? = null,
    val audio_url: String? = null,
    val video_url: String? = null,
    val metadata: PosterMetadata? = null
)

data class PosterMetadata(
    val image_id: String,
    val product_name: String,
    val language: String,
    val tone: String,
    val image_url: String,
    val generated_at: String,
    val user_id: String
)

// Poster Management models
data class PostersResponse(
    val success: Boolean,
    val posters: List<PosterInfo>
)

data class PosterInfo(
    val image_id: String,
    val product_name: String,
    val category: String,
    val tone: String,
    val language: String,
    val image_url: String,
    val brand: String
)

data class PosterResponse(
    val success: Boolean,
    val poster: PosterDetails
)

data class PosterDetails(
    val image_id: String,
    val product_name: String,
    val category: String,
    val tone: String,
    val language: String,
    val image_url: String,
    val brand: String,
    val price: Double,
    val currency: String,
    val features: List<String>,
    val description: String
)
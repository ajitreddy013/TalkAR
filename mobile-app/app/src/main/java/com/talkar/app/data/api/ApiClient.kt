package com.talkar.app.data.api

import com.talkar.app.data.models.*
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
}

object ApiClient {
    private const val BASE_URL = "http://10.17.5.127:3000/api/v1/"
    
    fun create(): ApiService {
        return retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}


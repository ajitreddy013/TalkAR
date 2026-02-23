package com.talkar.app.ar.video.backend

import com.talkar.app.data.api.ApiClient
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Factory for creating BackendVideoFetcher instances.
 */
object BackendVideoFetcherFactory {
    
    /**
     * Creates a BackendVideoFetcher with default configuration.
     * 
     * @return Configured BackendVideoFetcher instance
     */
    fun create(): BackendVideoFetcher {
        val apiService = ApiClient.create()
        
        // Create OkHttpClient with longer timeouts for video download
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        
        return BackendVideoFetcherImpl(apiService, okHttpClient)
    }
    
    /**
     * Creates a BackendVideoFetcher with custom configuration.
     * 
     * @param apiService Custom API service
     * @param okHttpClient Custom OkHttp client
     * @return Configured BackendVideoFetcher instance
     */
    fun create(
        apiService: com.talkar.app.data.api.ApiService,
        okHttpClient: OkHttpClient
    ): BackendVideoFetcher {
        return BackendVideoFetcherImpl(apiService, okHttpClient)
    }
}

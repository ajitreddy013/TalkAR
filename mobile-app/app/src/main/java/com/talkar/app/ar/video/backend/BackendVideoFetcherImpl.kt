package com.talkar.app.ar.video.backend

import android.util.Log
import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.GenerateResponse
import com.talkar.app.ar.video.models.StatusResponse
import com.talkar.app.ar.video.models.TalkingPhotoRequest
import com.talkar.app.data.api.ApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

/**
 * Implementation of BackendVideoFetcher using Retrofit API service.
 * 
 * Implements exponential backoff retry logic and status polling.
 */
class BackendVideoFetcherImpl(
    private val apiService: ApiService,
    private val okHttpClient: OkHttpClient
) : BackendVideoFetcher {
    
    companion object {
        private const val TAG = "BackendVideoFetcher"
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
        private const val STATUS_POLL_INTERVAL_MS = 2000L
        private const val STATUS_POLL_TIMEOUT_MS = 60000L // 60 seconds
    }
    
    @Volatile
    private var isCancelled = false
    
    override suspend fun generateLipSync(request: TalkingPhotoRequest): Result<String> {
        Log.d(TAG, "🎬 Generating lip-sync video")
        Log.d(TAG, "  Poster ID: ${request.posterId}")
        Log.d(TAG, "  Text: ${request.text}")
        Log.d(TAG, "  Voice: ${request.voiceId}")
        
        return retryWithExponentialBackoff {
            Log.d(TAG, "  📡 Sending request to backend...")
            
            val response = apiService.generateTalkingHeadVideo(
                com.talkar.app.data.api.TalkingHeadRequest(
                    imageId = request.posterId,
                    text = request.text,
                    voiceId = request.voiceId
                )
            )
            
            Log.d(TAG, "  📥 Response code: ${response.code()}")
            Log.d(TAG, "  📥 Response success: ${response.isSuccessful}")
            
            if (response.isSuccessful && response.body()?.success == true) {
                val videoId = response.body()?.videoUrl ?: throw Exception("No videoId in response")
                Log.d(TAG, "  ✅ Generation started: videoId=$videoId")
                videoId
            } else {
                val errorMsg = response.body()?.message ?: "Unknown error"
                Log.e(TAG, "  ❌ Generation failed: $errorMsg")
                Log.e(TAG, "  Response body: ${response.body()}")
                throw Exception(TalkingPhotoError.GenerationFailed(errorMsg).message)
            }
        }
    }
    
    override suspend fun checkStatus(videoId: String): Result<StatusResponse> {
        if (isCancelled) {
            return Result.failure(Exception("Operation cancelled"))
        }
        
        return try {
            Log.d(TAG, "🔍 Checking status for: $videoId")
            
            val response = apiService.getLipSyncStatus(videoId)
            
            Log.d(TAG, "  📥 Status response code: ${response.code()}")
            
            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                
                Log.d(TAG, "  Status: ${body.status}")
                Log.d(TAG, "  Video URL: ${body.videoUrl}")
                Log.d(TAG, "  Processing time: ${body.processingTime}ms")
                
                // Map the existing LipSyncResponse to StatusResponse
                val statusResponse = StatusResponse(
                    videoId = videoId,
                    status = body.status,
                    progress = if (body.status == "complete") 1.0f else 0.5f,
                    videoUrl = body.videoUrl,
                    lipCoordinates = null, // Will be added when backend provides it
                    checksum = null, // Will be added when backend provides it
                    durationMs = body.processingTime,
                    sizeBytes = null,
                    errorMessage = body.message,
                    estimatedTimeRemaining = null
                )
                
                Log.d(TAG, "  ✅ Status check successful")
                Result.success(statusResponse)
            } else {
                val errorMsg = response.body()?.message ?: "Status check failed"
                Log.e(TAG, "  ❌ Status check failed: $errorMsg")
                Result.failure(Exception(TalkingPhotoError.BackendUnavailable(errorMsg).message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Status check error", e)
            Result.failure(e)
        }
    }

    
    override suspend fun downloadVideo(
        videoUrl: String,
        destinationPath: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        if (isCancelled) {
            return Result.failure(Exception("Operation cancelled"))
        }
        
        Log.d(TAG, "Downloading video from: $videoUrl")
        
        return try {
            val request = Request.Builder()
                .url(videoUrl)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception(TalkingPhotoError.DownloadFailed(
                    "Download failed with code: ${response.code}",
                    videoUrl
                ).message)
            }
            
            val body = response.body ?: throw Exception(TalkingPhotoError.DownloadFailed(
                "Empty response body",
                videoUrl
            ).message)
            
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            
            val destinationFile = File(destinationPath)
            destinationFile.parentFile?.mkdirs()
            
            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled) {
                            destinationFile.delete()
                            return Result.failure(Exception("Download cancelled"))
                        }
                        
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes
                            onProgress(progress)
                        }
                    }
                }
            }
            
            Log.d(TAG, "✅ Download complete: $destinationPath")
            Result.success(destinationPath)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Download error", e)
            Result.failure(e)
        }
    }
    
    override fun cancel() {
        Log.d(TAG, "Cancelling operations")
        isCancelled = true
    }
    
    /**
     * Polls status endpoint until video generation is complete or failed.
     * 
     * @param videoId Video generation job ID
     * @return Result containing final StatusResponse
     */
    suspend fun pollUntilComplete(videoId: String): Result<StatusResponse> {
        val startTime = System.currentTimeMillis()
        
        while (coroutineContext.isActive && !isCancelled) {
            val statusResult = checkStatus(videoId)
            
            if (statusResult.isFailure) {
                return statusResult
            }
            
            val status = statusResult.getOrNull()!!
            
            when (status.status) {
                "complete" -> {
                    Log.d(TAG, "✅ Generation complete: videoId=$videoId")
                    return Result.success(status)
                }
                "failed" -> {
                    val errorMsg = status.errorMessage ?: "Generation failed"
                    Log.e(TAG, "❌ Generation failed: $errorMsg")
                    return Result.failure(
                        Exception(TalkingPhotoError.GenerationFailed(errorMsg, videoId).message)
                    )
                }
                "processing" -> {
                    // Check timeout
                    val elapsed = System.currentTimeMillis() - startTime
                    if (elapsed > STATUS_POLL_TIMEOUT_MS) {
                        Log.e(TAG, "❌ Status polling timeout")
                        return Result.failure(
                            Exception(TalkingPhotoError.GenerationFailed("Timeout", videoId).message)
                        )
                    }
                    
                    // Wait before next poll
                    delay(STATUS_POLL_INTERVAL_MS)
                }
                else -> {
                    Log.w(TAG, "Unknown status: ${status.status}")
                    delay(STATUS_POLL_INTERVAL_MS)
                }
            }
        }
        
        return Result.failure(Exception("Polling cancelled"))
    }
    
    /**
     * Retries a suspending operation with exponential backoff.
     * 
     * @param block Operation to retry
     * @return Result of the operation
     */
    private suspend fun <T> retryWithExponentialBackoff(
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = INITIAL_DELAY_MS
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES + 1) { attempt ->
            if (isCancelled) {
                return Result.failure(Exception("Operation cancelled"))
            }
            
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e
                
                if (attempt < MAX_RETRIES) {
                    Log.w(TAG, "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms", e)
                    delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                } else {
                    Log.e(TAG, "All $MAX_RETRIES retry attempts failed", e)
                }
            }
        }
        
        return Result.failure(
            lastException ?: Exception("Unknown error after $MAX_RETRIES retries")
        )
    }
}

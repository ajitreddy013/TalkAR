package com.talkar.app.ar.video.backend

import com.talkar.app.ar.video.models.GenerateResponse
import com.talkar.app.ar.video.models.StatusResponse
import com.talkar.app.ar.video.models.TalkingPhotoRequest

/**
 * Interface for fetching lip-sync videos from the backend service.
 * 
 * Manages communication with the backend for video generation, status polling,
 * and video download with progress tracking.
 */
interface BackendVideoFetcher {
    /**
     * Requests lip-sync video generation for a poster.
     * 
     * @param request Contains posterId, text, and voiceId
     * @return Result containing videoId for polling status, or error
     */
    suspend fun generateLipSync(request: TalkingPhotoRequest): Result<String>
    
    /**
     * Polls generation status.
     * 
     * @param videoId ID returned from generateLipSync
     * @return Result containing current status and progress, or error
     */
    suspend fun checkStatus(videoId: String): Result<StatusResponse>
    
    /**
     * Downloads completed lip-sync video.
     * 
     * @param videoUrl URL provided by backend
     * @param destinationPath Local file path to save the video
     * @param onProgress Progress callback (0.0 to 1.0)
     * @return Result containing local file path, or error
     */
    suspend fun downloadVideo(
        videoUrl: String,
        destinationPath: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String>
    
    /**
     * Cancels ongoing generation or download.
     */
    fun cancel()
}

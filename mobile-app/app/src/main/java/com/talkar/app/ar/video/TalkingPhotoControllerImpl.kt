package com.talkar.app.ar.video

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ar.core.Anchor
import com.talkar.app.ar.video.backend.BackendVideoFetcher
import com.talkar.app.ar.video.cache.VideoCache
import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.LipCoordinates
import com.talkar.app.ar.video.models.TalkingPhotoRequest
import com.talkar.app.ar.video.models.TalkingPhotoSession
import com.talkar.app.ar.video.models.TalkingPhotoState
import com.talkar.app.ar.video.models.TrackingData
import com.talkar.app.ar.video.rendering.LipRegionRenderer
import com.talkar.app.ar.video.rendering.RenderCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementation of TalkingPhotoController that orchestrates the complete
 * talking photo lifecycle.
 */
class TalkingPhotoControllerImpl(
    private val context: Context,
    private val backendFetcher: BackendVideoFetcher,
    private val videoCache: VideoCache,
    private val videoDecoder: VideoDecoder,
    private val lipRenderer: LipRegionRenderer,
    private val renderCoordinator: RenderCoordinator
) : TalkingPhotoController {
    
    companion object {
        private const val TAG = "TalkingPhotoController"
        private const val PAUSE_RESOURCE_RELEASE_DELAY_MS = 5000L
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var callbacks: TalkingPhotoCallbacks? = null
    
    private var currentState: TalkingPhotoState = TalkingPhotoState.IDLE
    private var session: TalkingPhotoSession? = null
    private var currentAnchor: Anchor? = null
    
    private var pausedPosition: Long = 0L
    private var resourceReleaseJob: Job? = null
    
    override suspend fun initialize(anchor: Anchor, posterId: String): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Initializing talking photo for poster: $posterId")
                
                currentAnchor = anchor
                setState(TalkingPhotoState.FETCHING_VIDEO)
                
                // Check cache first
                val cachedVideo = videoCache.retrieve(posterId)
                
                if (cachedVideo != null) {
                    Log.d(TAG, "Cache hit for poster: $posterId")
                    setupVideo(posterId, cachedVideo.videoPath, cachedVideo.lipCoordinates)
                    Result.success(Unit)
                } else {
                    Log.d(TAG, "Cache miss for poster: $posterId, fetching from backend")
                    fetchAndSetupVideo(posterId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize talking photo", e)
                val error = TalkingPhotoError.GenerationFailed("Initialization failed: ${e.message}")
                callbacks?.onError(error)
                setState(TalkingPhotoState.ERROR)
                Result.failure(e)
            }
        }
    }
    
    private suspend fun fetchAndSetupVideo(posterId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                setState(TalkingPhotoState.GENERATING)
                callbacks?.onGenerationStarted()
                
                // Request video generation
                val request = TalkingPhotoRequest(
                    posterId = posterId,
                    text = "Hello! Welcome to TalkAR.",
                    voiceId = "en-US-male-1"
                )
                
                val videoIdResult = backendFetcher.generateLipSync(request)
                if (videoIdResult.isFailure) {
                    throw videoIdResult.exceptionOrNull() 
                        ?: Exception("Failed to generate video")
                }
                
                val videoId = videoIdResult.getOrThrow()
                
                // Poll for completion
                var status = backendFetcher.checkStatus(videoId).getOrThrow()
                while (status.status == "processing") {
                    callbacks?.onGenerationProgress(status.progress)
                    delay(2000)
                    status = backendFetcher.checkStatus(videoId).getOrThrow()
                }
                
                if (status.status == "failed") {
                    throw Exception(status.errorMessage ?: "Generation failed")
                }
                
                // Download video
                setState(TalkingPhotoState.DOWNLOADING)
                val videoUrl = status.videoUrl 
                    ?: throw Exception("No video URL in response")
                val lipCoordinates = status.lipCoordinates?.toLipCoordinates()
                    ?: throw Exception("No lip coordinates in response")
                
                // Create destination path
                val cacheDir = File(context.cacheDir, "talking_photos")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val destinationPath = File(cacheDir, "$posterId.mp4").absolutePath
                
                val videoPathResult = backendFetcher.downloadVideo(
                    videoUrl = videoUrl,
                    destinationPath = destinationPath,
                    onProgress = { progress ->
                        callbacks?.onGenerationProgress(progress)
                    }
                )
                
                if (videoPathResult.isFailure) {
                    throw videoPathResult.exceptionOrNull() 
                        ?: Exception("Failed to download video")
                }
                
                val videoPath = videoPathResult.getOrThrow()
                
                // Cache the video
                videoCache.store(
                    posterId = posterId,
                    videoPath = videoPath,
                    lipCoordinates = lipCoordinates,
                    checksum = status.checksum ?: ""
                )
                
                withContext(Dispatchers.Main) {
                    setupVideo(posterId, videoPath, lipCoordinates)
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch and setup video", e)
                val error = TalkingPhotoError.GenerationFailed(e.message ?: "Unknown error")
                withContext(Dispatchers.Main) {
                    callbacks?.onError(error)
                    setState(TalkingPhotoState.ERROR)
                }
                Result.failure(e)
            }
        }
    }
    
    private suspend fun setupVideo(
        posterId: String,
        videoPath: String,
        lipCoordinates: LipCoordinates
    ) {
        withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Setting up video for playback")
                
                // Initialize renderer with lip coordinates
                lipRenderer.setLipCoordinates(lipCoordinates)
                
                // Get surface from renderer
                val surface = lipRenderer.getSurface()
                if (surface == null) {
                    throw Exception("Failed to get surface from renderer")
                }
                
                // Initialize video decoder
                val videoUri = Uri.fromFile(File(videoPath))
                val videoInfoResult = videoDecoder.initialize(videoUri, surface)
                if (videoInfoResult.isFailure) {
                    throw videoInfoResult.exceptionOrNull() 
                        ?: Exception("Failed to initialize decoder")
                }
                
                // Create session
                session = TalkingPhotoSession(
                    posterId = posterId,
                    anchorId = currentAnchor?.hashCode()?.toString() ?: "",
                    state = TalkingPhotoState.READY,
                    lipCoordinates = lipCoordinates,
                    videoPath = videoPath,
                    isTracking = true,
                    lastUpdateTimestamp = System.currentTimeMillis(),
                    consecutiveTrackingFrames = 0,
                    consecutiveLostFrames = 0
                )
                
                setState(TalkingPhotoState.READY)
                callbacks?.onVideoReady()
                
                Log.d(TAG, "Video setup complete, ready for playback")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup video", e)
                val error = TalkingPhotoError.GenerationFailed("Setup failed: ${e.message}")
                callbacks?.onError(error)
                setState(TalkingPhotoState.ERROR)
            }
        }
    }
    
    override fun updateTracking(trackingData: TrackingData) {
        val currentSession = session ?: return
        
        // Update session tracking state
        val updatedSession = if (trackingData.isTracking) {
            currentSession.copy(
                isTracking = true,
                lastUpdateTimestamp = trackingData.timestamp,
                consecutiveTrackingFrames = currentSession.consecutiveTrackingFrames + 1,
                consecutiveLostFrames = 0
            )
        } else {
            currentSession.copy(
                isTracking = false,
                lastUpdateTimestamp = trackingData.timestamp,
                consecutiveTrackingFrames = 0,
                consecutiveLostFrames = currentSession.consecutiveLostFrames + 1
            )
        }
        
        session = updatedSession
        
        // Handle tracking state changes
        if (updatedSession.shouldPause() && currentState == TalkingPhotoState.PLAYING) {
            pausedPosition = videoDecoder.getCurrentPosition()
            videoDecoder.pause()
            setState(TalkingPhotoState.PAUSED)
            callbacks?.onTrackingLost()
            
            // Schedule resource release after delay
            scheduleResourceRelease()
        } else if (updatedSession.shouldResume() && currentState == TalkingPhotoState.PAUSED) {
            cancelResourceRelease()
            videoDecoder.start()
            setState(TalkingPhotoState.PLAYING)
            callbacks?.onTrackingResumed()
        }
        
        // Update renderer transform if tracking
        if (trackingData.isTracking) {
            val anchor = currentAnchor ?: return
            // Calculate transform using render coordinator
            // This would typically use ARCore camera and viewport
            // For now, we'll apply the tracking data directly
            lipRenderer.setVisible(true)
        } else {
            lipRenderer.setVisible(false)
        }
    }
    
    override fun play() {
        if (currentState != TalkingPhotoState.READY && currentState != TalkingPhotoState.PAUSED) {
            Log.w(TAG, "Cannot play in state: $currentState")
            return
        }
        
        cancelResourceRelease()
        
        if (currentState == TalkingPhotoState.PAUSED && pausedPosition > 0) {
            videoDecoder.seekTo(pausedPosition)
        }
        
        videoDecoder.start()
        setState(TalkingPhotoState.PLAYING)
    }
    
    override fun pause() {
        if (currentState != TalkingPhotoState.PLAYING) {
            Log.w(TAG, "Cannot pause in state: $currentState")
            return
        }
        
        pausedPosition = videoDecoder.getCurrentPosition()
        videoDecoder.pause()
        setState(TalkingPhotoState.PAUSED)
        
        scheduleResourceRelease()
    }
    
    override fun stop() {
        videoDecoder.stop()
        pausedPosition = 0L
        setState(TalkingPhotoState.READY)
    }
    
    override fun getState(): TalkingPhotoState {
        return currentState
    }
    
    override fun getCurrentPosition(): Long {
        return if (currentState == TalkingPhotoState.PLAYING) {
            videoDecoder.getCurrentPosition()
        } else {
            pausedPosition
        }
    }
    
    override fun seekTo(positionMs: Long) {
        pausedPosition = positionMs
        if (currentState == TalkingPhotoState.PLAYING) {
            videoDecoder.seekTo(positionMs)
        }
    }
    
    override fun release() {
        Log.d(TAG, "Releasing talking photo controller")
        
        cancelResourceRelease()
        
        videoDecoder.release()
        lipRenderer.release()
        currentAnchor?.detach()
        currentAnchor = null
        session = null
        
        scope.cancel()
        
        setState(TalkingPhotoState.IDLE)
    }
    
    override fun setCallbacks(callbacks: TalkingPhotoCallbacks) {
        this.callbacks = callbacks
    }
    
    private fun setState(newState: TalkingPhotoState) {
        if (currentState != newState) {
            Log.d(TAG, "State transition: $currentState -> $newState")
            currentState = newState
            session = session?.copy(state = newState)
        }
    }
    
    private fun scheduleResourceRelease() {
        cancelResourceRelease()
        
        resourceReleaseJob = scope.launch {
            delay(PAUSE_RESOURCE_RELEASE_DELAY_MS)
            if (isActive && currentState == TalkingPhotoState.PAUSED) {
                Log.d(TAG, "Releasing decoder resources after pause timeout")
                videoDecoder.release()
            }
        }
    }
    
    private fun cancelResourceRelease() {
        resourceReleaseJob?.cancel()
        resourceReleaseJob = null
    }
}

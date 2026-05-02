package com.talkar.app.ar.video

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ar.core.Anchor
import com.talkar.app.ar.video.backend.BackendVideoFetcher
import com.talkar.app.ar.video.cache.VideoCache
import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.LipCoordinates
import com.talkar.app.ar.video.models.Matrix4
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
        private const val ARTIFACT_MAX_ATTEMPTS = 10
        private const val ARTIFACT_INITIAL_RETRY_MS = 1000L
        private const val ARTIFACT_MAX_RETRY_MS = 6000L
        private const val ARTIFACT_TOTAL_TIMEOUT_MS = 45000L
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
                setState(TalkingPhotoState.DETECTED)
                fetchAndSetupVideo(posterId)
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
                setState(TalkingPhotoState.FETCHING_ARTIFACT)
                callbacks?.onGenerationStarted()
                callbacks?.onGenerationProgress(0.1f)
                callbacks?.onArtifactStatus("FETCHING")

                var artifact: com.talkar.app.ar.video.models.TalkingPhotoArtifactResponse? = null
                var lastError: String? = null
                val fetchStartedAt = System.currentTimeMillis()
                var backoffMs = ARTIFACT_INITIAL_RETRY_MS
                for (attempt in 1..ARTIFACT_MAX_ATTEMPTS) {
                    val elapsed = System.currentTimeMillis() - fetchStartedAt
                    if (elapsed >= ARTIFACT_TOTAL_TIMEOUT_MS) {
                        lastError = "ARTIFACT_NOT_READY"
                        break
                    }

                    val artifactResult = backendFetcher.getTalkingPhotoArtifact(posterId)
                    if (artifactResult.isFailure) {
                        lastError = "NETWORK_UNAVAILABLE"
                    } else {
                        val candidate = artifactResult.getOrThrow()
                        if (candidate.runtimeMode == "ready_only" && candidate.status != "ready") {
                            lastError = "ARTIFACT_NOT_READY"
                            break
                        }
                        if (candidate.runtimeMode == "enqueue_disabled" && candidate.status != "ready") {
                            lastError = "ARTIFACT_NOT_READY"
                            break
                        }
                        if (candidate.status == "ready") {
                            artifact = candidate
                            break
                        }
                        lastError = candidate.errorCode ?: "ARTIFACT_NOT_READY"
                    }

                    callbacks?.onArtifactStatus("WAITING_$attempt")
                    setState(TalkingPhotoState.ARTIFACT_WAITING)
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(ARTIFACT_MAX_RETRY_MS)
                }

                val resolvedArtifact = artifact ?: throw Exception(lastError ?: "ARTIFACT_NOT_READY")

                val confidence = resolvedArtifact.confidence ?: 0f
                if (confidence < 0.80f) {
                    throw Exception("TRACKING_UNSTABLE")
                }

                val lipCoordinates = resolvedArtifact.lipLandmarks?.toLipCoordinates()
                    ?: throw Exception("ARTIFACT_NOT_READY")
                val videoUrl = resolvedArtifact.videoUrl
                    ?: throw Exception("ARTIFACT_NOT_READY")
                val cacheKey = "${posterId}_v${resolvedArtifact.version}"

                val cachedVideo = videoCache.retrieve(cacheKey)
                if (cachedVideo != null) {
                    withContext(Dispatchers.Main) {
                        setupVideo(posterId, cachedVideo.videoPath, cachedVideo.lipCoordinates)
                    }
                    return@withContext Result.success(Unit)
                }

                callbacks?.onGenerationProgress(0.5f)
                setState(TalkingPhotoState.FETCHING_ARTIFACT)

                val cacheDir = File(context.cacheDir, "talking_photos")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }
                val destinationPath = File(cacheDir, "${cacheKey}.mp4").absolutePath
                
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
                    posterId = cacheKey,
                    videoPath = videoPath,
                    lipCoordinates = lipCoordinates,
                    checksum = calculateChecksum(videoPath)
                )
                
                withContext(Dispatchers.Main) {
                    setupVideo(posterId, videoPath, lipCoordinates)
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch and setup video", e)
                val error = when (e.message) {
                    "ARTIFACT_NOT_READY" -> TalkingPhotoError.GenerationFailed("ARTIFACT_NOT_READY")
                    "TRACKING_UNSTABLE" -> TalkingPhotoError.GenerationFailed("TRACKING_UNSTABLE")
                    "NETWORK_UNAVAILABLE" -> TalkingPhotoError.BackendUnavailable("NETWORK_UNAVAILABLE")
                    else -> TalkingPhotoError.GenerationFailed(e.message ?: "Unknown error")
                }
                withContext(Dispatchers.Main) {
                    callbacks?.onError(error)
                    setState(TalkingPhotoState.ERROR_RETRYABLE)
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
                callbacks?.onLipCoordinatesReady(lipCoordinates)
                
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
            setState(TalkingPhotoState.LOST_TRACKING)
            callbacks?.onTrackingLost()
            
            // Schedule resource release after delay
            scheduleResourceRelease()
        } else if (updatedSession.shouldResume() && currentState == TalkingPhotoState.LOST_TRACKING) {
            cancelResourceRelease()
            videoDecoder.start()
            setState(TalkingPhotoState.RESUMED)
            setState(TalkingPhotoState.PLAYING)
            callbacks?.onTrackingResumed()
        }
        
        // Update renderer transform if tracking
        if (trackingData.isTracking) {
            val transform = trackingDataToMatrix(trackingData)
            lipRenderer.setTransform(transform)
            lipRenderer.setVisible(updatedSession.isTrackingStable())
            lipRenderer.renderFrame()
        } else {
            lipRenderer.setVisible(false)
        }
    }
    
    override fun play() {
        if (currentState != TalkingPhotoState.READY && currentState != TalkingPhotoState.PAUSED && currentState != TalkingPhotoState.LOST_TRACKING) {
            Log.w(TAG, "Cannot play in state: $currentState")
            return
        }
        
        cancelResourceRelease()
        
        if ((currentState == TalkingPhotoState.PAUSED || currentState == TalkingPhotoState.LOST_TRACKING) && pausedPosition > 0) {
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
            if (isActive && currentState == TalkingPhotoState.LOST_TRACKING) {
                Log.d(TAG, "Releasing decoder resources after pause timeout")
                videoDecoder.release()
            }
        }
    }
    
    private fun cancelResourceRelease() {
        resourceReleaseJob?.cancel()
        resourceReleaseJob = null
    }

    private fun trackingDataToMatrix(trackingData: TrackingData): Matrix4 {
        val view = trackingData.viewMatrix
        val projection = trackingData.projectionMatrix
        if (view != null && projection != null && view.size == 16 && projection.size == 16) {
            val model = floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                trackingData.position.x, trackingData.position.y, trackingData.position.z, 1f
            )
            val vp = multiply4x4(projection, view)
            return Matrix4(multiply4x4(vp, model))
        }

        val tx = trackingData.position.x
        val ty = trackingData.position.y
        val tz = trackingData.position.z
        val sx = (1f + trackingData.scale.x * 0.25f).coerceAtLeast(0.1f)
        val sy = (1f + trackingData.scale.y * 0.25f).coerceAtLeast(0.1f)
        return Matrix4(
            floatArrayOf(
                sx, 0f, 0f, 0f,
                0f, sy, 0f, 0f,
                0f, 0f, 1f, 0f,
                tx, ty, tz, 1f
            )
        )
    }

    private fun multiply4x4(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += a[row * 4 + k] * b[k * 4 + col]
                }
                out[row * 4 + col] = sum
            }
        }
        return out
    }

    private fun calculateChecksum(filePath: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val file = File(filePath)
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        val hashBytes = digest.digest()
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        return "sha256:$hexString"
    }
}

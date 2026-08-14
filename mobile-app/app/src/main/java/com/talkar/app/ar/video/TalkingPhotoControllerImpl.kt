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
import java.security.MessageDigest

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
        private const val MIN_CONFIDENCE_THRESHOLD = 0.45f
        private const val SYNC_SAMPLE_VIDEO_URL = "assets.sync.so/docs/example-talking-head.mp4"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var callbacks: TalkingPhotoCallbacks? = null
    
    private var currentState: TalkingPhotoState = TalkingPhotoState.IDLE
    private var session: TalkingPhotoSession? = null
    private var currentAnchor: Anchor? = null
    
    private var pausedPosition: Long = 0L
    private var resourceReleaseJob: Job? = null
    
    override suspend fun initialize(anchor: Anchor?, posterId: String): Result<Unit> {
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
                var generationQueued = false
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
                        val code = artifactResult.exceptionOrNull()?.message
                        lastError = when (code) {
                            "ARTIFACT_NOT_READY" -> "ARTIFACT_NOT_READY"
                            "NO_DEFAULT_SCRIPT" -> "NO_DEFAULT_SCRIPT"
                            "NO_FACE_IN_POSTER" -> "NO_FACE_IN_POSTER"
                            "PROVIDER_FAILED" -> "PROVIDER_FAILED"
                            else -> "NETWORK_UNAVAILABLE"
                        }
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
                        if (candidate.status == "ready" && isRejectedSampleArtifact(candidate.videoUrl)) {
                            Log.w(TAG, "Rejecting vendor sample artifact for posterId=$posterId")
                            lastError = "ARTIFACT_NOT_READY"
                        } else if (candidate.status == "ready") {
                            artifact = candidate
                            break
                        } else {
                            lastError = candidate.errorCode ?: "ARTIFACT_NOT_READY"
                        }
                    }

                    if ((lastError == "ARTIFACT_NOT_READY" || lastError == "PROVIDER_FAILED") && !generationQueued) {
                        val queueResult = backendFetcher.requestTalkingPhotoGeneration(posterId)
                        generationQueued = queueResult.isSuccess
                        val queueError = queueResult.exceptionOrNull()?.message
                        if (queueError == "NO_DEFAULT_SCRIPT" || queueError == "NO_FACE_IN_POSTER") {
                            lastError = queueError
                            break
                        }
                    }

                    callbacks?.onArtifactStatus("WAITING_$attempt")
                    setState(TalkingPhotoState.ARTIFACT_WAITING)
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(ARTIFACT_MAX_RETRY_MS)
                }

                val resolvedArtifact = artifact ?: throw Exception(lastError ?: "ARTIFACT_NOT_READY")

                val confidence = resolvedArtifact.confidence ?: 0f
                if (confidence < MIN_CONFIDENCE_THRESHOLD) {
                    Log.w(
                        TAG,
                        "Low confidence for posterId=$posterId confidence=$confidence threshold=$MIN_CONFIDENCE_THRESHOLD"
                    )
                    throw Exception("TRACKING_UNSTABLE")
                }

                val lipCoordinates = resolvedArtifact.lipLandmarks?.toLipCoordinates()
                    ?: throw Exception("ARTIFACT_NOT_READY")
                val videoUrl = resolvedArtifact.videoUrl
                    ?: throw Exception("ARTIFACT_NOT_READY")
                val cacheKey = buildArtifactCacheKey(
                    posterId = posterId,
                    version = resolvedArtifact.version,
                    videoUrl = videoUrl,
                    updatedAt = resolvedArtifact.updatedAt
                )

                val cachedVideo = videoCache.retrieve(cacheKey)
                if (cachedVideo != null) {
                    Log.d(TAG, "Using cached talking-photo artifact: $cacheKey")
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
                    "NO_DEFAULT_SCRIPT" -> TalkingPhotoError.GenerationFailed("NO_DEFAULT_SCRIPT")
                    "NO_FACE_IN_POSTER" -> TalkingPhotoError.GenerationFailed("NO_FACE_IN_POSTER")
                    "PROVIDER_FAILED" -> TalkingPhotoError.GenerationFailed("PROVIDER_FAILED")
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

    private fun isRejectedSampleArtifact(videoUrl: String?): Boolean {
        return videoUrl?.contains(SYNC_SAMPLE_VIDEO_URL) == true
    }

    private fun buildArtifactCacheKey(
        posterId: String,
        version: Int,
        videoUrl: String,
        updatedAt: String?
    ): String {
        val stableUrl = videoUrl.substringBefore("?")
        val fingerprintSource = "$stableUrl:${updatedAt.orEmpty()}"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(fingerprintSource.toByteArray())
            .take(6)
            .joinToString("") { "%02x".format(it) }
        return "${posterId}_v${version}_$digest"
    }
    
    private suspend fun setupVideo(
        posterId: String,
        videoPath: String,
        lipCoordinates: LipCoordinates
    ) {
        withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Setting up video for playback")
                
                // Notify lip coordinates to Compose overlay — GL renderer is bypassed.
                callbacks?.onLipCoordinatesReady(lipCoordinates)

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
        
        // Emit poster screen bounds and directly-projected lip bounds each tracking frame.
        if (trackingData.isTracking) {
            val transform = trackingDataToMatrix(trackingData)
            val m = transform.values
            val tl = ndcToScreen(mvpTransformPoint(m, 0f, 0f))
            val tr = ndcToScreen(mvpTransformPoint(m, 1f, 0f))
            val bl = ndcToScreen(mvpTransformPoint(m, 0f, 1f))
            val br = ndcToScreen(mvpTransformPoint(m, 1f, 1f))
            val left   = minOf(tl.first,  tr.first,  bl.first,  br.first)
            val top    = minOf(tl.second, tr.second, bl.second, br.second)
            val right  = maxOf(tl.first,  tr.first,  bl.first,  br.first)
            val bottom = maxOf(tl.second, tr.second, bl.second, br.second)
            Log.d(TAG, "Corners: tl=$tl tr=$tr bl=$bl br=$br → bounds L=${"%.3f".format(left)} T=${"%.3f".format(top)} R=${"%.3f".format(right)} B=${"%.3f".format(bottom)} W=${"%.3f".format(right-left)} H=${"%.3f".format(bottom-top)} ratio=${"%.3f".format((bottom-top)/(right-left))}")
            callbacks?.onPosterScreenBounds(left, top, right, bottom)

            // Direct lip projection: project the four lip UV corners through MVP to
            // get exact screen positions without the intermediate bounding-box step.
            val lip = session?.lipCoordinates
            if (lip != null) {
                val ltl = ndcToScreen(mvpTransformPoint(m, lip.lipX,               lip.lipY))
                val ltr = ndcToScreen(mvpTransformPoint(m, lip.lipX + lip.lipWidth, lip.lipY))
                val lbl = ndcToScreen(mvpTransformPoint(m, lip.lipX,               lip.lipY + lip.lipHeight))
                val lbr = ndcToScreen(mvpTransformPoint(m, lip.lipX + lip.lipWidth, lip.lipY + lip.lipHeight))
                val lLeft   = minOf(ltl.first,  ltr.first,  lbl.first,  lbr.first)
                val lTop    = minOf(ltl.second, ltr.second, lbl.second, lbr.second)
                val lRight  = maxOf(ltl.first,  ltr.first,  lbl.first,  lbr.first)
                val lBottom = maxOf(ltl.second, ltr.second, lbl.second, lbr.second)
                Log.d(TAG, "LipDirect: L=${"%.3f".format(lLeft)} T=${"%.3f".format(lTop)} R=${"%.3f".format(lRight)} B=${"%.3f".format(lBottom)}")
                callbacks?.onLipScreenBounds(lLeft, lTop, lRight, lBottom)
            }
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

    override fun getVideoPath(): String? {
        return session?.videoPath
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
        val proj = trackingData.projectionMatrix
        if (view != null && proj != null && view.size == 16 && proj.size == 16) {
            // extentX = poster width in metres, extentZ = poster height in metres
            val extX = trackingData.scale.x
            val extZ = trackingData.scale.y
            val hx = extX * 0.5f
            val hz = extZ * 0.5f

            // Rotation matrix from quaternion (standard formula)
            val q = trackingData.rotation
            val qx = q.x; val qy = q.y; val qz = q.z; val qw = q.w
            val xx = qx*qx; val yy = qy*qy; val zz = qz*qz
            val xy = qx*qy; val xz = qx*qz; val yz = qy*qz
            val wx = qw*qx; val wy = qw*qy; val wz = qw*qz
            val r00=1f-2f*(yy+zz); val r10=2f*(xy+wz); val r20=2f*(xz-wy)
            val r01=2f*(xy-wz);   val r11=1f-2f*(xx+zz); val r21=2f*(yz+wx)
            val r02=2f*(xz+wy);   val r12=2f*(yz-wx);    val r22=1f-2f*(xx+yy)
            val tx = trackingData.position.x
            val ty = trackingData.position.y
            val tz = trackingData.position.z

            // Model matrix (column-major) mapping poster UV (u,v,0,1) → world space.
            // ARCore image lies in the local XZ plane: local = ((u-0.5)*extX, 0, (v-0.5)*extZ).
            // Column 0 (u contribution): R * (extX, 0, 0)
            // Column 1 (v contribution): R * (0, 0, extZ)
            // Column 2 (z=0, unused but complete the matrix)
            // Column 3 (constant shift): R * (-hx, 0, -hz) + (tx,ty,tz)
            val model = floatArrayOf(
                r00*extX, r10*extX, r20*extX, 0f,
                r02*extZ, r12*extZ, r22*extZ, 0f,
                r01,      r11,      r21,      0f,
                -r00*hx - r02*hz + tx,
                -r10*hx - r12*hz + ty,
                -r20*hx - r22*hz + tz,
                1f
            )
            // Correct column-major multiplication: MVP = Projection * View * Model
            val vm  = colMajorMul(view, model)
            val pvm = colMajorMul(proj, vm)
            return Matrix4(pvm)
        }
        // Fallback (no view/proj matrices): identity-ish so renderer doesn't crash
        return Matrix4(floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        ))
    }

    // Project a 2D poster-UV point (x, y, z=0, w=1) through the MVP matrix → NDC (±1).
    private fun mvpTransformPoint(m: FloatArray, x: Float, y: Float): Pair<Float, Float> {
        val cx = m[0] * x + m[4] * y + m[12]
        val cy = m[1] * x + m[5] * y + m[13]
        val cw = m[3] * x + m[7] * y + m[15]
        return if (cw != 0f) Pair(cx / cw, cy / cw) else Pair(cx, cy)
    }

    // NDC (±1, Y up) → screen fraction (0..1, Y down).
    private fun ndcToScreen(ndc: Pair<Float, Float>): Pair<Float, Float> =
        Pair((ndc.first + 1f) / 2f, (1f - ndc.second) / 2f)

    // Column-major 4×4 multiply: C = A * B  (C[col*4+row] = Σ A[k*4+row] * B[col*4+k])
    private fun colMajorMul(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (col in 0..3) {
            for (row in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += a[k * 4 + row] * b[col * 4 + k]
                }
                out[col * 4 + row] = sum
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

package com.talkar.app.ui.viewmodels

import android.content.Context
import android.graphics.Matrix
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ar.core.Anchor
import com.talkar.app.ar.video.TalkingPhotoController
import com.talkar.app.ar.video.TalkingPhotoControllerFactory
import com.talkar.app.ar.video.TalkingPhotoCallbacks
import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.LipCoordinates
import com.talkar.app.ar.video.models.TalkingPhotoState
import com.talkar.app.ar.video.models.TrackingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Talking Photo screen.
 * 
 * Manages the state of the talking photo feature and coordinates
 * between the UI and the TalkingPhotoController.
 * 
 * Handles:
 * - Poster detection and tracking
 * - Video generation progress
 * - Playback state
 * - Error handling
 * - Refresh scan functionality
 * 
 * Requirements: 1.1, 2.1, 6.2, 6.3, 8.1, 8.2, 14.1, 14.2, 14.3, 14.4
 */
class TalkingPhotoViewModel(
    private val context: Context
) : ViewModel() {
    companion object {
        private const val TAG = "TalkingPhotoViewModel"
        private const val FLOW_TAG = "PPFlow"
    }
    
    private val controller: TalkingPhotoController by lazy {
        TalkingPhotoControllerFactory.create(context)
    }
    
    // State flows
    private val _state = MutableStateFlow(TalkingPhotoState.IDLE)
    val state: StateFlow<TalkingPhotoState> = _state.asStateFlow()
    
    private val _error = MutableStateFlow<TalkingPhotoError?>(null)
    val error: StateFlow<TalkingPhotoError?> = _error.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()
    
    private val _lipCoordinates = MutableStateFlow<LipCoordinates?>(null)
    val lipCoordinates: StateFlow<LipCoordinates?> = _lipCoordinates.asStateFlow()
    
    private val _transform = MutableStateFlow<Matrix?>(null)
    val transform: StateFlow<Matrix?> = _transform.asStateFlow()

    private val _previewMode = MutableStateFlow("ARCORE")
    val previewMode: StateFlow<String> = _previewMode.asStateFlow()

    private val _posterSource = MutableStateFlow("unknown")
    val posterSource: StateFlow<String> = _posterSource.asStateFlow()

    private val _lastArtifactStatus = MutableStateFlow("idle")
    val lastArtifactStatus: StateFlow<String> = _lastArtifactStatus.asStateFlow()
    
    private val _sessionRecreateTrigger = MutableStateFlow(0)
    val sessionRecreateTrigger: StateFlow<Int> = _sessionRecreateTrigger.asStateFlow()

    /** Lightweight detection-unlock signal — resets poster lock without recreating the session. */
    private val _softResetTrigger = MutableStateFlow(0)
    val softResetTrigger: StateFlow<Int> = _softResetTrigger.asStateFlow()

    private val _loadedPosterCount = MutableStateFlow(0)
    val loadedPosterCount: StateFlow<Int> = _loadedPosterCount.asStateFlow()

    private val _loadedPosterSource = MutableStateFlow("unknown")
    val loadedPosterSource: StateFlow<String> = _loadedPosterSource.asStateFlow()

    private val _detectedPosterName = MutableStateFlow<String?>(null)
    val detectedPosterName: StateFlow<String?> = _detectedPosterName.asStateFlow()

    private val _videoPath = MutableStateFlow<String?>(null)
    val videoPath: StateFlow<String?> = _videoPath.asStateFlow()

    /** Screen-fraction [left, top, right, bottom] of the poster, 0..1. Null when not tracking. */
    private val _posterScreenBounds = MutableStateFlow<FloatArray?>(null)
    val posterScreenBounds: StateFlow<FloatArray?> = _posterScreenBounds.asStateFlow()

    /** Directly-projected lip screen bounds [left, top, right, bottom], 0..1. Null when not tracking. */
    private val _lipScreenBounds = MutableStateFlow<FloatArray?>(null)
    val lipScreenBounds: StateFlow<FloatArray?> = _lipScreenBounds.asStateFlow()
    
    private var currentPosterId: String? = null
    private var currentAnchor: Anchor? = null
    private var smoothedTx = 0f
    private var smoothedTy = 0f
    private var smoothedSx = 1f
    private var smoothedSy = 1f
    private var scanAttemptId: String = "scan-none"
    private var isPreviewReady = false
    private var loadedPosterCountValue = 0
    
    init {
        setupCallbacks()
    }
    
    private fun setupCallbacks() {
        controller.setCallbacks(object : TalkingPhotoCallbacks {
            override fun onGenerationStarted() {
                _generationProgress.value = 0f
            }
            
            override fun onGenerationProgress(progress: Float) {
                _generationProgress.value = progress
            }

            override fun onArtifactStatus(status: String) {
                _lastArtifactStatus.value = status
                _state.value = if (status.startsWith("WAITING_")) {
                    TalkingPhotoState.ARTIFACT_WAITING
                } else {
                    TalkingPhotoState.FETCHING_ARTIFACT
                }
                android.util.Log.d(
                    FLOW_TAG,
                    "scanAttempt=$scanAttemptId artifactStatus=$status posterId=${currentPosterId ?: "none"} source=${_posterSource.value}"
                )
            }
            
            override fun onVideoReady() {
                _videoPath.value = controller.getVideoPath()
                _state.value = TalkingPhotoState.PLAYING
                controller.play()
                android.util.Log.d(FLOW_TAG, "scanAttempt=$scanAttemptId playback=start posterId=${currentPosterId ?: "none"}")
            }
            
            override fun onFirstFrameRendered() {
                // First frame rendered successfully
            }
            
            override fun onPlaybackComplete() {
                // Video playback completed, loop or stop
                controller.play() // Loop playback
            }
            
            override fun onError(error: TalkingPhotoError) {
                _error.value = error
                _videoPath.value = null
                _state.value = TalkingPhotoState.ERROR
                android.util.Log.e(
                    FLOW_TAG,
                    "scanAttempt=$scanAttemptId playback=error posterId=${currentPosterId ?: "none"} source=${_posterSource.value} error=${error.message}"
                )
            }
            
            override fun onTrackingLost() {
                _isTracking.value = false
                _posterScreenBounds.value = null
                _lipScreenBounds.value = null
            }
            
            override fun onTrackingResumed() {
                _isTracking.value = true
            }

            override fun onLipCoordinatesReady(lipCoordinates: LipCoordinates) {
                _lipCoordinates.value = lipCoordinates
            }

            override fun onPosterScreenBounds(left: Float, top: Float, right: Float, bottom: Float) {
                _posterScreenBounds.value = floatArrayOf(left, top, right, bottom)
            }

            override fun onLipScreenBounds(left: Float, top: Float, right: Float, bottom: Float) {
                _lipScreenBounds.value = floatArrayOf(left, top, right, bottom)
            }
        })
    }
    
    /**
     * Called when a poster is detected by ARCore.
     * 
     * Requirement: 1.1, 2.1
     */
    fun onPosterDetected(posterId: String, posterName: String, anchor: Anchor?) {
        if (currentPosterId != null) {
            // Already tracking a poster (single poster mode)
            return
        }

        val detectedName = posterName.takeIf { it.isNotBlank() } ?: posterId

        if (_posterSource.value == "test_fallback" || posterId.startsWith("test_")) {
            _error.value = TalkingPhotoError.GenerationFailed("TEST_FALLBACK_POSTER")
            _state.value = TalkingPhotoState.ERROR_RETRYABLE
            android.util.Log.w(FLOW_TAG, "scanAttempt=$scanAttemptId posterId=$posterId source=test_fallback blocked=non_production")
            return
        }

        controller.stop()
        _videoPath.value = null
        _lastArtifactStatus.value = "idle"
        _generationProgress.value = 0f
        _downloadProgress.value = 0f

        currentPosterId = posterId
        currentAnchor = anchor
        scanAttemptId = "scan-${System.currentTimeMillis()}"
        _detectedPosterName.value = detectedName
        _state.value = TalkingPhotoState.POSTER_DETECTED
        android.util.Log.d(
            FLOW_TAG,
            "scanAttempt=$scanAttemptId posterDetected=true posterId=$posterId posterName=$detectedName source=${_posterSource.value} previewMode=${_previewMode.value}"
        )
        
        viewModelScope.launch {
            val result = controller.initialize(anchor, posterId)
            if (result.isSuccess) {
                _state.value = controller.getState()
                android.util.Log.d(FLOW_TAG, "scanAttempt=$scanAttemptId initializationSuccess=true finalState=${_state.value}")
            } else {
                val exception = result.exceptionOrNull()
                val errorCode = exception?.message ?: "UNKNOWN"
                android.util.Log.e(FLOW_TAG, "scanAttempt=$scanAttemptId initializationFailed=true errorCode=$errorCode", exception)

                if (errorCode == "ARTIFACT_NOT_READY") {
                    _error.value = null
                    _state.value = TalkingPhotoState.ARTIFACT_WAITING
                    android.util.Log.d(FLOW_TAG, "scanAttempt=$scanAttemptId posterDetectedOnly posterId=$posterId posterName=$detectedName")
                    return@launch
                }
                
                _error.value = when (errorCode) {
                    "NO_DEFAULT_SCRIPT" -> TalkingPhotoError.GenerationFailed(
                        "This poster has no default script configured. Please set one in the Admin Dashboard."
                    )
                    "NO_FACE_IN_POSTER" -> TalkingPhotoError.GenerationFailed(
                        "No face detected in the poster. Lip-sync cannot be generated."
                    )
                    "TRACKING_UNSTABLE" -> TalkingPhotoError.GenerationFailed(
                        "Tracking was too unstable. Please hold the phone steady and try again."
                    )
                    else -> TalkingPhotoError.GenerationFailed(
                        "Generation failed ($errorCode). Please check your internet and try again."
                    )
                }
                _state.value = TalkingPhotoState.ERROR_RETRYABLE
            }
        }
    }
    
    /**
     * Called when a poster is lost (out of frame).
     * 
     * Requirement: 8.1
     */
    fun onPosterLost(posterId: String) {
        if (posterId == currentPosterId) {
            _isTracking.value = false
            // Clear screen bounds so overlay doesn't render at stale position
            _posterScreenBounds.value = null
            _lipScreenBounds.value = null
        }
    }

    /** Called when the lip-sync video finishes playing once. Stops and waits for user to press reset. */
    fun onVideoComplete() {
        controller.stop()
        _videoPath.value = null
        _posterScreenBounds.value = null
        _lipScreenBounds.value = null
        _isTracking.value = false
        _state.value = TalkingPhotoState.TRACKING_READY
        // Do NOT trigger softResetTrigger — detection stays locked until user presses reset
    }
    
    /**
     * Called every frame with tracking updates.
     * 
     * Requirement: 7.1, 7.4
     */
    fun onTrackingUpdate(trackingData: TrackingData) {
        controller.updateTracking(trackingData)
        _isTracking.value = trackingData.isTracking
        _transform.value = if (trackingData.isTracking) {
            buildProjectedTransform(trackingData) ?: run {
                val alpha = 0.2f
                smoothedTx = smoothedTx + alpha * ((trackingData.position.x * 100f) - smoothedTx)
                smoothedTy = smoothedTy + alpha * ((trackingData.position.y * 100f) - smoothedTy)
                smoothedSx = smoothedSx + alpha * ((1f + (trackingData.scale.x * 0.1f)) - smoothedSx)
                smoothedSy = smoothedSy + alpha * ((1f + (trackingData.scale.y * 0.1f)) - smoothedSy)

                Matrix().apply {
                    postScale(smoothedSx, smoothedSy)
                    postTranslate(smoothedTx, smoothedTy)
                }
            }
        } else {
            null
        }
        if (currentPosterId != null) {
            _state.value = controller.getState()
        }
    }

    /**
     * Called when an error occurs.
     * 
     * Requirement: 14.1, 14.2, 14.3
     */
    fun onError(errorMessage: String) {
        val current = _state.value
        if (current == TalkingPhotoState.PLAYING ||
            current == TalkingPhotoState.ARTIFACT_WAITING ||
            current == TalkingPhotoState.FETCHING_ARTIFACT
        ) {
            Log.w(TAG, "Ignoring AR error during active session ($current): $errorMessage")
            return
        }
        Log.e(TAG, "❌ AR/UI Error reported: $errorMessage")
        _error.value = TalkingPhotoError.GenerationFailed(errorMessage)
        _state.value = TalkingPhotoState.ERROR_RETRYABLE
    }

    fun onPreviewReady() {
        isPreviewReady = true
        if (_state.value == TalkingPhotoState.IDLE || _state.value == TalkingPhotoState.ERROR_RETRYABLE) {
            _error.value = null
            _state.value = TalkingPhotoState.POSTERS_LOADING
            android.util.Log.d(FLOW_TAG, "✅ Camera preview ready, clearing error state if any")
        }
        android.util.Log.d(FLOW_TAG, "scanAttempt=$scanAttemptId previewReady=true mode=${_previewMode.value}")
    }

    fun onTrackingReady() {
        if (_state.value == TalkingPhotoState.IDLE || _state.value == TalkingPhotoState.PREVIEW_READY || _state.value == TalkingPhotoState.POSTERS_LOADING) {
            _state.value = TalkingPhotoState.TRACKING_READY
        }
    }

    fun onPreviewModeChanged(mode: String) {
        _previewMode.value = mode
        android.util.Log.d(FLOW_TAG, "scanAttempt=$scanAttemptId previewMode=$mode")
    }

    fun onPosterSourceChanged(source: String) {
        _posterSource.value = source
        android.util.Log.d(FLOW_TAG, "scanAttempt=$scanAttemptId posterSource=$source")
    }

    fun onPosterDatabaseReady(count: Int, source: String) {
        _loadedPosterCount.value = count
        loadedPosterCountValue = count
        _loadedPosterSource.value = source
        android.util.Log.d(FLOW_TAG, "posterDatabaseReady count=$count source=$source")
    }
    
    /**
     * Refreshes the scan to allow scanning a new poster.
     * Soft reset: clears only the current poster session without recreating the ARCore session
     * or re-downloading posters. The session and image database remain live.
     */
    fun refreshScan() {
        Log.d(TAG, "Soft-resetting scan state (session stays alive)")
        controller.stop()

        currentPosterId = null
        currentAnchor = null

        _error.value = null
        _isTracking.value = false
        _generationProgress.value = 0f
        _downloadProgress.value = 0f
        _lipCoordinates.value = null
        _transform.value = null
        _posterScreenBounds.value = null
        _lipScreenBounds.value = null
        _lastArtifactStatus.value = "idle"
        _posterSource.value = "unknown"
        _detectedPosterName.value = null
        _videoPath.value = null

        // Go straight back to scanning — posters are already loaded, session is running.
        _state.value = TalkingPhotoState.TRACKING_READY
        _softResetTrigger.value += 1
    }

    /**
     * Forces a complete recreation of the ARCore session.
     * Useful if the camera becomes unresponsive or black.
     */
    fun hardResetAR() {
        Log.w(TAG, "🔄 Hard resetting AR session...")
        _sessionRecreateTrigger.value += 1
    }

    /**
     * Clears current poster/video state and returns to scanning mode.
     * Posters stay cached — no re-download.
     */
    fun fullResetAndReload() {
        Log.w(TAG, "⊘ Reset — clearing playback state, returning to scan")
        controller.stop()
        currentPosterId = null
        currentAnchor = null
        _error.value = null
        _isTracking.value = false
        _generationProgress.value = 0f
        _downloadProgress.value = 0f
        _lipCoordinates.value = null
        _transform.value = null
        _posterScreenBounds.value = null
        _lipScreenBounds.value = null
        _lastArtifactStatus.value = "idle"
        _posterSource.value = "unknown"
        _detectedPosterName.value = null
        _videoPath.value = null
        _state.value = TalkingPhotoState.TRACKING_READY
        _softResetTrigger.value += 1
    }
    
    /**
     * Retries after an error.
     * 
     * Requirement: 14.3
     */
    fun retry() {
        val posterId = currentPosterId
        val anchor = currentAnchor
        
        if (posterId != null && anchor != null) {
            controller.stop()
            _error.value = null
            _state.value = TalkingPhotoState.IDLE
            
            viewModelScope.launch {
                val result = controller.initialize(anchor, posterId)
                if (result.isSuccess) {
                    _state.value = controller.getState()
                } else {
                    _error.value = TalkingPhotoError.GenerationFailed(
                        "Retry failed: ${result.exceptionOrNull()?.message}"
                    )
                    _state.value = TalkingPhotoState.ERROR
                }
            }
        }
    }
    
    /**
     * Clears the current error.
     */
    fun clearError() {
        _error.value = null
        if (_state.value == TalkingPhotoState.ERROR || _state.value == TalkingPhotoState.ERROR_RETRYABLE) {
            _state.value = TalkingPhotoState.IDLE
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        controller.release()
    }

    private fun buildProjectedTransform(trackingData: TrackingData): Matrix? {
        val view = trackingData.viewMatrix ?: return null
        val projection = trackingData.projectionMatrix ?: return null

        val model = buildModelMatrix(trackingData)
        val vp = multiply4x4(projection, view)
        val mvp = multiply4x4(vp, model)

        val topLeft = projectPoint(mvp, 0f, 0f) ?: return null
        val topRight = projectPoint(mvp, 1f, 0f) ?: return null
        val bottomLeft = projectPoint(mvp, 0f, 1f) ?: return null

        val rawSx = (topRight.first - topLeft.first).coerceAtLeast(0.02f)
        val rawSy = (bottomLeft.second - topLeft.second).coerceAtLeast(0.02f)
        val rawTx = topLeft.first
        val rawTy = topLeft.second

        val alpha = 0.22f
        smoothedTx = smoothedTx + alpha * (rawTx - smoothedTx)
        smoothedTy = smoothedTy + alpha * (rawTy - smoothedTy)
        smoothedSx = smoothedSx + alpha * (rawSx - smoothedSx)
        smoothedSy = smoothedSy + alpha * (rawSy - smoothedSy)

        return Matrix().apply {
            postScale(smoothedSx, smoothedSy)
            postTranslate(smoothedTx, smoothedTy)
        }
    }

    private fun buildModelMatrix(trackingData: TrackingData): FloatArray {
        val q = trackingData.rotation
        val x = q.x
        val y = q.y
        val z = q.z
        val w = q.w

        val xx = x * x
        val yy = y * y
        val zz = z * z
        val xy = x * y
        val xz = x * z
        val yz = y * z
        val wx = w * x
        val wy = w * y
        val wz = w * z

        return floatArrayOf(
            1f - 2f * (yy + zz), 2f * (xy + wz), 2f * (xz - wy), 0f,
            2f * (xy - wz), 1f - 2f * (xx + zz), 2f * (yz + wx), 0f,
            2f * (xz + wy), 2f * (yz - wx), 1f - 2f * (xx + yy), 0f,
            trackingData.position.x, trackingData.position.y, trackingData.position.z, 1f
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

    private fun projectPoint(m: FloatArray, u: Float, v: Float): Pair<Float, Float>? {
        val x = u
        val y = v
        val z = 0f
        val w = 1f

        val clipX = m[0] * x + m[4] * y + m[8] * z + m[12] * w
        val clipY = m[1] * x + m[5] * y + m[9] * z + m[13] * w
        val clipW = m[3] * x + m[7] * y + m[11] * z + m[15] * w
        if (clipW == 0f) return null

        val ndcX = (clipX / clipW)
        val ndcY = (clipY / clipW)
        val screenX = (ndcX * 0.5f) + 0.5f
        val screenY = 1f - ((ndcY * 0.5f) + 0.5f)
        return Pair(screenX, screenY)
    }
}

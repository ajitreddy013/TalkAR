package com.talkar.app.ui.viewmodels

import android.content.Context
import android.graphics.Matrix
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        private const val LOST_TRACKING_RESET_TIMEOUT_MS = 12000L
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
    
    private var currentPosterId: String? = null
    private var currentAnchor: Anchor? = null
    private var smoothedTx = 0f
    private var smoothedTy = 0f
    private var smoothedSx = 1f
    private var smoothedSy = 1f
    private var lostTrackingResetJob: Job? = null
    
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
                _state.value = if (status.startsWith("WAITING_")) {
                    TalkingPhotoState.ARTIFACT_WAITING
                } else {
                    TalkingPhotoState.FETCHING_ARTIFACT
                }
            }
            
            override fun onVideoReady() {
                _state.value = TalkingPhotoState.READY
                // Auto-play when ready
                controller.play()
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
                _state.value = TalkingPhotoState.ERROR
            }
            
            override fun onTrackingLost() {
                _isTracking.value = false
                scheduleLostTrackingReset()
            }
            
            override fun onTrackingResumed() {
                _isTracking.value = true
                cancelLostTrackingReset()
            }

            override fun onLipCoordinatesReady(lipCoordinates: LipCoordinates) {
                _lipCoordinates.value = lipCoordinates
            }
        })
    }
    
    /**
     * Called when a poster is detected by ARCore.
     * 
     * Requirement: 1.1, 2.1
     */
    fun onPosterDetected(posterId: String, anchor: Anchor) {
        if (currentPosterId != null) {
            // Already tracking a poster (single poster mode)
            return
        }
        
        currentPosterId = posterId
        currentAnchor = anchor
        _state.value = TalkingPhotoState.POSTER_DETECTED
        
        viewModelScope.launch {
            val result = controller.initialize(anchor, posterId)
            if (result.isSuccess) {
                _state.value = controller.getState()
            } else {
                _error.value = TalkingPhotoError.GenerationFailed(
                    "Failed to initialize: ${result.exceptionOrNull()?.message}"
                )
                _state.value = TalkingPhotoState.ERROR
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
            scheduleLostTrackingReset()
        }
    }
    
    /**
     * Called every frame with tracking updates.
     * 
     * Requirement: 7.1, 7.4
     */
    fun onTrackingUpdate(trackingData: TrackingData) {
        controller.updateTracking(trackingData)
        _isTracking.value = trackingData.isTracking
        if (trackingData.isTracking) {
            cancelLostTrackingReset()
        } else {
            scheduleLostTrackingReset()
        }
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
        _state.value = controller.getState()
    }
    
    /**
     * Called when an error occurs.
     * 
     * Requirement: 14.1, 14.2, 14.3
     */
    fun onError(errorMessage: String) {
        _error.value = TalkingPhotoError.GenerationFailed(errorMessage)
        _state.value = TalkingPhotoState.ERROR_RETRYABLE
    }

    fun onPreviewReady() {
        _state.value = TalkingPhotoState.POSTERS_LOADING
    }

    fun onTrackingReady() {
        if (_state.value == TalkingPhotoState.IDLE || _state.value == TalkingPhotoState.PREVIEW_READY || _state.value == TalkingPhotoState.POSTERS_LOADING) {
            _state.value = TalkingPhotoState.TRACKING_READY
        }
    }
    
    /**
     * Refreshes the scan to allow scanning a new poster.
     * 
     * Requirement: 6.2, 6.3
     */
    fun refreshScan() {
        cancelLostTrackingReset()
        controller.stop()
        controller.release()
        
        currentPosterId = null
        currentAnchor = null
        
        _state.value = TalkingPhotoState.IDLE
        _error.value = null
        _isTracking.value = false
        _generationProgress.value = 0f
        _downloadProgress.value = 0f
        _lipCoordinates.value = null
        _transform.value = null
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
            cancelLostTrackingReset()
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
        cancelLostTrackingReset()
        controller.release()
    }

    private fun scheduleLostTrackingReset() {
        if (currentPosterId == null) return
        if (lostTrackingResetJob?.isActive == true) return
        lostTrackingResetJob = viewModelScope.launch {
            delay(LOST_TRACKING_RESET_TIMEOUT_MS)
            if (!_isTracking.value && currentPosterId != null) {
                refreshScan()
            }
        }
    }

    private fun cancelLostTrackingReset() {
        lostTrackingResetJob?.cancel()
        lostTrackingResetJob = null
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

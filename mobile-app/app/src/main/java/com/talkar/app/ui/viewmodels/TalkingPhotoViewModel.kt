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
            }
            
            override fun onTrackingResumed() {
                _isTracking.value = true
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
        _state.value = controller.getState()
    }
    
    /**
     * Called when an error occurs.
     * 
     * Requirement: 14.1, 14.2, 14.3
     */
    fun onError(errorMessage: String) {
        _error.value = TalkingPhotoError.GenerationFailed(errorMessage)
        _state.value = TalkingPhotoState.ERROR
    }
    
    /**
     * Refreshes the scan to allow scanning a new poster.
     * 
     * Requirement: 6.2, 6.3
     */
    fun refreshScan() {
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
        if (_state.value == TalkingPhotoState.ERROR) {
            _state.value = TalkingPhotoState.IDLE
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        controller.release()
    }
}

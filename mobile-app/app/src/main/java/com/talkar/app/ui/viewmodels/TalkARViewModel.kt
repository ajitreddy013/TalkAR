package com.talkar.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.ar.SpeechRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for TalkAR screen.
 * 
 * Orchestrates the complete AR interaction flow:
 * 1. Image detection
 * 2. Initial video playback (on long-press)
 * 3. Speech recognition (after video completes)
 * 4. Response video playback
 * 
 * @param context Application context for speech recognition
 */
class TalkARViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "TalkARViewModel"
    }
    
    // Speech recognition service
    private val speechService = SpeechRecognitionService(context)
    
    // UI State
    private val _uiState = MutableStateFlow(TalkARUiState())
    val uiState: StateFlow<TalkARUiState> = _uiState.asStateFlow()
    
    init {
        setupSpeechRecognition()
    }
    
    /**
     * Called when an image is detected by ARCore.
     */
    fun onImageDetected(imageName: String) {
        Log.i(TAG, "Image detected: $imageName")
        
        _uiState.value = _uiState.value.copy(
            detectedImage = imageName,
            errorMessage = null,
            interactionState = InteractionState.IMAGE_DETECTED
        )
    }
    
    /**
     * Called when an image is lost (no longer tracked).
     */
    fun onImageLost(imageName: String) {
        Log.i(TAG, "Image lost: $imageName")
        
        // Only clear if it's the currently detected image
        if (_uiState.value.detectedImage == imageName) {
            _uiState.value = _uiState.value.copy(
                detectedImage = null,
                interactionState = InteractionState.IDLE
            )
        }
    }
    
    /**
     * Called when user long-presses on a detected image.
     * Starts the initial video playback.
     */
    fun onImageLongPressed(imageName: String) {
        Log.i(TAG, "Image long-pressed: $imageName")
        
        // TODO: Get video URI from backend based on imageName
        // For now, use a placeholder
        val videoUri = getInitialVideoUri(imageName)
        
        _uiState.value = _uiState.value.copy(
            currentVideoUri = videoUri,
            interactionState = InteractionState.PLAYING_INITIAL_VIDEO
        )
    }
    
    /**
     * Called when the initial video completes.
     * Starts speech recognition for user response.
     */
    fun onInitialVideoCompleted() {
        Log.i(TAG, "Initial video completed, starting speech recognition")
        
        _uiState.value = _uiState.value.copy(
            interactionState = InteractionState.LISTENING_FOR_SPEECH
        )
        
        // Start listening for speech
        viewModelScope.launch {
            speechService.startListening()
        }
    }
    
    /**
     * Called when speech recognition produces a result.
     * Selects and plays the response video.
     */
    private fun onSpeechRecognized(text: String) {
        Log.i(TAG, "Speech recognized: \"$text\"")
        
        _uiState.value = _uiState.value.copy(
            recognizedSpeech = text,
            interactionState = InteractionState.PROCESSING_SPEECH
        )
        
        // TODO: Send text to backend to get response video
        // For now, use a placeholder
        val responseVideoUri = getResponseVideoUri(text)
        
        _uiState.value = _uiState.value.copy(
            currentVideoUri = responseVideoUri,
            interactionState = InteractionState.PLAYING_RESPONSE_VIDEO
        )
    }
    
    /**
     * Called when the response video completes.
     * Returns to ready state for another interaction.
     */
    fun onResponseVideoCompleted() {
        Log.i(TAG, "Response video completed")
        
        _uiState.value = _uiState.value.copy(
            currentVideoUri = null,
            recognizedSpeech = null,
            interactionState = InteractionState.IMAGE_DETECTED
        )
    }
    
    /**
     * Called when an error occurs.
     */
    fun onError(error: String) {
        Log.e(TAG, "Error: $error")
        
        _uiState.value = _uiState.value.copy(
            errorMessage = error,
            interactionState = InteractionState.ERROR
        )
    }
    
    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            interactionState = if (_uiState.value.detectedImage != null) {
                InteractionState.IMAGE_DETECTED
            } else {
                InteractionState.IDLE
            }
        )
    }
    
    /**
     * Sets up speech recognition callbacks.
     */
    private fun setupSpeechRecognition() {
        speechService.onResult = { text ->
            onSpeechRecognized(text)
        }
        
        speechService.onError = { error ->
            onError("Speech recognition error: $error")
        }
        
        speechService.onReadyForSpeech = {
            Log.d(TAG, "Ready for speech input")
        }
        
        speechService.onBeginningOfSpeech = {
            Log.d(TAG, "User started speaking")
        }
        
        speechService.onEndOfSpeech = {
            Log.d(TAG, "User stopped speaking")
        }
    }
    
    /**
     * Gets the initial video URI for a detected image.
     * TODO: Replace with actual backend API call.
     */
    private fun getInitialVideoUri(imageName: String): Uri {
        // Placeholder - in production, fetch from backend
        return Uri.parse("android.resource://${context.packageName}/raw/placeholder_video")
    }
    
    /**
     * Gets the response video URI based on recognized speech.
     * TODO: Replace with actual backend API call.
     */
    private fun getResponseVideoUri(speech: String): Uri {
        // Placeholder - in production, send speech to backend and get response video
        return Uri.parse("android.resource://${context.packageName}/raw/response_video")
    }
    
    override fun onCleared() {
        super.onCleared()
        speechService.cleanup()
    }
}

/**
 * UI state for TalkAR screen.
 */
data class TalkARUiState(
    val detectedImage: String? = null,
    val currentVideoUri: Uri? = null,
    val recognizedSpeech: String? = null,
    val errorMessage: String? = null,
    val interactionState: InteractionState = InteractionState.IDLE
)

/**
 * Interaction states for the AR experience.
 */
enum class InteractionState {
    IDLE,                       // No image detected
    IMAGE_DETECTED,             // Image detected, waiting for user interaction
    PLAYING_INITIAL_VIDEO,      // Playing the first video
    LISTENING_FOR_SPEECH,       // Listening for user speech
    PROCESSING_SPEECH,          // Processing recognized speech
    PLAYING_RESPONSE_VIDEO,     // Playing response video
    ERROR                       // Error state
}

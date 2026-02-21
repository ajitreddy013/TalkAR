package com.talkar.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.R
import com.talkar.app.ar.SpeechRecognitionService
import com.talkar.app.data.api.TalkARApiService
import com.talkar.app.data.api.ApiException
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
    
    // API service for backend communication
    private val apiService = TalkARApiService()
    
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
     * Fetches the initial video from backend and starts playback.
     */
    fun onImageLongPressed(imageName: String) {
        Log.i(TAG, "Image long-pressed: $imageName")
        
        _uiState.value = _uiState.value.copy(
            interactionState = InteractionState.LOADING_VIDEO
        )
        
        // Fetch video from backend
        viewModelScope.launch {
            try {
                val videoResponse = apiService.getInitialVideo(
                    imageName = imageName,
                    language = "en",
                    emotion = "excited"
                )
                
                if (videoResponse.videoUrl.isNotEmpty()) {
                    // Use backend video URL
                    val videoUri = Uri.parse(videoResponse.videoUrl)
                    
                    _uiState.value = _uiState.value.copy(
                        currentVideoUri = videoUri,
                        currentScript = videoResponse.script,
                        interactionState = InteractionState.PLAYING_INITIAL_VIDEO
                    )
                    
                    Log.i(TAG, "Loaded video from backend: ${videoResponse.videoUrl}")
                } else {
                    // Fallback to local video if backend doesn't return URL
                    Log.w(TAG, "Backend returned empty video URL, using local fallback")
                    val localUri = getLocalVideoUri(imageName)
                    
                    _uiState.value = _uiState.value.copy(
                        currentVideoUri = localUri,
                        interactionState = InteractionState.PLAYING_INITIAL_VIDEO
                    )
                }
                
            } catch (e: ApiException) {
                Log.e(TAG, "API error loading video: ${e.message}", e)
                
                // Fallback to local video on error
                val localUri = getLocalVideoUri(imageName)
                
                _uiState.value = _uiState.value.copy(
                    currentVideoUri = localUri,
                    errorMessage = "Using offline video (network error)",
                    interactionState = InteractionState.PLAYING_INITIAL_VIDEO
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading video: ${e.message}", e)
                onError("Failed to load video: ${e.message}")
            }
        }
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
     * Sends query to backend and plays the response video.
     */
    private fun onSpeechRecognized(text: String) {
        Log.i(TAG, "Speech recognized: \"$text\"")
        
        _uiState.value = _uiState.value.copy(
            recognizedSpeech = text,
            interactionState = InteractionState.PROCESSING_SPEECH
        )
        
        // Send query to backend
        viewModelScope.launch {
            try {
                val response = apiService.sendVoiceQuery(text)
                
                if (response.success && response.audioUrl.isNotEmpty()) {
                    // Use backend audio/video URL
                    val responseUri = Uri.parse(response.audioUrl)
                    
                    _uiState.value = _uiState.value.copy(
                        currentVideoUri = responseUri,
                        responseText = response.response,
                        interactionState = InteractionState.PLAYING_RESPONSE_VIDEO
                    )
                    
                    Log.i(TAG, "Playing response from backend: ${response.audioUrl}")
                } else {
                    // Fallback to local video if backend doesn't return URL
                    Log.w(TAG, "Backend returned empty response, using local fallback")
                    val localUri = getLocalVideoUri(_uiState.value.detectedImage ?: "sunrich")
                    
                    _uiState.value = _uiState.value.copy(
                        currentVideoUri = localUri,
                        responseText = response.response,
                        interactionState = InteractionState.PLAYING_RESPONSE_VIDEO
                    )
                }
                
            } catch (e: ApiException) {
                Log.e(TAG, "API error processing speech: ${e.message}", e)
                
                // Fallback to local video on error
                val localUri = getLocalVideoUri(_uiState.value.detectedImage ?: "sunrich")
                
                _uiState.value = _uiState.value.copy(
                    currentVideoUri = localUri,
                    responseText = "Thank you for your response!",
                    errorMessage = "Using offline response (network error)",
                    interactionState = InteractionState.PLAYING_RESPONSE_VIDEO
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing speech: ${e.message}", e)
                onError("Failed to process speech: ${e.message}")
            }
        }
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
     * Gets the local video URI as fallback.
     * Used when backend is unavailable or returns empty URL.
     */
    private fun getLocalVideoUri(imageName: String): Uri {
        // Map image names to video resources
        val videoResId = when (imageName.lowercase()) {
            "sunrich" -> R.raw.sunrich_video
            "tony" -> R.raw.sunrich_video // Using same video for now
            else -> R.raw.sunrich_video // Default fallback
        }
        
        return Uri.parse("android.resource://${context.packageName}/$videoResId")
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
    val currentScript: String? = null,
    val responseText: String? = null,
    val errorMessage: String? = null,
    val interactionState: InteractionState = InteractionState.IDLE
)

/**
 * Interaction states for the AR experience.
 */
enum class InteractionState {
    IDLE,                       // No image detected
    IMAGE_DETECTED,             // Image detected, waiting for user interaction
    LOADING_VIDEO,              // Loading video from backend
    PLAYING_INITIAL_VIDEO,      // Playing the first video
    LISTENING_FOR_SPEECH,       // Listening for user speech
    PROCESSING_SPEECH,          // Processing recognized speech with backend
    PLAYING_RESPONSE_VIDEO,     // Playing response video
    ERROR                       // Error state
}

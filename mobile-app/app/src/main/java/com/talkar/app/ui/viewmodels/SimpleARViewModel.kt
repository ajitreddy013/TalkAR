package com.talkar.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.TalkARApplication
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.SyncRequest
import com.talkar.app.data.models.SyncResponse
import com.talkar.app.data.models.TalkingHeadVideo
import com.google.ar.core.AugmentedImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SimpleARViewModel : ViewModel() {
    
    private val imageRepository = TalkARApplication.instance.imageRepository
    private val syncRepository = TalkARApplication.instance.syncRepository
    
    private val _uiState = MutableStateFlow(SimpleARUiState())
    val uiState: StateFlow<SimpleARUiState> = _uiState.asStateFlow()
    
    private val _recognizedImage = MutableStateFlow<ImageRecognition?>(null)
    val recognizedImage: StateFlow<ImageRecognition?> = _recognizedImage.asStateFlow()
    
    private val _syncVideo = MutableStateFlow<SyncResponse?>(null)
    val syncVideo: StateFlow<SyncResponse?> = _syncVideo.asStateFlow()
    
    private val _talkingHeadVideo = MutableStateFlow<TalkingHeadVideo?>(null)
    val talkingHeadVideo: StateFlow<TalkingHeadVideo?> = _talkingHeadVideo.asStateFlow()
    
    private val _recognizedAugmentedImage = MutableStateFlow<AugmentedImage?>(null)
    val recognizedAugmentedImage: StateFlow<AugmentedImage?> = _recognizedAugmentedImage.asStateFlow()
    
    init {
        // Simplified initialization - no heavy processing
        android.util.Log.d("SimpleARViewModel", "Simple AR ViewModel initialized")
    }
    
    fun recognizeImage(imageRecognition: ImageRecognition) {
        viewModelScope.launch {
            android.util.Log.d("SimpleARViewModel", "recognizeImage called for: ${imageRecognition.name}")
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Update the recognized image
                _recognizedImage.value = imageRecognition
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recognizedImage = imageRecognition
                    )
                }
                
                android.util.Log.d("SimpleARViewModel", "Image set in state: ${imageRecognition.name}")
                
                // Trigger haptic feedback for image detection
                triggerHapticFeedback()
                
                // Load additional image data from repository if needed
                imageRepository.getImageById(imageRecognition.id).collect { fullImageData ->
                    _recognizedImage.value = fullImageData
                    _uiState.update {
                        it.copy(
                            recognizedImage = fullImageData
                        )
                    }
                    android.util.Log.d("SimpleARViewModel", "Full image data loaded: ${fullImageData?.name}")
                }
                
                // Automatically fetch talking head video for this image
                android.util.Log.d("SimpleARViewModel", "About to fetch talking head video for: ${imageRecognition.id}")
                fetchTalkingHeadVideo(imageRecognition.id)
            } catch (e: Exception) {
                android.util.Log.e("SimpleARViewModel", "Error in recognizeImage", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to process recognized image: ${e.message}"
                )
            }
        }
    }
    
    private fun triggerHapticFeedback() {
        // This would trigger haptic feedback when an image is detected
        android.util.Log.d("SimpleARViewModel", "Image detected - triggering haptic feedback")
    }
    
    private fun fetchTalkingHeadVideo(imageId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SimpleARViewModel", "Fetching talking head video for image: $imageId")
                
                // Call the API to get the pre-saved talking head video
                val response = TalkARApplication.instance.apiClient.getTalkingHeadVideo(imageId)
                
                if (response.isSuccessful) {
                    val talkingHeadVideo = response.body()
                    if (talkingHeadVideo != null) {
                        _talkingHeadVideo.value = talkingHeadVideo
                        android.util.Log.d("SimpleARViewModel", "Talking head video loaded: ${talkingHeadVideo.title}")
                        android.util.Log.d("SimpleARViewModel", "Video URL: ${talkingHeadVideo.videoUrl}")
                    } else {
                        android.util.Log.e("SimpleARViewModel", "Talking head video response body is null")
                    }
                } else {
                    android.util.Log.e("SimpleARViewModel", "Failed to fetch talking head video: ${response.code()}")
                    android.util.Log.e("SimpleARViewModel", "Error message: ${response.message()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SimpleARViewModel", "Error fetching talking head video", e)
            }
        }
    }
    
    fun setRecognizedAugmentedImage(augmentedImage: AugmentedImage) {
        _recognizedAugmentedImage.value = augmentedImage
    }
    
    fun generateSyncVideo(text: String, language: String, voiceId: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingVideo = true)
            
            val request = SyncRequest(
                text = text,
                language = language,
                voiceId = voiceId
            )
            
            try {
                syncRepository.generateSyncVideo(request).collect { response ->
                    _syncVideo.value = response
                    _uiState.update {
                        it.copy(
                            isGeneratingVideo = false,
                            syncVideo = response
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingVideo = false,
                    error = "Failed to generate sync video: ${e.message}"
                )
            }
        }
    }
    
    fun setError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(error = errorMessage)
    }
    
    fun setArError(errorMessage: String) {
        _uiState.value = _uiState.value.copy(arError = errorMessage)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, arError = null)
    }
    
    fun resetRecognition() {
        _recognizedImage.value = null
        _recognizedAugmentedImage.value = null
        _talkingHeadVideo.value = null
        _syncVideo.value = null
        _uiState.value = _uiState.value.copy(
            recognizedImage = null,
            syncVideo = null,
            error = null,
            arError = null
        )
    }
}

data class SimpleARUiState(
    val isLoading: Boolean = false,
    val isGeneratingVideo: Boolean = false,
    val images: List<ImageRecognition> = emptyList(),
    val recognizedImage: ImageRecognition? = null,
    val syncVideo: SyncResponse? = null,
    val error: String? = null,
    val arError: String? = null
)

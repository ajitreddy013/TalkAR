package com.talkar.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.TalkARApplication
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.SyncRequest
import com.talkar.app.data.models.SyncResponse
import com.talkar.app.data.models.TalkingHeadVideo
import com.talkar.app.data.services.ARImageRecognitionService
import com.google.ar.core.AugmentedImage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ARViewModel : ViewModel() {
    
    private val imageRepository = TalkARApplication.instance.imageRepository
    private val syncRepository = TalkARApplication.instance.syncRepository
    private val arService = TalkARApplication.instance.arImageRecognitionService
    
    private val _uiState = MutableStateFlow(ARUiState())
    val uiState: StateFlow<ARUiState> = _uiState.asStateFlow()
    
    private val _recognizedImage = MutableStateFlow<ImageRecognition?>(null)
    val recognizedImage: StateFlow<ImageRecognition?> = _recognizedImage.asStateFlow()
    
    private val _syncVideo = MutableStateFlow<SyncResponse?>(null)
    val syncVideo: StateFlow<SyncResponse?> = _syncVideo.asStateFlow()
    
    private val _talkingHeadVideo = MutableStateFlow<TalkingHeadVideo?>(null)
    val talkingHeadVideo: StateFlow<TalkingHeadVideo?> = _talkingHeadVideo.asStateFlow()
    
    private val _recognizedAugmentedImage = MutableStateFlow<AugmentedImage?>(null)
    val recognizedAugmentedImage: StateFlow<AugmentedImage?> = _recognizedAugmentedImage.asStateFlow()
    
    init {
        loadImages()
    }
    
    private fun loadImages() {
        viewModelScope.launch {
            try {
                imageRepository.getAllImages().collect { images ->
                    _uiState.update { it.copy(images = images) }
                    
                    // Load images into ARCore database for recognition
                    if (images.isNotEmpty()) {
                        android.util.Log.d("ARViewModel", "Loading ${images.size} images into ARCore database")
                        arService.loadImagesFromAPI(images)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load images: ${e.message}"
                )
            }
        }
    }
    
    fun recognizeImage(imageRecognition: ImageRecognition) {
        viewModelScope.launch {
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
                }
                
                // Automatically fetch talking head video for this image
                fetchTalkingHeadVideo(imageRecognition.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to process recognized image: ${e.message}"
                )
            }
        }
    }
    
    private fun triggerHapticFeedback() {
        // This would trigger haptic feedback when an image is detected
        // Implementation depends on the platform
        android.util.Log.d("ARViewModel", "Image detected - triggering haptic feedback")
    }
    
    private fun fetchTalkingHeadVideo(imageId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ARViewModel", "Fetching talking head video for image: $imageId")
                
                // Find the image in our local cache to get the language
                val image = _uiState.value.images.find { it.id == imageId }
                val language = image?.dialogues?.firstOrNull()?.language ?: "en"
                
                // Call the API to get the pre-saved talking head video with language support
                val response = TalkARApplication.instance.apiClient.getTalkingHeadVideo(imageId, language)
                
                if (response.isSuccessful) {
                    val talkingHeadVideo = response.body()
                    if (talkingHeadVideo != null) {
                        _talkingHeadVideo.value = talkingHeadVideo
                        android.util.Log.d("ARViewModel", "Talking head video loaded: ${talkingHeadVideo.title}")
                    }
                } else {
                    android.util.Log.e("ARViewModel", "Failed to fetch talking head video: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ARViewModel", "Error fetching talking head video", e)
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
                    error = e.message
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setArError(error: String) {
        _uiState.value = _uiState.value.copy(error = error)
    }

    
    fun resetRecognition() {
        _recognizedImage.value = null
        _syncVideo.value = null
        _recognizedAugmentedImage.value = null
        _uiState.update {
            it.copy(
                recognizedImage = null,
                syncVideo = null
            )
        }
    }
}

data class ARUiState(
    val images: List<ImageRecognition> = emptyList(),
    val isLoading: Boolean = false,
    val isGeneratingVideo: Boolean = false,
    val recognizedImage: ImageRecognition? = null,
    val syncVideo: SyncResponse? = null,
    val error: String? = null
)


package com.talkar.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.TalkARApplication
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.SyncRequest
import com.talkar.app.data.models.SyncResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ARViewModel : ViewModel() {
    
    private val imageRepository = TalkARApplication.instance.imageRepository
    private val syncRepository = TalkARApplication.instance.syncRepository
    
    private val _uiState = MutableStateFlow(ARUiState())
    val uiState: StateFlow<ARUiState> = _uiState.asStateFlow()
    
    private val _recognizedImage = MutableStateFlow<ImageRecognition?>(null)
    val recognizedImage: StateFlow<ImageRecognition?> = _recognizedImage.asStateFlow()
    
    private val _syncVideo = MutableStateFlow<SyncResponse?>(null)
    val syncVideo: StateFlow<SyncResponse?> = _syncVideo.asStateFlow()
    
    init {
        loadImages()
    }
    
    private fun loadImages() {
        viewModelScope.launch {
            imageRepository.getAllImages().collect { images ->
                _uiState.value = _uiState.value.copy(images = images)
            }
        }
    }
    
    fun recognizeImage(imageId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            imageRepository.getImageById(imageId).collect { image ->
                _recognizedImage.value = image
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    recognizedImage = image
                )
            }
        }
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
                    _uiState.value = _uiState.value.copy(
                        isGeneratingVideo = false,
                        syncVideo = response
                    )
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
    
    fun resetRecognition() {
        _recognizedImage.value = null
        _syncVideo.value = null
        _uiState.value = _uiState.value.copy(
            recognizedImage = null,
            syncVideo = null
        )
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


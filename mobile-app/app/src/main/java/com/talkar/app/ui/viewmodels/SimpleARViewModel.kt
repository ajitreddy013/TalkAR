package com.talkar.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.TalkARApplication
import android.content.pm.ApplicationInfo
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.SyncRequest
import com.talkar.app.data.models.SyncResponse
import com.talkar.app.data.models.TalkingHeadVideo
import com.talkar.app.data.config.ApiConfig
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
        // Lightweight initialization - defer heavy work
        android.util.Log.d("SimpleARViewModel", "Simple AR ViewModel initialized")
        
        // Initialize repositories in background to avoid blocking UI
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Pre-warm repositories in background
                android.util.Log.d("SimpleARViewModel", "Pre-warming repositories in background")
                // No heavy initialization here - just logging
            } catch (e: Exception) {
                android.util.Log.e("SimpleARViewModel", "Error in background initialization", e)
            }
        }
    }
    
    fun recognizeImage(imageRecognition: ImageRecognition) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            android.util.Log.d("SimpleARViewModel", "recognizeImage called for: ${imageRecognition.name}")
            
            // Immediate UI update on main thread
            _recognizedImage.value = imageRecognition
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                recognizedImage = imageRecognition
            )
            
            android.util.Log.d("SimpleARViewModel", "Image set in state: ${imageRecognition.name}")
            
            // Trigger haptic feedback for image detection
            triggerHapticFeedback()
            
            // Check if detected image matches any backend image
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    android.util.Log.d("SimpleARViewModel", "Checking if image exists in backend: ${imageRecognition.name}")
                    
                    // Get all images from backend
                    val backendImages = TalkARApplication.instance.apiClient.getImages()
                    
                    if (backendImages.isSuccessful) {
                        val images = backendImages.body()
                        android.util.Log.d("SimpleARViewModel", "Backend has ${images?.size ?: 0} images")
                        
                        // Check if any backend image matches the detected image
                        // Prefer deterministic matching by ID. Only allow brittle name-contains or hard-coded test ID
                        // when the app is built in DEBUG mode (developer fallback).
                        val matchingImage = images?.find { backendImage ->
                            // Deterministic match: backend-provided ID must equal recognized image ID
                            val idMatch = backendImage.id == imageRecognition.id

                            // Debug-only fallback: name contains match or hard-coded test image id.
                            val isDebuggable = try {
                                (TalkARApplication.instance.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                            } catch (e: Exception) {
                                false
                            }

                            val debugFallback = if (isDebuggable) {
                                backendImage.name.contains(imageRecognition.name, ignoreCase = true) ||
                                    backendImage.id == "57c37559-e257-4c77-a93b-8ada45761586"
                            } else {
                                false
                            }

                            idMatch || debugFallback
                        }
                        
                        if (matchingImage != null) {
                            android.util.Log.d("SimpleARViewModel", "✅ Image MATCHED in backend: ${matchingImage.name}")
                            
                            // Update UI on main thread
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = null
                                )
                            }
                            
                            // Generate lip sync video only if image exists in backend
                            android.util.Log.d("SimpleARViewModel", "Generating lip sync video for matched image: ${matchingImage.name}")
                            generateLipSyncVideoForMatchedImage(matchingImage)
                            
                        } else {
                            android.util.Log.d("SimpleARViewModel", "❌ Image NOT FOUND in backend: ${imageRecognition.name}")
                            
                            // Update UI on main thread
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Image '${imageRecognition.name}' not found in backend database. No lip sync video will be generated."
                                )
                            }
                        }
                    } else {
                        android.util.Log.e("SimpleARViewModel", "Failed to fetch backend images: ${backendImages.code()}")
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to connect to backend. Cannot verify image."
                            )
                        }
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("SimpleARViewModel", "Error in recognizeImage", e)
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to process recognized image: ${e.message}"
                        )
                    }
                }
            }
        }
    }
    
    private fun triggerHapticFeedback() {
        // This would trigger haptic feedback when an image is detected
        android.util.Log.d("SimpleARViewModel", "Image detected - triggering haptic feedback")
    }
    
    private fun fetchTalkingHeadVideo(imageId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("SimpleARViewModel", "Fetching talking head video for image: $imageId")
                
                // Make real API call to backend for lip sync video generation
                val response = TalkARApplication.instance.apiClient.getTalkingHeadVideo(imageId)
                
                if (response.isSuccessful) {
                    val talkingHeadVideo = response.body()
                    if (talkingHeadVideo != null) {
                        // Update UI on main thread
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            _talkingHeadVideo.value = talkingHeadVideo
                            android.util.Log.d("SimpleARViewModel", "Talking head video loaded: ${talkingHeadVideo.title}")
                            android.util.Log.d("SimpleARViewModel", "Video URL: ${talkingHeadVideo.videoUrl}")
                        }
                    } else {
                        android.util.Log.e("SimpleARViewModel", "Talking head video response body is null")
                        // Fallback to mock data if API fails
                        createMockVideo(imageId)
                    }
                } else {
                    android.util.Log.e("SimpleARViewModel", "Failed to fetch talking head video: ${response.code()}")
                    android.util.Log.e("SimpleARViewModel", "Error message: ${response.message()}")
                    // Fallback to mock data if API fails
                    createMockVideo(imageId)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SimpleARViewModel", "Error fetching talking head video", e)
                // Fallback to mock data if API fails
                createMockVideo(imageId)
            }
        }
    }
    
    private fun generateLipSyncVideoForMatchedImage(backendImage: com.talkar.app.data.models.BackendImage) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("SimpleARViewModel", "Generating lip sync video for backend image: ${backendImage.name}")
                
                // Get the dialogue/script for this image
                val dialogue = backendImage.dialogues.firstOrNull()
                val scriptText = dialogue?.text ?: "Hello! I am ${backendImage.name}."
                val language = dialogue?.language ?: "en"
                val voiceId = dialogue?.voiceId ?: "voice-1"
                
                android.util.Log.d("SimpleARViewModel", "Using script: '$scriptText'")
                android.util.Log.d("SimpleARViewModel", "Language: $language, Voice: $voiceId")
                
                // Create sync request for lip sync video generation
                val syncRequest = SyncRequest(
                    text = scriptText,
                    language = language,
                    voiceId = voiceId,
                    imageUrl = ApiConfig.getFullImageUrl(backendImage.imageUrl)
                )
                
                android.util.Log.d("SimpleARViewModel", "Sending sync request with image URL: ${syncRequest.imageUrl}")
                
                // Call the sync API to generate lip sync video
                val response = TalkARApplication.instance.apiClient.generateSyncVideo(syncRequest)
                
                if (response.isSuccessful) {
                    val syncResponse = response.body()
                    if (syncResponse != null && syncResponse.videoUrl != null && syncResponse.duration != null) {
                        android.util.Log.d("SimpleARViewModel", "✅ Lip sync video generated successfully!")
                        android.util.Log.d("SimpleARViewModel", "Video URL: ${syncResponse.videoUrl}")
                        android.util.Log.d("SimpleARViewModel", "Duration: ${syncResponse.duration}s")
                        
                        // Create talking head video from sync response
                        val talkingHeadVideo = TalkingHeadVideo(
                            imageId = backendImage.id,
                            videoUrl = syncResponse.videoUrl,
                            duration = syncResponse.duration.toInt(),
                            title = "${backendImage.name} Lip Sync Video",
                            description = "AI-generated lip sync video for ${backendImage.name}",
                            language = language,
                            voiceId = voiceId,
                            createdAt = System.currentTimeMillis().toString()
                        )
                        
                        // Update UI on main thread
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            _talkingHeadVideo.value = talkingHeadVideo
                            _syncVideo.value = syncResponse
                            android.util.Log.d("SimpleARViewModel", "Talking head video loaded: ${talkingHeadVideo.title}")
                        }
                    } else {
                        android.util.Log.e("SimpleARViewModel", "Sync response body is null")
                        createMockVideo(backendImage.id)
                    }
                } else {
                    android.util.Log.e("SimpleARViewModel", "Failed to generate lip sync video: ${response.code()}")
                    android.util.Log.e("SimpleARViewModel", "Error message: ${response.message()}")
                    createMockVideo(backendImage.id)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SimpleARViewModel", "Error generating lip sync video", e)
                createMockVideo(backendImage.id)
            }
        }
    }
    
    private fun generateLipSyncVideo(imageRecognition: ImageRecognition) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("SimpleARViewModel", "Generating lip sync video for: ${imageRecognition.name}")
                
                // Get the first dialogue text for lip sync
                val dialogueText = imageRecognition.dialogues.firstOrNull()?.text ?: "Hello! I'm a ${imageRecognition.name}."
                
                // Create sync request for lip sync video generation
                val syncRequest = SyncRequest(
                    text = dialogueText,
                    language = "en",
                    voiceId = "voice_001"
                )
                
                android.util.Log.d("SimpleARViewModel", "Sending sync request: $dialogueText")
                
                // Call the sync API to generate lip sync video
                val response = TalkARApplication.instance.apiClient.generateSyncVideo(syncRequest)
                
                if (response.isSuccessful) {
                    val syncResponse = response.body()
                    if (syncResponse != null && syncResponse.videoUrl != null && syncResponse.duration != null) {
                        android.util.Log.d("SimpleARViewModel", "Lip sync video generated successfully!")
                        android.util.Log.d("SimpleARViewModel", "Video URL: ${syncResponse.videoUrl}")
                        android.util.Log.d("SimpleARViewModel", "Duration: ${syncResponse.duration}s")
                        
                        // Create talking head video from sync response
                        val talkingHeadVideo = TalkingHeadVideo(
                            imageId = imageRecognition.id,
                            videoUrl = syncResponse.videoUrl,
                            duration = syncResponse.duration.toInt(),
                            title = "${imageRecognition.name} Lip Sync Video",
                            description = "AI-generated lip sync video for ${imageRecognition.name}",
                            language = "en",
                            voiceId = "voice_001",
                            createdAt = System.currentTimeMillis().toString()
                        )
                        
                        // Update UI on main thread
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            _talkingHeadVideo.value = talkingHeadVideo
                            _syncVideo.value = syncResponse
                            android.util.Log.d("SimpleARViewModel", "Talking head video loaded: ${talkingHeadVideo.title}")
                        }
                    } else {
                        android.util.Log.e("SimpleARViewModel", "Sync response body is null")
                        createMockVideo(imageRecognition.id)
                    }
                } else {
                    android.util.Log.e("SimpleARViewModel", "Failed to generate lip sync video: ${response.code()}")
                    android.util.Log.e("SimpleARViewModel", "Error message: ${response.message()}")
                    createMockVideo(imageRecognition.id)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("SimpleARViewModel", "Error generating lip sync video", e)
                createMockVideo(imageRecognition.id)
            }
        }
    }
    
    private fun createMockVideo(imageId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val mockVideo = TalkingHeadVideo(
                imageId = imageId,
                videoUrl = "https://example.com/talking_head_water_bottle.mp4",
                duration = 30,
                title = "Water Bottle Talking Head (Mock)",
                description = "AI-generated talking head video for water bottle",
                language = "en",
                voiceId = "voice_001",
                createdAt = System.currentTimeMillis().toString()
            )
            _talkingHeadVideo.value = mockVideo
            android.util.Log.d("SimpleARViewModel", "Using mock video: ${mockVideo.title}")
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

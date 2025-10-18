package com.talkar.app.ui.viewmodels

import com.talkar.app.utils.HapticFeedbackUtil
import com.talkar.app.performance.PerformanceMetrics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.TalkARApplication
import com.talkar.app.ar.AvatarManager
import com.talkar.app.data.models.AvatarModel3D
import com.talkar.app.data.models.AvatarType
import com.talkar.app.data.models.Gender
import com.talkar.app.data.models.IdleAnimation
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
    private val videoCacheManager = TalkARApplication.instance.videoCacheManager
    
    // 3D Avatar Manager (lazy initialization)
    private var _avatarManager: AvatarManager? = null
    
    // Avatar loading state
    private val _avatarLoadingState = MutableStateFlow<AvatarLoadState>(AvatarLoadState.Idle)
    val avatarLoadingState: StateFlow<AvatarLoadState> = _avatarLoadingState.asStateFlow()
    
    private val _uiState = MutableStateFlow(SimpleARUiState())
    val uiState: StateFlow<SimpleARUiState> = _uiState.asStateFlow()
    
    private val _recognizedImage = MutableStateFlow<ImageRecognition?>(null)
    val recognizedImage: StateFlow<ImageRecognition?> = _recognizedImage.asStateFlow()
    
    private val _syncVideo = MutableStateFlow<SyncResponse?>(null)
    val syncVideo: StateFlow<SyncResponse?> = _syncVideo.asStateFlow()
    
    private val _talkingHeadVideo = MutableStateFlow<TalkingHeadVideo?>(null)
    val talkingHeadVideo: StateFlow<TalkingHeadVideo?> = _talkingHeadVideo.asStateFlow()
    
    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()
    
    private val _isLoadingVideo = MutableStateFlow(false)
    val isLoadingVideo: StateFlow<Boolean> = _isLoadingVideo.asStateFlow()
    
    private val _recognizedAugmentedImage = MutableStateFlow<AugmentedImage?>(null)
    val recognizedAugmentedImage: StateFlow<AugmentedImage?> = _recognizedAugmentedImage.asStateFlow()

    // Simple debounce/deduplication to avoid repeated detection events flooding logs
    // and triggering duplicate backend work when AR repeatedly detects the same target.
    private val detectionCooldownMillis = 2_000L // 2 seconds cooldown per image
    private val lastDetectionTimestamp = mutableMapOf<String, Long>()
    private var currentlyProcessingImageId: String? = null
    
    init {
        // Lightweight initialization - defer heavy work
        android.util.Log.d("SimpleARViewModel", "Simple AR ViewModel initialized")
        
        // Initialize repositories in background to avoid blocking UI
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Pre-warm repositories in background
                android.util.Log.d("SimpleARViewModel", "Pre-warming repositories in background")
                
                // Initialize default avatar configurations
                initializeDefaultAvatars()
                
            } catch (e: Exception) {
                android.util.Log.e("SimpleARViewModel", "Error in background initialization", e)
            }
        }
    }
    
    /**
     * Initialize AvatarManager (called when AR scene is ready)
     */
    fun initializeAvatarManager(context: android.content.Context) {
        if (_avatarManager == null) {
            _avatarManager = AvatarManager(context)
            android.util.Log.d("SimpleARViewModel", "AvatarManager initialized")
            
            // Register default avatars
            registerDefaultAvatars()
        }
    }
    
    /**
     * Get AvatarManager instance
     */
    fun getAvatarManager(): AvatarManager? {
        return _avatarManager
    }
    
    /**
     * Initialize default avatar configurations
     */
    private suspend fun initializeDefaultAvatars() {
        android.util.Log.d("SimpleARViewModel", "Initializing default avatar configurations...")
        // Avatars will be registered when AvatarManager is created
    }
    
    /**
     * Register default avatars in the manager
     */
    private fun registerDefaultAvatars() {
        val avatarManager = _avatarManager ?: return
        
        // Define sample avatar configurations
        val avatars = listOf(
            // Generic male presenter
            AvatarModel3D(
                id = "avatar_generic_male_1",
                name = "Generic Male Presenter",
                description = "Professional male presenter avatar",
                modelUrl = null, // Will be loaded from res/raw when models are added
                scale = 0.3f, // Scale down for AR scene
                idleAnimation = IdleAnimation.BREATHING_AND_BLINKING,
                avatarType = AvatarType.GENERIC,
                gender = Gender.MALE,
                isActive = true
            ),
            
            // Generic female presenter
            AvatarModel3D(
                id = "avatar_generic_female_1",
                name = "Generic Female Presenter",
                description = "Professional female presenter avatar",
                modelUrl = null,
                scale = 0.3f,
                idleAnimation = IdleAnimation.BREATHING_AND_BLINKING,
                avatarType = AvatarType.GENERIC,
                gender = Gender.FEMALE,
                isActive = true
            ),
            
            // Celebrity-style male avatar (e.g., SRK-style)
            AvatarModel3D(
                id = "avatar_celebrity_male_srk",
                name = "Celebrity Male Avatar (SRK Style)",
                description = "Celebrity-style male avatar inspired by Bollywood",
                modelUrl = null,
                scale = 0.3f,
                idleAnimation = IdleAnimation.BREATHING_AND_BLINKING,
                avatarType = AvatarType.CELEBRITY,
                gender = Gender.MALE,
                isActive = true
            )
        )
        
        avatarManager.registerAvatars(avatars)
        android.util.Log.d("SimpleARViewModel", "Registered ${avatars.size} default avatars")
    }
    
    /**
     * Map detected image to appropriate avatar
     */
    private fun mapImageToAvatar(imageId: String, imageName: String) {
        val avatarManager = _avatarManager ?: return
        
        // Map based on image name/type
        val avatarId = when {
            imageName.contains("srk", ignoreCase = true) || 
            imageName.contains("celebrity", ignoreCase = true) -> {
                "avatar_celebrity_male_srk"
            }
            imageName.contains("female", ignoreCase = true) -> {
                "avatar_generic_female_1"
            }
            else -> {
                "avatar_generic_male_1"
            }
        }
        
        avatarManager.mapImageToAvatar(imageId, avatarId)
        android.util.Log.d("SimpleARViewModel", "Mapped image '$imageName' to avatar '$avatarId'")
    }
    
    fun recognizeImage(imageRecognition: ImageRecognition) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val detectionStartTime = PerformanceMetrics.startTiming()
            android.util.Log.d("SimpleARViewModel", "recognizeImage called for: ${imageRecognition.name}")
            // Deduplicate rapid repeated detections for the same image
            val now = System.currentTimeMillis()
            val key = imageRecognition.id
            val last = lastDetectionTimestamp[key] ?: 0L
            if (now - last < detectionCooldownMillis) {
                android.util.Log.d(
                    "SimpleARViewModel",
                    "Duplicate detection ignored for '${imageRecognition.name}' (id=$key). Time since last: ${now - last}ms"
                )
                return@launch
            }

            // If we are already processing this image (network/generation) avoid starting again
            if (currentlyProcessingImageId == key) {
                android.util.Log.d(
                    "SimpleARViewModel",
                    "Already processing image '${imageRecognition.name}' (id=$key) - skipping duplicate detection"
                )
                // update timestamp to avoid noisy repeats
                lastDetectionTimestamp[key] = now
                return@launch
            }

            // record this detection time
            lastDetectionTimestamp[key] = now

            // Immediate UI update on main thread
            _recognizedImage.value = imageRecognition
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                recognizedImage = imageRecognition
            )
            
            android.util.Log.d("SimpleARViewModel", "Image set in state: ${imageRecognition.name}")
            
            // Trigger haptic feedback for image detection
            triggerHapticFeedback()
            
            // Record image detection latency
            PerformanceMetrics.recordImageDetectionLatency(detectionStartTime)
            
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
                            
                            // Map image to avatar for 3D rendering
                            mapImageToAvatar(matchingImage.id, matchingImage.name)
                            
                            // Update UI on main thread
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = true,
                                    error = null
                                )
                                _isLoadingVideo.value = true
                                _avatarLoadingState.value = AvatarLoadState.LoadingAvatar(matchingImage.id)
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
        // Trigger haptic feedback when an image is detected
        android.util.Log.d("SimpleARViewModel", "Image detected - triggering haptic feedback")
        try {
            HapticFeedbackUtil.onImageDetected(TalkARApplication.instance.applicationContext)
        } catch (e: Exception) {
            android.util.Log.w("SimpleARViewModel", "Failed to trigger haptic feedback: ${e.message}")
        }
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
            val apiStartTime = PerformanceMetrics.startTiming()
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
                
                // Record API call latency
                PerformanceMetrics.recordApiCallLatency(apiStartTime, "/sync/generate")
                
                if (response.isSuccessful) {
                    val videoLoadStartTime = PerformanceMetrics.startTiming()
                    val syncResponse = response.body()
                    if (syncResponse != null && syncResponse.videoUrl != null && syncResponse.duration != null) {
                        android.util.Log.d("SimpleARViewModel", "✅ Lip sync video generated successfully!")
                        android.util.Log.d("SimpleARViewModel", "Video URL: ${syncResponse.videoUrl}")
                        android.util.Log.d("SimpleARViewModel", "Duration: ${syncResponse.duration}s")
                        
                        // Cache the video in background
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                videoCacheManager.cacheVideo(syncResponse.videoUrl, backendImage.id)
                                android.util.Log.d("SimpleARViewModel", "Video cached successfully")
                                
                                // Preload next script's video if available
                                preloadNextScriptVideo(backendImage)
                            } catch (e: Exception) {
                                android.util.Log.w("SimpleARViewModel", "Failed to cache video: ${e.message}")
                            }
                        }
                        
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
                            // Check if video is cached, use cached version if available
                            val cachedPath = videoCacheManager.getCachedVideoPath(syncResponse.videoUrl)
                            val videoUrl = if (cachedPath != null) {
                                android.util.Log.d("SimpleARViewModel", "Using cached video: $cachedPath")
                                PerformanceMetrics.recordCacheHit("video")
                                "file://$cachedPath"
                            } else {
                                PerformanceMetrics.recordCacheMiss("video")
                                syncResponse.videoUrl
                            }
                            
                            // Record video load latency
                            PerformanceMetrics.recordVideoLoadLatency(videoLoadStartTime, videoUrl)
                            
                            _talkingHeadVideo.value = talkingHeadVideo
                            _syncVideo.value = syncResponse
                            _currentVideoUrl.value = videoUrl
                            _isLoadingVideo.value = false
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            android.util.Log.d("SimpleARViewModel", "Talking head video loaded: ${talkingHeadVideo.title}")
                            android.util.Log.d("SimpleARViewModel", "Video URL set for playback: $videoUrl")
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
            _currentVideoUrl.value = mockVideo.videoUrl
            android.util.Log.d("SimpleARViewModel", "Using mock video: ${mockVideo.title}")
        }
    }
    
    /**
     * Preload next script's video in background
     * Fetches the next dialogue and generates/caches its lip-sync video
     */
    private suspend fun preloadNextScriptVideo(backendImage: com.talkar.app.data.models.BackendImage) {
        try {
            if (backendImage.dialogues.size > 1) {
                // Get the next dialogue (assuming sequential order)
                val nextDialogue = backendImage.dialogues.getOrNull(1)
                if (nextDialogue != null) {
                    android.util.Log.d("SimpleARViewModel", "Preloading next script video...")
                    
                    // Create sync request for next dialogue
                    val syncRequest = SyncRequest(
                        text = nextDialogue.text,
                        language = nextDialogue.language,
                        voiceId = nextDialogue.voiceId,
                        imageUrl = ApiConfig.getFullImageUrl(backendImage.imageUrl)
                    )
                    
                    // Generate video (this will be async on backend)
                    val response = TalkARApplication.instance.apiClient.generateSyncVideo(syncRequest)
                    
                    if (response.isSuccessful) {
                        val syncResponse = response.body()
                        if (syncResponse != null && syncResponse.videoUrl != null) {
                            android.util.Log.d("SimpleARViewModel", "Preloading video URL: ${syncResponse.videoUrl}")
                            // Cache the preloaded video
                            videoCacheManager.preloadVideo(syncResponse.videoUrl, backendImage.id)
                            android.util.Log.d("SimpleARViewModel", "✅ Next script video preloaded and cached")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("SimpleARViewModel", "Failed to preload next video: ${e.message}")
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
    
    /**
     * Preload videos for popular/frequently detected images
     * Call this on app startup or after loading images
     */
    fun preloadPopularVideos() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                android.util.Log.d("SimpleARViewModel", "Preloading videos for popular images...")
                
                // Get all images from backend
                val response = TalkARApplication.instance.apiClient.getImages()
                
                if (response.isSuccessful) {
                    val images = response.body()?.take(2) // Preload first 2 images
                    
                    images?.forEach { backendImage ->
                        val firstDialogue = backendImage.dialogues.firstOrNull()
                        if (firstDialogue != null) {
                            val syncRequest = SyncRequest(
                                text = firstDialogue.text,
                                language = firstDialogue.language,
                                voiceId = firstDialogue.voiceId,
                                imageUrl = ApiConfig.getFullImageUrl(backendImage.imageUrl)
                            )
                            
                            val syncResponse = TalkARApplication.instance.apiClient.generateSyncVideo(syncRequest)
                            if (syncResponse.isSuccessful && syncResponse.body()?.videoUrl != null) {
                                val videoUrl = syncResponse.body()!!.videoUrl!!
                                videoCacheManager.preloadVideo(videoUrl, backendImage.id)
                                android.util.Log.d("SimpleARViewModel", "Preloaded video for: ${backendImage.name}")
                            }
                        }
                    }
                    
                    android.util.Log.d("SimpleARViewModel", "✅ Popular videos preloaded")
                }
            } catch (e: Exception) {
                android.util.Log.w("SimpleARViewModel", "Failed to preload popular videos: ${e.message}")
            }
        }
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

/**
 * Avatar loading states for 3D avatar rendering
 */
sealed class AvatarLoadState {
    object Idle : AvatarLoadState()
    data class LoadingAvatar(val imageId: String) : AvatarLoadState()
    data class AvatarLoaded(val imageId: String, val avatarId: String) : AvatarLoadState()
    data class AvatarError(val imageId: String, val error: String) : AvatarLoadState()
}

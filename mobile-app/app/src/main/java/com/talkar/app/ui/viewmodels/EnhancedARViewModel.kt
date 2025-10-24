package com.talkar.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.data.models.AdContent
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.repository.ImageRepository
import com.talkar.app.TalkARApplication
import com.talkar.app.data.services.AdContentGenerationService
import com.talkar.app.data.services.EnhancedARService
import com.google.ar.core.LightEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Enhanced AR functionality
 */
class EnhancedARViewModel(
    private val imageRepository: ImageRepository
) : ViewModel() {
    
    private val TAG = "EnhancedARViewModel"
    
    // AR Service
    private val arService = EnhancedARService(TalkARApplication.instance)
    
    // UI State
    private val _isAvatarVisible = MutableStateFlow(false)
    val isAvatarVisible: StateFlow<Boolean> = _isAvatarVisible.asStateFlow()
    
    private val _currentAvatar = MutableStateFlow<Avatar?>(null)
    val currentAvatar: StateFlow<Avatar?> = _currentAvatar.asStateFlow()
    
    private val _currentImage = MutableStateFlow<BackendImage?>(null)
    val currentImage: StateFlow<BackendImage?> = _currentImage.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _detectionStatus = MutableStateFlow("Ready")
    val detectionStatus: StateFlow<String> = _detectionStatus.asStateFlow()
    
    // Enhanced tracking metrics
    private val _trackingQuality = MutableStateFlow(com.talkar.app.data.services.EnhancedARService.TrackingQuality.UNKNOWN)
    val trackingQuality: StateFlow<com.talkar.app.data.services.EnhancedARService.TrackingQuality> = _trackingQuality.asStateFlow()
    
    private val _lightingQuality = MutableStateFlow(com.talkar.app.data.services.EnhancedARService.LightingQuality.UNKNOWN)
    val lightingQuality: StateFlow<com.talkar.app.data.services.EnhancedARService.LightingQuality> = _lightingQuality.asStateFlow()
    
    private val _lightEstimate = MutableStateFlow<LightEstimate?>(null)
    val lightEstimate: StateFlow<LightEstimate?> = _lightEstimate.asStateFlow()
    
    private val _isAvatarSpeaking = MutableStateFlow(false)
    val isAvatarSpeaking: StateFlow<Boolean> = _isAvatarSpeaking.asStateFlow()
    
    // Backend data
    private val _images = MutableStateFlow<List<BackendImage>>(emptyList())
    val images: StateFlow<List<BackendImage>> = _images.asStateFlow()
    
    private val _avatars = MutableStateFlow<List<Avatar>>(emptyList())
    val avatars: StateFlow<List<Avatar>> = _avatars.asStateFlow()
    
    // Ad Content State
    private val _currentAdContent = MutableStateFlow<AdContent?>(null)
    val currentAdContent: StateFlow<AdContent?> = _currentAdContent.asStateFlow()
    
    private val _isAdContentLoading = MutableStateFlow(false)
    val isAdContentLoading: StateFlow<Boolean> = _isAdContentLoading.asStateFlow()
    
    private val _adContentError = MutableStateFlow<String?>(null)
    val adContentError: StateFlow<String?> = _adContentError.asStateFlow()
    
    // Services
    private val adContentService = AdContentGenerationService.getInstance()
    
    init {
        loadBackendData()
        initializeARService()
    }
    
    /**
     * Initialize AR service
     */
    private fun initializeARService() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing AR service...")
                val initialized = arService.initialize()
                if (initialized) {
                    Log.d(TAG, "AR service initialized successfully")
                    // Start observing AR service state
                    observeARServiceState()
                } else {
                    Log.e(TAG, "Failed to initialize AR service")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing AR service: ${e.message}")
            }
        }
    }
    
    /**
     * Observe AR service state changes
     */
    private fun observeARServiceState() {
        viewModelScope.launch {
            // Observe tracking state
            arService.isTracking.collect { isTracking ->
                _isTracking.value = isTracking
            }
        }
        
        viewModelScope.launch {
            // Observe tracking quality
            arService.trackingQuality.collect { quality ->
                _trackingQuality.value = quality
            }
        }
        
        viewModelScope.launch {
            // Observe lighting quality
            arService.lightingQuality.collect { quality ->
                _lightingQuality.value = quality
            }
        }
        
        viewModelScope.launch {
            // Observe light estimate
            arService.lightEstimate.collect { estimate ->
                _lightEstimate.value = estimate
            }
        }
        
        viewModelScope.launch {
            // Observe avatar speaking state
            arService.isAvatarSpeaking.collect { isSpeaking ->
                _isAvatarSpeaking.value = isSpeaking
            }
        }
    }
    
    /**
     * Load images and avatars from backend
     */
    private fun loadBackendData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading backend data...")
                
                // Load images - simplified for now
                Log.d(TAG, "Loading images...")
                _images.value = emptyList()
                
                // Load avatars - simplified for now
                Log.d(TAG, "Loading avatars...")
                _avatars.value = emptyList()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading backend data: ${e.message}")
            }
        }
    }
    
    /**
     * Handle image detection
     */
    fun onImageDetected() {
        Log.d(TAG, "Image detected")
        _isTracking.value = true
        _detectionStatus.value = "Image Detected"
        
        // Show avatar if we have one
        if (_currentAvatar.value != null) {
            _isAvatarVisible.value = true
        }
    }
    
    /**
     * Handle image loss
     */
    fun onImageLost() {
        Log.d(TAG, "Image lost")
        _isTracking.value = false
        _isAvatarVisible.value = false
        _detectionStatus.value = "Searching..."
    }
    
    /**
     * Set current image and avatar
     */
    fun setCurrentImageAndAvatar(image: BackendImage, avatar: Avatar) {
        Log.d(TAG, "Setting current image: ${image.name} with avatar: ${avatar.name}")
        _currentImage.value = image
        _currentAvatar.value = avatar
    }
    
    /**
     * Handle avatar tap
     */
    fun onAvatarTapped() {
        Log.d(TAG, "Avatar tapped")
        // TODO: Implement avatar interaction (play script, lip-sync, etc.)
    }
    
    /**
     * Start AR tracking
     */
    fun startTracking() {
        Log.d(TAG, "Starting AR tracking")
        _detectionStatus.value = "Starting AR..."
        
        // Start AR service
        viewModelScope.launch {
            try {
                arService.resumeTracking()
                _detectionStatus.value = "Searching for images..."
            } catch (e: Exception) {
                Log.e(TAG, "Error starting AR tracking: ${e.message}")
                _detectionStatus.value = "Error: ${e.message}"
            }
        }
    }
    
    /**
     * Stop AR tracking
     */
    fun stopTracking() {
        Log.d(TAG, "Stopping AR tracking")
        _isTracking.value = false
        _isAvatarVisible.value = false
        _detectionStatus.value = "Stopped"
        
        // Stop AR service
        viewModelScope.launch {
            try {
                arService.stopTracking()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AR tracking: ${e.message}")
            }
        }
    }
    
    /**
     * Set avatar speaking state
     */
    fun setAvatarSpeaking(isSpeaking: Boolean) {
        _isAvatarSpeaking.value = isSpeaking
        arService.setAvatarSpeaking(isSpeaking)
    }
    
    /**
     * Start ambient audio
     */
    fun startAmbientAudio() {
        arService.startAmbientAudio()
    }
    
    /**
     * Stop ambient audio
     */
    fun stopAmbientAudio() {
        arService.stopAmbientAudio()
    }
    
    /**
     * Pause ambient audio
     */
    fun pauseAmbientAudio() {
        arService.pauseAmbientAudio()
    }
    
    /**
     * Resume ambient audio
     */
    fun resumeAmbientAudio() {
        arService.resumeAmbientAudio()
    }
    
    /**
     * Generate ad content for a detected image/product
     */
    fun generateAdContentForImage(imageId: String, productName: String) {
        Log.d(TAG, "Generating ad content for image: $imageId, product: $productName")
        
        viewModelScope.launch {
            try {
                _isAdContentLoading.value = true
                _adContentError.value = null
                
                
                // Call the ad content generation service
                val result = adContentService.generateAdContent(productName)
                
                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    val adContent = AdContent(
                        script = response.script ?: "No script available",
                        audioUrl = response.audio_url,
                        videoUrl = response.video_url,
                        productName = productName
                    )
                    
                    _currentAdContent.value = adContent
                    _isAvatarVisible.value = true // Show the avatar overlay
                    _isAdContentLoading.value = false
                    
                    Log.d(TAG, "Ad content generated successfully for $productName")
                } else {
                    _adContentError.value = result.exceptionOrNull()?.message ?: "Failed to generate ad content"
                    _isAdContentLoading.value = false
                    Log.e(TAG, "Failed to generate ad content: ${_adContentError.value}")
                }
            } catch (e: Exception) {
                _adContentError.value = e.message ?: "Unknown error occurred"
                _isAdContentLoading.value = false
                Log.e(TAG, "Exception while generating ad content: ${e.message}")
            }
        }
    }
    
    /**
     * Clear current ad content
     */
    fun clearAdContent() {
        _currentAdContent.value = null
        _adContentError.value = null
        _isAdContentLoading.value = false
    }
    
    /**
     * Get avatar for specific image
     */
    fun getAvatarForImage(imageId: String): Avatar? {
        // For now, return the first available avatar
        // In production, you would map images to avatars
        return _avatars.value.firstOrNull()
    }
    
    /**
     * Refresh backend data
     */
    fun refreshData() {
        loadBackendData()
    }
    
    /**
     * Simulate image detection for testing
     */
    fun simulateImageDetection() {
        viewModelScope.launch {
            val images = _images.value
            val avatars = _avatars.value
            
            if (images.isNotEmpty() && avatars.isNotEmpty()) {
                val randomImage = images.random()
                val randomAvatar = avatars.random()
                
                setCurrentImageAndAvatar(randomImage, randomAvatar)
                onImageDetected()
                
                // Simulate image loss after 3 seconds
                kotlinx.coroutines.delay(3000)
                onImageLost()
            }
        }
    }
    
    /**
     * Cleanup when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            arService.stopTracking()
        }
    }
}
package com.talkar.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.repository.ImageRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    // UI State
    private val _isAvatarVisible = MutableStateFlow(false)
    val isAvatarVisible: StateFlow<Boolean> = _isAvatarVisible.asStateFlow()
    
    private val _currentAvatar = MutableStateFlow<Avatar?>(null)
    val currentAvatar: StateFlow<Avatar?> = _currentAvatar.asStateFlow()
    
    private val _currentImage = MutableStateFlow<BackendImage?>(null)
    val currentImage: StateFlow<BackendImage?> = _currentImage.asStateFlow()
    
    private val _currentDialogue = MutableStateFlow<com.talkar.app.data.models.Dialogue?>(null)
    val currentDialogue: StateFlow<com.talkar.app.data.models.Dialogue?> = _currentDialogue.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _detectionStatus = MutableStateFlow("Ready")
    val detectionStatus: StateFlow<String> = _detectionStatus.asStateFlow()
    
    // Video playback state
    private val _isVideoPlaying = MutableStateFlow(false)
    val isVideoPlaying: StateFlow<Boolean> = _isVideoPlaying.asStateFlow()
    
    private val _currentVideoUrl = MutableStateFlow<String?>(null)
    val currentVideoUrl: StateFlow<String?> = _currentVideoUrl.asStateFlow()
    
    // Bug fix: Avatar disappearing incorrectly
    // Track image loss with debounce to prevent flickering
    private var imageLossJob: Job? = null
    private val imageLossDebounceMs = 500L // Wait 500ms before hiding avatar
    
    // Bug fix: Anchor stability
    private val _anchorStable = MutableStateFlow(false)
    val anchorStable: StateFlow<Boolean> = _anchorStable.asStateFlow()
    
    // Backend data
    private val _images = MutableStateFlow<List<BackendImage>>(emptyList())
    val images: StateFlow<List<BackendImage>> = _images.asStateFlow()
    
    private val _avatars = MutableStateFlow<List<Avatar>>(emptyList())
    val avatars: StateFlow<List<Avatar>> = _avatars.asStateFlow()
    
    init {
        loadBackendData()
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
     * Handle image detection with stability check
     * Bug fix: Prevents avatar from appearing/disappearing rapidly
     */
    fun onImageDetected() {
        Log.d(TAG, "Image detected")
        
        // Cancel any pending image loss
        imageLossJob?.cancel()
        imageLossJob = null
        
        _isTracking.value = true
        _anchorStable.value = true
        _detectionStatus.value = "Image Detected"
        
        // Show avatar if we have one
        if (_currentAvatar.value != null) {
            _isAvatarVisible.value = true
            // Auto-play video when image detected
            _isVideoPlaying.value = true
            Log.d(TAG, "Avatar shown with auto-play")
        }
    }
    
    /**
     * Handle image loss with debounce
     * Bug fix: Adds delay before hiding to prevent flickering
     */
    fun onImageLost() {
        Log.d(TAG, "Image lost - starting debounce timer")
        
        // Cancel previous job if exists
        imageLossJob?.cancel()
        
        // Start debounced hide
        imageLossJob = viewModelScope.launch {
            delay(imageLossDebounceMs)
            
            Log.d(TAG, "Image loss confirmed after debounce")
            _isTracking.value = false
            _isAvatarVisible.value = false
            _anchorStable.value = false
            _detectionStatus.value = "Searching..."
            // Pause video when image lost
            _isVideoPlaying.value = false
        }
    }
    
    /**
     * Reset image loss (called when image is re-detected quickly)
     * Bug fix: Prevents unnecessary hiding
     */
    fun resetImageLoss() {
        imageLossJob?.cancel()
        imageLossJob = null
        Log.d(TAG, "Image loss cancelled - image still visible")
    }
    
    /**
     * Set current image and avatar
     */
    fun setCurrentImageAndAvatar(image: BackendImage, avatar: Avatar, videoUrl: String? = null) {
        Log.d(TAG, "Setting current image: ${image.name} with avatar: ${avatar.name}")
        _currentImage.value = image
        _currentAvatar.value = avatar
        _currentVideoUrl.value = videoUrl
        
        // Set first dialogue if available
        // Note: BackendImage doesn't have dialogues field in the current model
        // You may need to fetch dialogues separately or add to BackendImage model
    }
    
    /**
     * Handle avatar tap - Toggle play/pause
     */
    fun onAvatarTapped() {
        Log.d(TAG, "Avatar tapped - toggling playback")
        _isVideoPlaying.value = !_isVideoPlaying.value
        
        if (_isVideoPlaying.value) {
            Log.d(TAG, "Video playing")
        } else {
            Log.d(TAG, "Video paused")
        }
    }
    
    /**
     * Start AR tracking
     */
    fun startTracking() {
        Log.d(TAG, "Starting AR tracking")
        _detectionStatus.value = "Starting AR..."
        
        // Simulate AR initialization
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _detectionStatus.value = "Searching for images..."
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
                
                // Simulate image loss after 5 seconds
                kotlinx.coroutines.delay(5000)
                onImageLost()
            } else {
                // Create mock data for testing
                val mockImage = BackendImage(
                    id = "test-image-1",
                    name = "Test Product",
                    imageUrl = "https://example.com/test.jpg",
                    description = "Test product for AR",
                    isActive = true,
                    createdAt = System.currentTimeMillis().toString(),
                    updatedAt = System.currentTimeMillis().toString()
                )
                
                val mockAvatar = Avatar(
                    id = "test-avatar-1",
                    name = "Demo Avatar",
                    avatarImageUrl = "https://example.com/avatar.jpg",
                    voiceId = "voice_001",
                    language = "en",
                    isActive = true,
                    createdAt = System.currentTimeMillis().toString(),
                    updatedAt = System.currentTimeMillis().toString()
                )
                
                setCurrentImageAndAvatar(mockImage, mockAvatar)
                onImageDetected()
                
                // Simulate image loss after 5 seconds
                kotlinx.coroutines.delay(5000)
                onImageLost()
            }
        }
    }
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        imageLossJob?.cancel()
        Log.d(TAG, "ViewModel cleared, jobs cancelled")
    }
}

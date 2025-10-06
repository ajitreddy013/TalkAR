package com.talkar.app.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.repository.ImageRepository
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
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _detectionStatus = MutableStateFlow("Ready")
    val detectionStatus: StateFlow<String> = _detectionStatus.asStateFlow()
    
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
                
                // Simulate image loss after 3 seconds
                kotlinx.coroutines.delay(3000)
                onImageLost()
            }
        }
    }
}

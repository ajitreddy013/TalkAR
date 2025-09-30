package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

class ARImageRecognitionService(private val context: Context) {
    
    private val tag = "ARImageRecognitionService"
    private var session: Session? = null
    private var imageDatabase: AugmentedImageDatabase? = null
    
    private val _recognizedImages = MutableStateFlow<List<AugmentedImage>>(emptyList())
    val recognizedImages: StateFlow<List<AugmentedImage>> = _recognizedImages.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Cache for recognized images to avoid duplicate processing
    private val recognizedImageCache = ConcurrentHashMap<String, ImageRecognition>()
    
    /**
     * Initialize ARCore session and image database
     */
    suspend fun initialize(): Boolean {
        return try {
            // Check if ARCore is available
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            // Create ARCore session
            session = Session(context)
            
            // Initialize image database
            initializeImageDatabase()
            
            // Start tracking
            startTracking()
            
            Log.d(tag, "AR Image Recognition Service initialized successfully")
            true
        } catch (e: UnavailableException) {
            Log.e(tag, "ARCore unavailable", e)
            _error.value = "ARCore is not available: ${e.message}"
            false
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AR service", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Check if ARCore is supported on this device
     */
    private fun isARCoreSupported(): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            availability.isSupported
        } catch (e: Exception) {
            Log.e(tag, "Error checking ARCore availability", e)
            false
        }
    }
    
    /**
     * Initialize the image database with reference images
     */
    private fun initializeImageDatabase() {
        try {
            // Create empty image database
            imageDatabase = AugmentedImageDatabase(session)
            
            // In a real implementation, you would load reference images from:
            // 1. Local storage (pre-downloaded images)
            // 2. Remote API (downloaded on demand)
            // 3. Admin-uploaded images from the backend
            
            // For now, we'll create a placeholder database
            // In production, this would be populated with actual reference images
            Log.d(tag, "Image database initialized")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize image database", e)
            throw e
        }
    }
    
    /**
     * Start ARCore tracking
     */
    private fun startTracking() {
        try {
            session?.let { session ->
                // Configure session for image tracking
                val config = Config(session)
                config.focusMode = Config.FocusMode.AUTO
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                
                // Enable image tracking
                config.augmentedImageDatabase = imageDatabase
                
                // Configure session
                session.configure(config)
                
                _isTracking.value = true
                Log.d(tag, "ARCore tracking started")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start tracking", e)
            _error.value = "Failed to start ARCore tracking: ${e.message}"
        }
    }
    
    /**
     * Process camera frame for image recognition
     */
    fun processFrame(frame: Frame) {
        try {
            val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            
            for (augmentedImage in augmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.TRACKING -> {
                        handleImageRecognized(augmentedImage)
                    }
                    TrackingState.PAUSED -> {
                        Log.d(tag, "Image tracking paused: ${augmentedImage.name}")
                    }
                    TrackingState.STOPPED -> {
                        Log.d(tag, "Image tracking stopped: ${augmentedImage.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing frame", e)
        }
    }
    
    /**
     * Handle recognized image
     */
    private fun handleImageRecognized(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name ?: "unknown"
        
        // Check if we've already processed this image recently
        if (recognizedImageCache.containsKey(imageName)) {
            return
        }
        
        try {
            // Create image recognition result
            val imageRecognition = ImageRecognition(
                id = imageName,
                name = imageName,
                description = "Recognized image: $imageName",
                imageUrl = "", // This would be populated from the database
                thumbnailUrl = "",
                isActive = true,
                trackingState = augmentedImage.trackingState.name,
                centerPose = augmentedImage.centerPose,
                extentX = augmentedImage.extentX,
                extentZ = augmentedImage.extentZ
            )
            
            // Cache the result
            recognizedImageCache[imageName] = imageRecognition
            
            // Update state
            val currentImages = _recognizedImages.value.toMutableList()
            currentImages.add(augmentedImage)
            _recognizedImages.value = currentImages
            
            Log.d(tag, "Image recognized: $imageName")
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling recognized image", e)
        }
    }
    
    /**
     * Get recognized image by name
     */
    fun getRecognizedImage(name: String): ImageRecognition? {
        return recognizedImageCache[name]
    }
    
    /**
     * Clear recognized images
     */
    fun clearRecognizedImages() {
        recognizedImageCache.clear()
        _recognizedImages.value = emptyList()
        Log.d(tag, "Cleared recognized images")
    }
    
    /**
     * Add reference image to database
     */
    fun addReferenceImage(imageName: String, imageBytes: ByteArray): Boolean {
        return try {
            imageDatabase?.let { database ->
                val augmentedImage = database.addImage(imageName, imageBytes)
                Log.d(tag, "Added reference image: $imageName")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(tag, "Failed to add reference image: $imageName", e)
            _error.value = "Failed to add reference image: ${e.message}"
            false
        }
    }
    
    /**
     * Remove reference image from database
     */
    fun removeReferenceImage(imageName: String): Boolean {
        return try {
            imageDatabase?.let { database ->
                database.removeImage(imageName)
                Log.d(tag, "Removed reference image: $imageName")
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove reference image: $imageName", e)
            false
        }
    }
    
    /**
     * Pause tracking
     */
    fun pauseTracking() {
        try {
            session?.pause()
            _isTracking.value = false
            Log.d(tag, "ARCore tracking paused")
        } catch (e: Exception) {
            Log.e(tag, "Failed to pause tracking", e)
        }
    }
    
    /**
     * Resume tracking
     */
    fun resumeTracking() {
        try {
            session?.resume()
            _isTracking.value = true
            Log.d(tag, "ARCore tracking resumed")
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume tracking", e)
        }
    }
    
    /**
     * Stop tracking and cleanup
     */
    fun stopTracking() {
        try {
            session?.close()
            session = null
            imageDatabase = null
            _isTracking.value = false
            _recognizedImages.value = emptyList()
            recognizedImageCache.clear()
            Log.d(tag, "ARCore tracking stopped and cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop tracking", e)
        }
    }
    
    /**
     * Get current tracking state
     */
    fun getTrackingState(): String {
        return when {
            _isTracking.value -> "TRACKING"
            session != null -> "PAUSED"
            else -> "STOPPED"
        }
    }
}

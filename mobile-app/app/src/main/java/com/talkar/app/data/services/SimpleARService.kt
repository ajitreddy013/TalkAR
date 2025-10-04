package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Simple AR Service - Bypasses problematic depth estimation
 * 
 * This service addresses the common ARCore issues:
 * - NOT_FOUND: Not able to find any depth measurements
 * - VIO initialization failures
 * - Camera preview not showing
 * 
 * Solution: Use basic image tracking without depth estimation
 */
class SimpleARService(private val context: Context) {
    
    private val tag = "SimpleARService"
    private var session: Session? = null
    private var imageDatabase: AugmentedImageDatabase? = null
    
    // Tracking state
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Recognition state
    private val _recognizedImages = MutableStateFlow<List<AugmentedImage>>(emptyList())
    val recognizedImages: StateFlow<List<AugmentedImage>> = _recognizedImages.asStateFlow()
    
    private val _recognitionConfidence = MutableStateFlow(0f)
    val recognitionConfidence: StateFlow<Float> = _recognitionConfidence.asStateFlow()
    
    // Cache
    private val recognizedImageCache = ConcurrentHashMap<String, ImageRecognition>()
    
    /**
     * Initialize simple AR service with minimal configuration
     */
    suspend fun initialize(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            session = Session(context)
            configureSimpleSession()
            initializeSimpleImageDatabase()
            startSimpleTracking()
            
            Log.d(tag, "Simple AR Service initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize simple AR service", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Configure session with minimal settings to avoid depth estimation issues
     */
    private fun configureSimpleSession() {
        session?.let { session ->
            val config = Config(session)
            
            // Minimal configuration to avoid depth estimation problems
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            
            // DISABLE depth mode to avoid depth estimation failures
            config.depthMode = Config.DepthMode.DISABLED
            
            // DISABLE plane detection to reduce complexity
            config.planeFindingMode = Config.PlaneFindingMode.DISABLED
            
            // DISABLE instant placement
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            
            // DISABLE cloud anchors
            config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            
            // Set image database
            imageDatabase?.let { database ->
                config.augmentedImageDatabase = database
            }
            
            session.configure(config)
            Log.d(tag, "ARCore session configured with minimal settings")
        }
    }
    
    /**
     * Initialize simple image database
     */
    private fun initializeSimpleImageDatabase() {
        try {
            imageDatabase = AugmentedImageDatabase(session)
            
            // Create simple test images that work well with basic tracking
            createSimpleTestImages()
            
            Log.d(tag, "Simple image database initialized")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize simple image database", e)
            throw e
        }
    }
    
    /**
     * Create simple test images optimized for basic AR tracking
     */
    private fun createSimpleTestImages() {
        try {
            val testImages = listOf(
                "simple_test_1" to createSimpleTestPattern("SIMPLE TEST 1"),
                "simple_test_2" to createSimpleTestPattern("SIMPLE TEST 2"),
                "simple_test_3" to createSimpleTestPattern("SIMPLE TEST 3")
            )
            
            testImages.forEach { (name, bitmap) ->
                if (validateSimpleImageQuality(bitmap)) {
                    imageDatabase?.addImage(name, bitmap)
                    Log.d(tag, "Added simple test image: $name")
                } else {
                    Log.w(tag, "Simple test image failed quality validation: $name")
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to create simple test images", e)
        }
    }
    
    /**
     * Create simple test pattern optimized for basic tracking
     */
    private fun createSimpleTestPattern(text: String): android.graphics.Bitmap {
        val size = 1024 // Smaller size for better performance
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }
        
        // High contrast background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        // Simple geometric pattern for basic tracking
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 4f
        
        // Central circle
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Corner squares for tracking
        val cornerSize = size / 8f
        canvas.drawRect(0f, 0f, cornerSize, cornerSize, paint)
        canvas.drawRect(size - cornerSize, 0f, size.toFloat(), cornerSize, paint)
        canvas.drawRect(0f, size - cornerSize, cornerSize, size.toFloat(), paint)
        canvas.drawRect(size - cornerSize, size - cornerSize, size.toFloat(), size.toFloat(), paint)
        
        // Simple text
        paint.textSize = 48f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(text, centerX, centerY + 20f, paint)
        
        return bitmap
    }
    
    /**
     * Validate simple image quality with relaxed requirements
     */
    private fun validateSimpleImageQuality(bitmap: android.graphics.Bitmap): Boolean {
        try {
            // Relaxed requirements for simple tracking
            if (bitmap.width < 256 || bitmap.height < 256) {
                return false
            }
            
            // Basic contrast check
            val contrast = calculateSimpleContrast(bitmap)
            if (contrast < 0.2) {
                return false
            }
            
            Log.d(tag, "Simple image quality validation passed - contrast: $contrast")
            return true
            
        } catch (e: Exception) {
            Log.e(tag, "Error validating simple image quality", e)
            return false
        }
    }
    
    /**
     * Calculate simple contrast
     */
    private fun calculateSimpleContrast(bitmap: android.graphics.Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 10
        
        val pixels = mutableListOf<Int>()
        for (y in 0 until height step sampleSize) {
            for (x in 0 until width step sampleSize) {
                pixels.add(bitmap.getPixel(x, y))
            }
        }
        
        val luminances = pixels.map { pixel ->
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        }
        
        val mean = luminances.average()
        val variance = luminances.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * Start simple tracking
     */
    private fun startSimpleTracking() {
        try {
            session?.let { session ->
                session.resume()
                _isTracking.value = true
                _trackingState.value = TrackingState.TRACKING
                
                startSimpleFrameProcessing()
                Log.d(tag, "Simple ARCore tracking started")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start simple tracking", e)
            _error.value = "Failed to start ARCore tracking: ${e.message}"
        }
    }
    
    /**
     * Start simple frame processing
     */
    private fun startSimpleFrameProcessing() {
        GlobalScope.launch(Dispatchers.IO) {
            while (_isTracking.value) {
                try {
                    session?.let { session ->
                        val frame = session.update()
                        processSimpleFrame(frame)
                    }
                    
                    delay(50) // 20 FPS - slower to reduce load
                } catch (e: Exception) {
                    Log.e(tag, "Error in simple frame processing", e)
                    delay(100)
                }
            }
        }
    }
    
    /**
     * Process frame with simple recognition
     */
    private fun processSimpleFrame(frame: Frame) {
        try {
            val camera = frame.camera
            val trackingState = camera.trackingState
            
            when (trackingState) {
                TrackingState.TRACKING -> {
                    processSimpleRecognition(frame)
                }
                TrackingState.PAUSED -> {
                    Log.w(tag, "Simple tracking paused")
                }
                TrackingState.STOPPED -> {
                    Log.w(tag, "Simple tracking stopped")
                }
            }
            
            _trackingState.value = trackingState
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing simple frame", e)
        }
    }
    
    /**
     * Process simple recognition
     */
    private fun processSimpleRecognition(frame: Frame) {
        try {
            val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            
            for (augmentedImage in augmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.TRACKING -> {
                        handleSimpleRecognition(augmentedImage)
                    }
                    TrackingState.PAUSED -> {
                        Log.d(tag, "Simple image tracking paused: ${augmentedImage.name}")
                    }
                    TrackingState.STOPPED -> {
                        Log.d(tag, "Simple image tracking stopped: ${augmentedImage.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing simple recognition", e)
        }
    }
    
    /**
     * Handle simple recognition
     */
    private fun handleSimpleRecognition(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name ?: "unknown"
        
        if (recognizedImageCache.containsKey(imageName)) {
            return
        }
        
        try {
            // Simple confidence calculation
            val confidence = when (augmentedImage.trackingState) {
                TrackingState.TRACKING -> 0.9f
                TrackingState.PAUSED -> 0.6f
                TrackingState.STOPPED -> 0.3f
            }
            
            _recognitionConfidence.value = confidence
            
            // Create simple image recognition result
            val imageRecognition = ImageRecognition(
                id = imageName,
                name = imageName,
                description = "Simple recognition: $imageName (confidence: ${(confidence * 100).toInt()}%)",
                imageUrl = "",
                dialogues = emptyList(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            recognizedImageCache[imageName] = imageRecognition
            
            val currentImages = _recognizedImages.value.toMutableList()
            currentImages.add(augmentedImage)
            _recognizedImages.value = currentImages
            
            Log.d(tag, "Simple recognition: $imageName (confidence: $confidence)")
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling simple recognition", e)
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
        Log.d(tag, "Cleared simple recognition")
    }
    
    /**
     * Pause tracking
     */
    fun pauseTracking() {
        try {
            session?.pause()
            _isTracking.value = false
            _trackingState.value = TrackingState.PAUSED
            Log.d(tag, "Simple ARCore tracking paused")
        } catch (e: Exception) {
            Log.e(tag, "Failed to pause simple tracking", e)
        }
    }
    
    /**
     * Resume tracking
     */
    fun resumeTracking() {
        try {
            session?.resume()
            _isTracking.value = true
            _trackingState.value = TrackingState.TRACKING
            Log.d(tag, "Simple ARCore tracking resumed")
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume simple tracking", e)
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
            _trackingState.value = TrackingState.STOPPED
            _recognizedImages.value = emptyList()
            recognizedImageCache.clear()
            
            Log.d(tag, "Simple ARCore tracking stopped and cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop simple tracking", e)
        }
    }
    
    /**
     * Check if ARCore is supported
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
     * Post an error message
     */
    fun postError(errorMessage: String) {
        _error.value = errorMessage
    }
    
    /**
     * Check if service is initialized
     */
    fun isInitialized(): Boolean {
        return session != null && imageDatabase != null
    }
    
    /**
     * Get simple metrics
     */
    fun getSimpleMetrics(): Map<String, Any> {
        return mapOf(
            "trackingState" to _trackingState.value.name,
            "recognitionConfidence" to _recognitionConfidence.value,
            "recognizedImagesCount" to _recognizedImages.value.size,
            "isInitialized" to isInitialized()
        )
    }
}

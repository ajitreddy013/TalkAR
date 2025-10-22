package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.pow

/**
 * Enhanced AR Service that addresses common ARCore tracking issues:
 * - VIO (Visual-Inertial Odometry) tracking loss
 * - Insufficient visual features
 * - Depth measurement failures
 * - Initialization failures
 * - High motion dynamics
 */
class EnhancedARService(private val context: Context) {
    
    private val tag = "EnhancedARService"
    private var session: Session? = null
    private var imageDatabase: AugmentedImageDatabase? = null
    
    // Performance optimization: Throttle lighting updates
    private var lastLightingUpdate = 0L
    private val LIGHTING_UPDATE_INTERVAL = 500L // Update lighting every 500ms
    
    // Tracking state management
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    private val _recognizedImages = MutableStateFlow<List<AugmentedImage>>(emptyList())
    val recognizedImages: StateFlow<List<AugmentedImage>> = _recognizedImages.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Enhanced tracking metrics
    private val _trackingQuality = MutableStateFlow(TrackingQuality.UNKNOWN)
    val trackingQuality: StateFlow<TrackingQuality> = _trackingQuality.asStateFlow()
    
    private val _lightingQuality = MutableStateFlow(LightingQuality.UNKNOWN)
    val lightingQuality: StateFlow<LightingQuality> = _lightingQuality.asStateFlow()
    
    private val _motionStability = MutableStateFlow(MotionStability.UNKNOWN)
    val motionStability: StateFlow<MotionStability> = _motionStability.asStateFlow()
    
    private val _trackingGuidance = MutableStateFlow<String?>(null)
    val trackingGuidance: StateFlow<String?> = _trackingGuidance.asStateFlow()
    
    // Light estimation data
    private val _lightEstimate = MutableStateFlow<LightEstimate?>(null)
    val lightEstimate: StateFlow<LightEstimate?> = _lightEstimate.asStateFlow()
    
    // Ambient audio service
    private var ambientAudioService: AmbientAudioService? = null
    
    // Avatar voice state
    private val _isAvatarSpeaking = MutableStateFlow(false)
    val isAvatarSpeaking: StateFlow<Boolean> = _isAvatarSpeaking.asStateFlow()
    
    // Cache for recognized images
    private val recognizedImageCache = ConcurrentHashMap<String, ImageRecognition>()
    
    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Tracking metrics for stability analysis
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var trackingLossCount = 0
    private var lastPose: Pose? = null
    private var motionHistory = mutableListOf<Float>()
    private var featureCountHistory = mutableListOf<Int>()
    
    // Configuration constants based on ARCore log analysis
    private companion object {
        const val MIN_FEATURE_RATIO = 0.2f // ARCore requires 20% inlier ratio
        const val MIN_FEATURE_COUNT = 50
        const val MAX_MOTION_THRESHOLD = 0.1f // Maximum allowed motion per frame
        const val TRACKING_LOSS_THRESHOLD = 1.2f // Seconds before VIO reset
        const val LIGHTING_SAMPLE_SIZE = 100
        const val MOTION_HISTORY_SIZE = 10
        const val FEATURE_HISTORY_SIZE = 5
    }
    
    enum class TrackingQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }
    
    enum class LightingQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }
    
    enum class MotionStability {
        STABLE, MODERATE, UNSTABLE, UNKNOWN
    }
    
    /**
     * Initialize enhanced ARCore session with improved configuration
     */
    suspend fun initialize(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            // Create ARCore session with enhanced configuration
            session = Session(context)
            
            // Initialize ambient audio service
            initializeAmbientAudio()
            
            // Configure session for better tracking stability
            configureSessionForStability()
            
            // Initialize image database with quality validation
            initializeImageDatabase()
            
            // Start enhanced tracking
            startEnhancedTracking()
            
            Log.d(tag, "Enhanced AR Service initialized successfully")
            true
        } catch (e: UnavailableException) {
            Log.e(tag, "ARCore unavailable", e)
            _error.value = "ARCore is not available: ${e.message}"
            false
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize enhanced AR service", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Initialize ambient audio service
     */
    private fun initializeAmbientAudio() {
        try {
            // Initialize with ambient background audio resource
            ambientAudioService = AmbientAudioService(context)
            // Note: In a real implementation, you would initialize with a valid audio resource
            // For now, we'll just initialize the service without a specific audio file
            Log.d(tag, "Ambient audio service initialized")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize ambient audio service", e)
        }
    }
    
    /**
     * Configure ARCore session for maximum tracking stability
     */
    private fun configureSessionForStability() {
        session?.let { session ->
            val config = Config(session)
            
            // Enhanced focus mode for better tracking
            config.focusMode = Config.FocusMode.AUTO
            
            // Use latest camera image for better performance
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            
            // Enable plane detection for better tracking context
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            
            // Enable depth for better tracking (if available)
            config.depthMode = Config.DepthMode.AUTOMATIC
            
            // Enable instant placement for better initialization
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            
            // Configure for better performance on mobile devices
            config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            
            // Enable light estimation for dynamic lighting
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            
            // Enable image tracking
            imageDatabase?.let { database ->
                config.augmentedImageDatabase = database
            }
            
            // Apply configuration
            session.configure(config)
            
            Log.d(tag, "ARCore session configured for enhanced stability and light estimation")
        }
    }
    
    /**
     * Check if ARCore is supported with enhanced validation
     */
    private fun isARCoreSupported(): Boolean {
        return try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            val isSupported = availability.isSupported
            
            if (isSupported) {
                Log.d(tag, "ARCore is supported on this device")
            } else {
                Log.w(tag, "ARCore is not supported on this device")
            }
            
            isSupported
        } catch (e: Exception) {
            Log.e(tag, "Error checking ARCore availability", e)
            false
        }
    }
    
    /**
     * Initialize image database with enhanced quality validation
     */
    private fun initializeImageDatabase() {
        try {
            imageDatabase = AugmentedImageDatabase(session)
            
            // Load high-quality test images
            loadHighQualityTestImages()
            
            Log.d(tag, "Enhanced image database initialized")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize image database", e)
            throw e
        }
    }
    
    /**
     * Load high-quality test images optimized for AR tracking
     */
    private fun loadHighQualityTestImages() {
        try {
            // Create multiple high-quality test patterns
            val testImages = listOf(
                "talkar_logo" to createHighQualityTestPattern("TALKAR LOGO"),
                "ar_pattern_1" to createHighQualityTestPattern("AR PATTERN 1"),
                "ar_pattern_2" to createHighQualityTestPattern("AR PATTERN 2")
            )
            
            testImages.forEach { (name, bitmap) ->
                if (validateImageQuality(bitmap)) {
                    imageDatabase?.addImage(name, bitmap)
                    Log.d(tag, "Added high-quality test image: $name")
                } else {
                    Log.w(tag, "Test image failed quality validation: $name")
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to load high-quality test images", e)
        }
    }
    
    /**
     * Create high-quality test pattern optimized for AR tracking
     */
    private fun createHighQualityTestPattern(text: String): android.graphics.Bitmap {
        val size = 2048 // High resolution for better tracking
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // High contrast background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        // Create complex geometric patterns for better feature detection
        paint.color = android.graphics.Color.BLACK
        paint.style = android.graphics.Paint.Style.FILL
        
        // Draw multiple circles for corner detection
        val circleRadius = size / 12f
        val positions = listOf(
            Pair(circleRadius * 2, circleRadius * 2),
            Pair(size - circleRadius * 2, circleRadius * 2),
            Pair(circleRadius * 2, size - circleRadius * 2),
            Pair(size - circleRadius * 2, size - circleRadius * 2),
            Pair(size / 2f, size / 2f)
        )
        
        positions.forEach { (x, y) ->
            canvas.drawCircle(x, y, circleRadius, paint)
        }
        
        // Draw grid pattern for better feature detection
        paint.color = android.graphics.Color.DKGRAY
        val gridSize = size / 8f
        for (i in 1..7) {
            val pos = i * gridSize
            canvas.drawLine(pos, 0f, pos, size.toFloat(), paint)
            canvas.drawLine(0f, pos, size.toFloat(), pos, paint)
        }
        
        // Add high-contrast text
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 80f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.setShadowLayer(6f, 3f, 3f, android.graphics.Color.WHITE)
        
        canvas.drawText(text, size / 2f, size / 2f - 50f, paint)
        canvas.drawText("AR TRACKING", size / 2f, size / 2f + 50f, paint)
        
        return bitmap
    }
    
    /**
     * Enhanced image quality validation based on ARCore requirements
     */
    private fun validateImageQuality(bitmap: android.graphics.Bitmap): Boolean {
        try {
            // Check minimum size requirements
            if (bitmap.width < 512 || bitmap.height < 512) {
                Log.w(tag, "Image too small: ${bitmap.width}x${bitmap.height}")
                return false
            }
            
            // Check contrast quality
            val contrast = calculateContrast(bitmap)
            if (contrast < 0.4) { // Higher threshold for better tracking
                Log.w(tag, "Image has insufficient contrast: $contrast")
                return false
            }
            
            // Check feature count
            val featureCount = calculateFeatureCount(bitmap)
            if (featureCount < 100) { // Higher threshold for better tracking
                Log.w(tag, "Image has insufficient features: $featureCount")
                return false
            }
            
            // Check for sufficient edges and corners
            val edgeCount = calculateEdgeCount(bitmap)
            if (edgeCount < 200) {
                Log.w(tag, "Image has insufficient edges: $edgeCount")
                return false
            }
            
            Log.d(tag, "Image quality validation passed - contrast: $contrast, features: $featureCount, edges: $edgeCount")
            return true
            
        } catch (e: Exception) {
            Log.e(tag, "Error validating image quality", e)
            return false
        }
    }
    
    /**
     * Calculate image contrast with improved algorithm
     */
    private fun calculateContrast(bitmap: android.graphics.Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 5 // More samples for better accuracy
        
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
        return sqrt(variance)
    }
    
    /**
     * Calculate feature count using improved edge detection
     */
    private fun calculateFeatureCount(bitmap: android.graphics.Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        var featureCount = 0
        
        // Use Sobel operator for better edge detection
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = calculateSobelGx(bitmap, x, y)
                val gy = calculateSobelGy(bitmap, x, y)
                val magnitude = sqrt((gx * gx + gy * gy).toDouble())
                
                if (magnitude > 50) { // Higher threshold for better features
                    featureCount++
                }
            }
        }
        
        return featureCount
    }
    
    /**
     * Calculate Sobel Gx operator
     */
    private fun calculateSobelGx(bitmap: android.graphics.Bitmap, x: Int, y: Int): Int {
        val p1 = getGrayValue(bitmap.getPixel(x - 1, y - 1))
        val p2 = getGrayValue(bitmap.getPixel(x - 1, y))
        val p3 = getGrayValue(bitmap.getPixel(x - 1, y + 1))
        val p4 = getGrayValue(bitmap.getPixel(x + 1, y - 1))
        val p5 = getGrayValue(bitmap.getPixel(x + 1, y))
        val p6 = getGrayValue(bitmap.getPixel(x + 1, y + 1))
        
        return (-p1 + p3 - 2 * p2 + 2 * p5 - p4 + p6)
    }
    
    /**
     * Calculate Sobel Gy operator
     */
    private fun calculateSobelGy(bitmap: android.graphics.Bitmap, x: Int, y: Int): Int {
        val p1 = getGrayValue(bitmap.getPixel(x - 1, y - 1))
        val p2 = getGrayValue(bitmap.getPixel(x, y - 1))
        val p3 = getGrayValue(bitmap.getPixel(x + 1, y - 1))
        val p4 = getGrayValue(bitmap.getPixel(x - 1, y + 1))
        val p5 = getGrayValue(bitmap.getPixel(x, y + 1))
        val p6 = getGrayValue(bitmap.getPixel(x + 1, y + 1))
        
        return (-p1 - 2 * p2 - p3 + p4 + 2 * p5 + p6)
    }
    
    /**
     * Calculate edge count for additional quality validation
     */
    private fun calculateEdgeCount(bitmap: android.graphics.Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        var edgeCount = 0
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = calculateSobelGx(bitmap, x, y)
                val gy = calculateSobelGy(bitmap, x, y)
                val magnitude = sqrt((gx * gx + gy * gy).toDouble())
                
                if (magnitude > 30) {
                    edgeCount++
                }
            }
        }
        
        return edgeCount
    }
    
    /**
     * Get grayscale value from color pixel
     */
    private fun getGrayValue(pixel: Int): Int {
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
    
    /**
     * Start enhanced tracking with stability monitoring
     */
    private fun startEnhancedTracking() {
        try {
            session?.let { session ->
                session.resume()
                _isTracking.value = true
                _trackingState.value = TrackingState.TRACKING
                
                // Start monitoring thread
                startTrackingMonitoring()
                
                Log.d(tag, "Enhanced ARCore tracking started")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start enhanced tracking", e)
            _error.value = "Failed to start ARCore tracking: ${e.message}"
        }
    }
    
    /**
     * Start monitoring thread for tracking stability
     */
    private fun startTrackingMonitoring() {
        coroutineScope.launch {
            while (_isTracking.value) {
                try {
                    session?.let { session ->
                        val frame = session.update()
                        processFrame(frame)
                    }
                    
                    delay(33) // ~30 FPS monitoring
                } catch (e: Exception) {
                    Log.e(tag, "Error in tracking monitoring", e)
                    delay(100)
                }
            }
        }
    }
    
    /**
     * Process ARCore frame with enhanced tracking and light estimation
     */
    private fun processFrame(frame: Frame) {
        try {
            // Update tracking state
            updateTrackingState(frame)
            
            // Analyze motion stability
            analyzeMotionStability(frame)
            
            // Analyze lighting quality using ARCore light estimation
            analyzeLightEstimation(frame)
            
            // Update overall tracking quality
            updateTrackingQuality()
            
            // Process augmented images
            processAugmentedImages(frame)
            
            frameCount++
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing frame", e)
        }
    }
    
    /**
     * Update tracking state based on ARCore frame
     */
    private fun updateTrackingState(frame: Frame) {
        try {
            val camera = frame.camera
            val trackingState = camera.trackingState
            
            when (trackingState) {
                TrackingState.TRACKING -> {
                    _trackingGuidance.value = "Tracking excellent"
                }
                TrackingState.PAUSED -> {
                    Log.w(tag, "Tracking paused - insufficient features")
                    _trackingQuality.value = TrackingQuality.POOR
                    _trackingGuidance.value = "Move camera slowly to find more features"
                }
                TrackingState.STOPPED -> {
                    Log.w(tag, "Tracking stopped - VIO reset")
                    trackingLossCount++
                    _trackingQuality.value = TrackingQuality.POOR
                    _trackingGuidance.value = "Tracking lost. Move camera slowly to reinitialize"
                }
            }
            
            // Update tracking state
            _trackingState.value = trackingState
            
        } catch (e: Exception) {
            Log.e(tag, "Error updating tracking state", e)
        }
    }
    
    /**
     * Analyze motion stability to prevent VIO resets
     */
    private fun analyzeMotionStability(frame: Frame) {
        lastPose?.let { lastPose ->
            val translation = frame.camera.pose.translation
            val lastTranslation = lastPose.translation
            
            val motion = sqrt(
                (translation[0] - lastTranslation[0]).toDouble().pow(2) +
                (translation[1] - lastTranslation[1]).toDouble().pow(2) +
                (translation[2] - lastTranslation[2]).toDouble().pow(2)
            ).toFloat()
            
            motionHistory.add(motion)
            if (motionHistory.size > MOTION_HISTORY_SIZE) {
                motionHistory.removeAt(0)
            }
            
            val avgMotion = motionHistory.average().toFloat()
            
            _motionStability.value = when {
                avgMotion < 0.02f -> MotionStability.STABLE
                avgMotion < 0.05f -> MotionStability.MODERATE
                else -> MotionStability.UNSTABLE
            }
            
            if (avgMotion > MAX_MOTION_THRESHOLD) {
                _trackingGuidance.value = "Move camera more slowly for better tracking"
            }
        }
        
        lastPose = frame.camera.pose
    }
    
    /**
     * Analyze lighting quality using ARCore light estimation
     * Performance optimized with throttling
     */
    private fun analyzeLightEstimation(frame: Frame) {
        // Performance optimization: Throttle lighting updates
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLightingUpdate < LIGHTING_UPDATE_INTERVAL) {
            return
        }
        lastLightingUpdate = currentTime
        
        try {
            val lightEstimate = frame.lightEstimate
            _lightEstimate.value = lightEstimate
            
            // Update lighting quality based on light estimate state and pixel intensity
            _lightingQuality.value = when (lightEstimate.state) {
                LightEstimate.State.NOT_VALID -> LightingQuality.POOR
                LightEstimate.State.VALID -> {
                    val pixelIntensity = lightEstimate.pixelIntensity
                    when {
                        pixelIntensity > 0.7f -> LightingQuality.EXCELLENT
                        pixelIntensity > 0.4f -> LightingQuality.GOOD
                        pixelIntensity > 0.2f -> LightingQuality.FAIR
                        else -> LightingQuality.POOR
                    }
                }
                else -> LightingQuality.UNKNOWN
            }
            
            if (_lightingQuality.value == LightingQuality.POOR) {
                _trackingGuidance.value = "Poor lighting detected. Move to a well-lit area for better avatar rendering"
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Error analyzing light estimation", e)
        }
    }
    
    /**
     * Update tracking quality based on all metrics
     */
    private fun updateTrackingQuality() {
        val quality = when {
            _motionStability.value == MotionStability.STABLE && 
            _lightingQuality.value == LightingQuality.GOOD -> TrackingQuality.EXCELLENT
            _motionStability.value == MotionStability.STABLE && 
            _lightingQuality.value == LightingQuality.FAIR -> TrackingQuality.GOOD
            _motionStability.value == MotionStability.MODERATE -> TrackingQuality.FAIR
            else -> TrackingQuality.POOR
        }
        
        _trackingQuality.value = quality
        
        // Provide guidance based on quality
        when (quality) {
            TrackingQuality.EXCELLENT -> _trackingGuidance.value = "Tracking excellent"
            TrackingQuality.GOOD -> _trackingGuidance.value = "Tracking good"
            TrackingQuality.FAIR -> _trackingGuidance.value = "Tracking fair - try to hold camera steadier"
            TrackingQuality.POOR -> _trackingGuidance.value = "Tracking poor - improve lighting and hold camera steady"
            else -> {}
        }
    }
    
    /**
     * Process augmented images with enhanced validation
     */
    private fun processAugmentedImages(frame: Frame) {
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
            Log.e(tag, "Error processing augmented images", e)
        }
    }
    
    /**
     * Handle recognized image with enhanced processing
     */
    private fun handleImageRecognized(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name ?: "unknown"
        
        if (recognizedImageCache.containsKey(imageName)) {
            return
        }
        
        try {
            val imageRecognition = ImageRecognition(
                id = imageName,
                name = imageName,
                description = "Recognized image: $imageName",
                imageUrl = "",
                dialogues = emptyList(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            recognizedImageCache[imageName] = imageRecognition
            
            val currentImages = _recognizedImages.value.toMutableList()
            currentImages.add(augmentedImage)
            _recognizedImages.value = currentImages
            
            Log.d(tag, "Image recognized with enhanced tracking: $imageName")
            
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
     * Pause tracking
     */
    fun pauseTracking() {
        try {
            session?.pause()
            _isTracking.value = false
            _trackingState.value = TrackingState.PAUSED
            Log.d(tag, "Enhanced ARCore tracking paused")
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
            _trackingState.value = TrackingState.TRACKING
            Log.d(tag, "Enhanced ARCore tracking resumed")
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume tracking", e)
        }
    }
    
    /**
     * Stop tracking and cleanup
     */
    fun stopTracking() {
        try {
            // Stop ambient audio
            stopAmbientAudio()
            
            session?.close()
            session = null
            imageDatabase = null
            _isTracking.value = false
            _trackingState.value = TrackingState.STOPPED
            _recognizedImages.value = emptyList()
            recognizedImageCache.clear()
            
            // Reset metrics
            motionHistory.clear()
            featureCountHistory.clear()
            trackingLossCount = 0
            frameCount = 0
            
            Log.d(tag, "Enhanced ARCore tracking stopped and cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop tracking", e)
        }
    }
    
    /**
     * Get current tracking state
     */
    fun getTrackingState(): String {
        return when (_trackingState.value) {
            TrackingState.TRACKING -> "TRACKING"
            TrackingState.PAUSED -> "PAUSED"
            TrackingState.STOPPED -> "STOPPED"
        }
    }
    
    /**
     * Get tracking metrics for debugging
     */
    fun getTrackingMetrics(): Map<String, Any> {
        return mapOf(
            "trackingState" to getTrackingState(),
            "trackingQuality" to _trackingQuality.value.name,
            "lightingQuality" to _lightingQuality.value.name,
            "motionStability" to _motionStability.value.name,
            "trackingLossCount" to trackingLossCount,
            "frameCount" to frameCount,
            "recognizedImagesCount" to _recognizedImages.value.size
        )
    }
    
    /**
     * Check if ARCore service is properly initialized
     */
    fun isInitialized(): Boolean {
        return session != null && imageDatabase != null
    }
    
    /**
     * Post an error message to be displayed in the UI
     */
    fun postError(errorMessage: String) {
        _error.value = errorMessage
    }
    
    /**
     * Set avatar speaking state to control ambient audio
     */
    fun setAvatarSpeaking(isSpeaking: Boolean) {
        if (_isAvatarSpeaking.value != isSpeaking) {
            _isAvatarSpeaking.value = isSpeaking
            
            // Control ambient audio based on avatar speaking state
            if (isSpeaking) {
                // Fade out ambient audio when avatar is speaking
                ambientAudioService?.fadeOut()
            } else {
                // Fade in ambient audio when avatar is not speaking
                ambientAudioService?.startAmbientAudio()
            }
            
            Log.d(tag, "Avatar speaking state changed to: $isSpeaking")
        }
    }
    
    /**
     * Start ambient background audio
     */
    fun startAmbientAudio() {
        if (!_isAvatarSpeaking.value) {
            ambientAudioService?.startAmbientAudio()
        }
    }
    
    /**
     * Stop ambient audio
     */
    fun stopAmbientAudio() {
        ambientAudioService?.stop()
    }
    
    /**
     * Pause ambient audio
     */
    fun pauseAmbientAudio() {
        ambientAudioService?.pause()
    }
    
    /**
     * Resume ambient audio
     */
    fun resumeAmbientAudio() {
        if (!_isAvatarSpeaking.value) {
            ambientAudioService?.resume()
        }
    }
}

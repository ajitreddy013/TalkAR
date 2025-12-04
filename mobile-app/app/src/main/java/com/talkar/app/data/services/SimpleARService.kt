package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Data class representing tracking quality information
 */
data class TrackingQuality(
    val isStable: Boolean,
    val reason: String
)

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
    
    // VIO Debug Helper
    private val vioDebugHelper = VioDebugHelper()
    
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

    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Throttle frequent per-frame success logs to avoid flooding logs (milliseconds)
    private var lastFrameSuccessLogTime = 0L
    private val frameSuccessLogInterval = 2_000L // 2 seconds
    
    // VIO initialization retry parameters
    private var vioInitializationAttempts = 0
    private val maxVioInitializationAttempts = 15
    private val vioRetryDelay = 1000L // 1 second
    
    /**
     * Initialize simple AR service with minimal configuration
     */
    suspend fun initialize(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            // Create session with proper configuration
            session = Session(context)
            
            // Wait a bit for GL context to be available
            delay(1500) // Increased delay to ensure GL context is ready
            
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
     * Initialize simple AR service with offline fallback
     */
    suspend fun initializeWithOfflineFallback(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            // Create session with proper configuration
            session = Session(context)
            
            // Wait a bit for GL context to be available
            delay(2000) // Increased delay to ensure GL context is ready
            
            configureSimpleSession()
            
            // Try to initialize image database with fallback
            try {
                initializeSimpleImageDatabase()
            } catch (networkException: Exception) {
                Log.w(tag, "Network initialization failed, using fallback local images", networkException)
                vioDebugHelper.logNetworkIssues(networkException)
                initializeLocalImageDatabase()
            }
            
            startSimpleTracking()
            
            Log.d(tag, "Simple AR Service initialized successfully with offline fallback")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize simple AR service with offline fallback", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Initialize simple AR service with completely offline mode
     * This method bypasses all network-dependent operations
     */
    suspend fun initializeOfflineOnly(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            // Create session with proper configuration
            session = Session(context)
            
            // Wait a bit for GL context to be available
            delay(1000)
            
            // Configure session with minimal offline settings
            configureSimpleSessionOffline()
            
            // Initialize with completely empty database (no image downloads)
            imageDatabase = AugmentedImageDatabase(session)
            
            startSimpleTracking()
            
            Log.d(tag, "Simple AR Service initialized successfully in offline-only mode")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize simple AR service in offline mode", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Initialize ARCore session with GL context support
     */
    suspend fun initializeWithGLContext(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            // Create session
            session = Session(context)
            
            // Wait for GL context to be ready
            var attempts = 0
            val maxAttempts = 30 // Increased attempts
            
            while (attempts < maxAttempts) {
                try {
                    // Try to configure the session
                    configureSimpleSession()
                    initializeSimpleImageDatabase()
                    
                    // Test if GL context is ready by trying to resume
                    session?.resume()
                    
                    Log.d(tag, "ARCore session initialized with GL context support")
                    return true
                    
                } catch (e: com.google.ar.core.exceptions.MissingGlContextException) {
                    Log.d(tag, "GL context not ready, attempt ${attempts + 1}/$maxAttempts")
                    delay(1000) // Increased delay
                    attempts++
                } catch (e: Exception) {
                    Log.e(tag, "Error during GL context initialization", e)
                    delay(1000)
                    attempts++
                }
            }
            
            _error.value = "Failed to initialize ARCore GL context after $maxAttempts attempts"
            false
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize ARCore with GL context", e)
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
            
            // ENABLE lightweight plane detection to help with landmark detection
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            
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
     * Configure session with minimal settings for offline mode
     */
    private fun configureSimpleSessionOffline() {
        session?.let { session ->
            val config = Config(session)
            
            // Minimal configuration to avoid depth estimation problems
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            
            // DISABLE depth mode to avoid depth estimation failures
            config.depthMode = Config.DepthMode.DISABLED
            
            // ENABLE lightweight plane detection to help with landmark detection
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            
            // DISABLE instant placement
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            
            // DISABLE cloud anchors
            config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            
            // IMPORTANT: No image database for offline mode - we'll detect whatever images we can
            
            session.configure(config)
            Log.d(tag, "ARCore session configured with offline settings")
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
            // Even if we can't create test images, we can still initialize with an empty database
            imageDatabase = AugmentedImageDatabase(session)
            Log.w(tag, "Initialized with empty image database due to image creation errors")
        }
    }
    
    /**
     * Initialize local image database as fallback
     */
    private fun initializeLocalImageDatabase() {
        try {
            imageDatabase = AugmentedImageDatabase(session)
            
            // Create local test images that don't require network
            createLocalTestImages()
            
            // Apply to session config
            session?.let { session ->
                val config = session.config
                imageDatabase?.let { database ->
                    config.augmentedImageDatabase = database
                }
                session.configure(config)
            }
            
            Log.d(tag, "Local image database initialized as fallback")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize local image database", e)
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
                "simple_test_3" to createSimpleTestPattern("SIMPLE TEST 3"),
                "simple_test_4" to createEnhancedTestPattern("SIMPLE TEST 4"), // Added enhanced pattern
                "simple_test_5" to createEnhancedTestPattern("SIMPLE TEST 5")  // Added enhanced pattern
            )
            
            var imagesAdded = 0
            testImages.forEach { (name, bitmap) ->
                try {
                    if (validateSimpleImageQuality(bitmap)) {
                        imageDatabase?.addImage(name, bitmap)
                        Log.d(tag, "Added simple test image: $name")
                        imagesAdded++
                    } else {
                        Log.w(tag, "Simple test image failed quality validation: $name")
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Failed to add simple test image $name to database", e)
                }
            }
            
            Log.d(tag, "Successfully added $imagesAdded test images to database")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to create simple test images", e)
            // Don't throw the exception, let the service continue with an empty database
        }
    }
    
    /**
     * Create local test images for offline use
     */
    private fun createLocalTestImages() {
        try {
            val testImages = listOf(
                "local_test_1" to createHighContrastPattern("LOCAL 1"),
                "local_test_2" to createHighContrastPattern("LOCAL 2"),
                "local_test_3" to createHighContrastPattern("LOCAL 3")
            )
            
            testImages.forEach { (name, bitmap) ->
                imageDatabase?.addImage(name, bitmap)
                Log.d(tag, "Added local test image: $name")
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to create local test images", e)
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
     * Create enhanced test pattern with more visual features for better landmark detection
     */
    private fun createEnhancedTestPattern(text: String): android.graphics.Bitmap {
        val size = 1024
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }
        
        // Checkerboard background for rich visual features
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val gridSize = size / 16
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                if ((x + y) % 2 == 0) {
                    val left = x * gridSize
                    val top = y * gridSize
                    val right = left + gridSize
                    val bottom = top + gridSize
                    canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
                }
            }
        }
        
        // Concentric circles for additional features
        val centerX = size / 2f
        val centerY = size / 2f
        for (i in 1..8) {
            val radius = i * (size / 20f)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 4f
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
        
        // Diagonal lines
        paint.strokeWidth = 6f
        canvas.drawLine(0f, 0f, size.toFloat(), size.toFloat(), paint)
        canvas.drawLine(size.toFloat(), 0f, 0f, size.toFloat(), paint)
        
        // Text with higher contrast
        paint.color = android.graphics.Color.WHITE
        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = 64f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(text, centerX, centerY + 25f, paint)
        
        return bitmap
    }
    
    /**
     * Create high contrast pattern optimized for tracking without network
     */
    private fun createHighContrastPattern(text: String): android.graphics.Bitmap {
        val size = 512 // Smaller size for faster processing
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }
        
        // Black and white checkerboard for maximum contrast
        val gridSize = size / 8
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                paint.color = if ((x + y) % 2 == 0) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                val left = x * gridSize
                val top = y * gridSize
                val right = left + gridSize
                val bottom = top + gridSize
                canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            }
        }
        
        // Bold white text on black background
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 48f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText(text, size / 2f, size / 2f + 15f, paint)
        
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
            if (contrast < 0.15) { // Reduced contrast requirement
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
        coroutineScope.launch {
            var glContextWaitTime = 0L
            val maxGlContextWaitTime = 15000L // 15 seconds max wait (increased)
            
            while (_isTracking.value) {
                try {
                    session?.let { session ->
                        // Attempt to update the session - this will throw MissingGlContextException if GL context isn't ready
                        val frame = session.update()
                        processSimpleFrame(frame)
                        
                        // Reset VIO initialization attempts counter on successful frame
                        vioInitializationAttempts = 0
                        
                        // If we get here, GL context is ready and we successfully got a frame
                        val now = System.currentTimeMillis()
                        if (now - lastFrameSuccessLogTime >= frameSuccessLogInterval) {
                            Log.d(tag, "GL context is ready and frame processed successfully")
                            lastFrameSuccessLogTime = now
                        }
                        
                    }
                    
                    delay(50) // Faster processing for better tracking (reduced from 100)
                } catch (e: com.google.ar.core.exceptions.MissingGlContextException) {
                    Log.w(tag, "Missing GL context, waiting for initialization... (${glContextWaitTime}ms)")
                    
                    // Check if we've been waiting too long
                    if (glContextWaitTime >= maxGlContextWaitTime) {
                        Log.e(tag, "GL context not ready after ${maxGlContextWaitTime}ms")
                        _error.value = "ARCore GL context initialization timeout. Please restart the app."
                        _isTracking.value = false
                        return@launch
                    }
                    
                    delay(500)
                    glContextWaitTime += 500
                } catch (e: com.google.ar.core.exceptions.SessionPausedException) {
                    Log.d(tag, "ARCore session paused, waiting...")
                    delay(1000)
                } catch (e: com.google.ar.core.exceptions.UnavailableException) {
                    Log.w(tag, "ARCore unavailable, retrying...")
                    delay(2000)
                } catch (e: Exception) {
                    // Handle VIO initialization failures specifically
                    if (e.message?.contains("Image has too few landmarks") == true) {
                        vioInitializationAttempts++
                        
                        // Extract landmark information from error message
                        val requiredLandmarks = extractNumberFromMessage(e.message, "Required:") ?: 9
                        val actualLandmarks = extractNumberFromMessage(e.message, "Actual:") ?: 0
                        
                        // Log detailed VIO initialization failure
                        vioDebugHelper.logVioInitializationFailure(
                            e.message ?: "Unknown VIO error",
                            requiredLandmarks,
                            actualLandmarks
                        )
                        
                        Log.w(tag, "VIO initialization failed due to insufficient landmarks. Attempt $vioInitializationAttempts/$maxVioInitializationAttempts")
                        Log.w(tag, vioDebugHelper.getLandmarkRecommendations(actualLandmarks, requiredLandmarks))
                        
                        // Retry initialization if we haven't exceeded max attempts
                        if (vioInitializationAttempts < maxVioInitializationAttempts) {
                            Log.d(tag, "Retrying VIO initialization in ${vioRetryDelay}ms...")
                            delay(vioRetryDelay)
                            
                            // Try to reconfigure session
                            try {
                                configureSimpleSession()
                                session?.resume()
                            } catch (reconfigException: Exception) {
                                Log.e(tag, "Failed to reconfigure session", reconfigException)
                            }
                        } else {
                            Log.e(tag, "VIO initialization failed after $maxVioInitializationAttempts attempts")
                            _error.value = "AR tracking failed to initialize. Please point camera at textured surfaces with good lighting."
                            _isTracking.value = false
                            return@launch
                        }
                    } else if (e.message?.contains("VIO is moving fast") == true) {
                        // Handle VIO motion tracking issues
                        val speed = extractFloatFromMessage(e.message, "speed (m/s):") ?: 2.0f
                        val duration = extractDurationFromMessage(e.message) ?: "unknown"
                        
                        vioDebugHelper.logVioMotionIssues(speed, duration)
                        Log.w(tag, "VIO motion tracking issue detected. Recommend holding device more steadily.")
                        
                        // Continue processing but with reduced frequency
                        delay(200)
                    } else {
                        Log.e(tag, "Error in simple frame processing", e)
                        delay(500)
                    }
                }
            }
        }
    }
    
    /**
     * Extract number from error message
     */
    private fun extractNumberFromMessage(message: String?, prefix: String): Int? {
        return try {
            if (message != null) {
                val startIndex = message.indexOf(prefix)
                if (startIndex != -1) {
                    val endIndex = message.indexOf(".", startIndex)
                    val numberStr = if (endIndex != -1) {
                        message.substring(startIndex + prefix.length, endIndex).trim()
                    } else {
                        message.substring(startIndex + prefix.length).trim()
                    }
                    numberStr.toIntOrNull()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error extracting number from message", e)
            null
        }
    }
    
    /**
     * Extract float from error message
     */
    private fun extractFloatFromMessage(message: String?, prefix: String): Float? {
        return try {
            if (message != null) {
                val startIndex = message.indexOf(prefix)
                if (startIndex != -1) {
                    val start = startIndex + prefix.length
                    val endIndex = message.indexOf(" ", start)
                    val numberStr = if (endIndex != -1) {
                        message.substring(start, endIndex).trim()
                    } else {
                        message.substring(start).trim()
                    }
                    numberStr.toFloatOrNull()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error extracting float from message", e)
            null
        }
    }
    
    /**
     * Extract duration from error message
     */
    private fun extractDurationFromMessage(message: String?): String? {
        return try {
            if (message != null) {
                val startIndex = message.indexOf("duration:")
                if (startIndex != -1) {
                    val start = startIndex + "duration:".length
                    val endIndex = message.indexOf("ms", start)
                    if (endIndex != -1) {
                        "${message.substring(start, endIndex + 2).trim()}"
                    } else {
                        message.substring(start).trim()
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error extracting duration from message", e)
            null
        }
    }
    
    /**
     * Process frame with simple recognition and stabilization
     */
    private fun processSimpleFrame(frame: Frame) {
        try {
            val camera = frame.camera
            val trackingState = camera.trackingState
            
            // Check camera pose quality for stabilization
            val pose = camera.pose
            val trackingQuality = checkTrackingQuality(frame)
            
            when (trackingState) {
                TrackingState.TRACKING -> {
                    // Only process recognition if tracking quality is acceptable
                    if (trackingQuality.isStable) {
                        processSimpleRecognition(frame)
                    } else {
                        Log.w(tag, "Skipping recognition due to unstable tracking: ${trackingQuality.reason}")
                    }
                }
                TrackingState.PAUSED -> {
                    Log.w(tag, "Simple tracking paused")
                }
                TrackingState.STOPPED -> {
                    Log.w(tag, "Simple tracking stopped")
                    // Attempt to recover from stopped state
                    handleTrackingStopped()
                }
            }
            
            _trackingState.value = trackingState
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing simple frame", e)
        }
    }
    
    /**
     * Check tracking quality for stabilization
     */
    private fun checkTrackingQuality(frame: Frame): TrackingQuality {
        try {
            val camera = frame.camera
            val pose = camera.pose
            
            // Check if device is moving too fast
            val velocity = estimateDeviceVelocity(frame)
            if (velocity > 2.0f) { // meters per second
                return TrackingQuality(false, "Device moving too fast (${String.format("%.2f", velocity)} m/s)")
            }
            
            // Check tracking confidence
            val trackingConfidence = getTrackingConfidence(camera)
            if (trackingConfidence < 0.5f) {
                return TrackingQuality(false, "Low tracking confidence (${String.format("%.2f", trackingConfidence)})")
            }
            
            return TrackingQuality(true, "Stable tracking")
            
        } catch (e: Exception) {
            Log.e(tag, "Error checking tracking quality", e)
            return TrackingQuality(true, "Unable to determine quality - assuming stable")
        }
    }
    
    /**
     * Estimate device velocity for stabilization
     */
    private fun estimateDeviceVelocity(frame: Frame): Float {
        // Simplified velocity estimation
        // In a real implementation, you would use IMU data or optical flow
        return 0.5f // Conservative estimate
    }
    
    /**
     * Get tracking confidence level
     */
    private fun getTrackingConfidence(camera: Camera): Float {
        // Return a conservative confidence value
        return when (camera.trackingState) {
            TrackingState.TRACKING -> 0.8f
            TrackingState.PAUSED -> 0.3f
            TrackingState.STOPPED -> 0.1f
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
     * Handle tracking stopped state by attempting recovery
     */
    private fun handleTrackingStopped() {
        try {
            // Try to resume tracking
            session?.resume()
            Log.d(tag, "Attempted to resume tracking after stopped state")
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume tracking", e)
        }
    }
    
    /**
     * Get simple metrics
     */
    fun getSimpleMetrics(): Map<String, Any> {
        return mapOf(
            "trackingState" to _trackingState.value.name,
            "recognitionConfidence" to _recognitionConfidence.value,
            "recognizedImagesCount" to _recognizedImages.value.size,
            "isInitialized" to isInitialized(),
            "vioInitializationAttempts" to vioInitializationAttempts
        )
    }
    
    /**
     * Provide user guidance for VIO initialization issues
     */
    fun getVioInitializationGuidance(): String {
        return "Ensure good lighting and hold device steady during initialization"
    }
    
    /**
     * Get the ARCore session
     */
    fun getSession(): Session? {
        return session
    }
    
    /**
     * Process a frame for image recognition
     */
    fun processFrame(frame: Frame) {
        processSimpleFrame(frame)
    }
}
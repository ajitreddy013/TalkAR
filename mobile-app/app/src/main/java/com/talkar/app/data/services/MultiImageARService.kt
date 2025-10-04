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
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.pow

/**
 * Multi-Image AR Service that uses multiple reference images for better recognition
 * Addresses the limitation of single-image matching by providing:
 * - Multiple reference images per object
 * - Different angles and lighting conditions
 * - Fallback options for better tracking stability
 * - Enhanced recognition accuracy
 */
class MultiImageARService(private val context: Context) {
    
    private val tag = "MultiImageARService"
    private var session: Session? = null
    private var imageDatabase: AugmentedImageDatabase? = null
    
    // Tracking state management
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    private val _recognizedImages = MutableStateFlow<List<AugmentedImage>>(emptyList())
    val recognizedImages: StateFlow<List<AugmentedImage>> = _recognizedImages.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Multi-image recognition metrics
    private val _recognitionConfidence = MutableStateFlow(0f)
    val recognitionConfidence: StateFlow<Float> = _recognitionConfidence.asStateFlow()
    
    private val _activeReferenceImages = MutableStateFlow<List<String>>(emptyList())
    val activeReferenceImages: StateFlow<List<String>> = _activeReferenceImages.asStateFlow()
    
    private val _recognitionStats = MutableStateFlow(RecognitionStats())
    val recognitionStats: StateFlow<RecognitionStats> = _recognitionStats.asStateFlow()
    
    // Cache for recognized images
    private val recognizedImageCache = ConcurrentHashMap<String, ImageRecognition>()
    
    // Multi-image database
    private val referenceImageGroups = ConcurrentHashMap<String, List<String>>()
    
    data class RecognitionStats(
        val totalRecognitions: Int = 0,
        val successfulMatches: Int = 0,
        val averageConfidence: Float = 0f,
        val bestPerformingImage: String = "",
        val recognitionRate: Float = 0f
    )
    
    /**
     * Initialize multi-image ARCore session
     */
    suspend fun initialize(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            session = Session(context)
            configureSessionForMultiImage()
            initializeMultiImageDatabase()
            startMultiImageTracking()
            
            Log.d(tag, "Multi-Image AR Service initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize multi-image AR service", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Configure session for multi-image recognition
     */
    private fun configureSessionForMultiImage() {
        session?.let { session ->
            val config = Config(session)
            
            // Enhanced configuration for multiple images
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.depthMode = Config.DepthMode.AUTOMATIC
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            
            // Enable cloud anchor mode for better tracking across multiple images
            config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            
            session.configure(config)
            Log.d(tag, "ARCore session configured for multi-image recognition")
        }
    }
    
    /**
     * Initialize database with multiple reference images per object
     */
    private fun initializeMultiImageDatabase() {
        try {
            imageDatabase = AugmentedImageDatabase(session)
            
            // Load multiple reference images for each object
            loadMultiImageReferences()
            
            Log.d(tag, "Multi-image database initialized")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize multi-image database", e)
            throw e
        }
    }
    
    /**
     * Load multiple reference images for better recognition
     */
    private fun loadMultiImageReferences() {
        try {
            // Create multiple reference images for each object
            val imageGroups = mapOf(
                "talkar_logo" to listOf(
                    "talkar_logo_front",
                    "talkar_logo_angle_1", 
                    "talkar_logo_angle_2",
                    "talkar_logo_lighting_1",
                    "talkar_logo_lighting_2"
                ),
                "ar_pattern" to listOf(
                    "ar_pattern_1",
                    "ar_pattern_2", 
                    "ar_pattern_3",
                    "ar_pattern_4",
                    "ar_pattern_5"
                ),
                "test_object" to listOf(
                    "test_object_front",
                    "test_object_side",
                    "test_object_top",
                    "test_object_lighting_1",
                    "test_object_lighting_2"
                )
            )
            
            imageGroups.forEach { (objectName, imageNames) ->
                referenceImageGroups[objectName] = imageNames
                
                imageNames.forEach { imageName ->
                    val bitmap = createMultiAngleTestPattern(imageName, objectName)
                    if (validateImageQuality(bitmap)) {
                        imageDatabase?.addImage(imageName, bitmap)
                        Log.d(tag, "Added multi-angle reference image: $imageName for object: $objectName")
                    } else {
                        Log.w(tag, "Multi-angle image failed quality validation: $imageName")
                    }
                }
            }
            
            Log.d(tag, "Loaded ${imageGroups.values.flatten().size} multi-angle reference images")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to load multi-image references", e)
        }
    }
    
    /**
     * Create test pattern with different angles and lighting conditions
     */
    private fun createMultiAngleTestPattern(imageName: String, objectName: String): android.graphics.Bitmap {
        val size = 2048
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // Base background with variations based on image name
        val backgroundColor = when {
            imageName.contains("lighting_1") -> android.graphics.Color.LTGRAY
            imageName.contains("lighting_2") -> android.graphics.Color.DKGRAY
            imageName.contains("angle") -> android.graphics.Color.WHITE
            else -> android.graphics.Color.WHITE
        }
        
        paint.color = backgroundColor
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        // Create different patterns based on angle/lighting
        when {
            imageName.contains("front") -> createFrontViewPattern(canvas, paint, size, objectName)
            imageName.contains("angle_1") -> createAngle1Pattern(canvas, paint, size, objectName)
            imageName.contains("angle_2") -> createAngle2Pattern(canvas, paint, size, objectName)
            imageName.contains("side") -> createSideViewPattern(canvas, paint, size, objectName)
            imageName.contains("top") -> createTopViewPattern(canvas, paint, size, objectName)
            imageName.contains("lighting_1") -> createLighting1Pattern(canvas, paint, size, objectName)
            imageName.contains("lighting_2") -> createLighting2Pattern(canvas, paint, size, objectName)
            else -> createDefaultPattern(canvas, paint, size, objectName)
        }
        
        return bitmap
    }
    
    /**
     * Create front view pattern
     */
    private fun createFrontViewPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.BLACK
        
        // Central circle
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 6f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Corner markers
        val cornerSize = size / 8f
        canvas.drawRect(0f, 0f, cornerSize, cornerSize, paint)
        canvas.drawRect(size - cornerSize, 0f, size.toFloat(), cornerSize, paint)
        canvas.drawRect(0f, size - cornerSize, cornerSize, size.toFloat(), paint)
        canvas.drawRect(size - cornerSize, size - cornerSize, size.toFloat(), size.toFloat(), paint)
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} FRONT", centerX, centerY - 100f, paint)
    }
    
    /**
     * Create angle 1 pattern
     */
    private fun createAngle1Pattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.BLACK
        
        // Rotated rectangle
        val rectSize = size / 4f
        val centerX = size / 2f
        val centerY = size / 2f
        
        canvas.save()
        canvas.rotate(15f, centerX, centerY)
        canvas.drawRect(centerX - rectSize/2, centerY - rectSize/2, centerX + rectSize/2, centerY + rectSize/2, paint)
        canvas.restore()
        
        // Diagonal lines for angle indication
        paint.strokeWidth = 8f
        canvas.drawLine(0f, 0f, size.toFloat(), size.toFloat(), paint)
        canvas.drawLine(size.toFloat(), 0f, 0f, size.toFloat(), paint)
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} ANGLE 1", centerX, centerY + 100f, paint)
    }
    
    /**
     * Create angle 2 pattern
     */
    private fun createAngle2Pattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.BLACK
        
        // Different rotation
        val rectSize = size / 4f
        val centerX = size / 2f
        val centerY = size / 2f
        
        canvas.save()
        canvas.rotate(-15f, centerX, centerY)
        canvas.drawRect(centerX - rectSize/2, centerY - rectSize/2, centerX + rectSize/2, centerY + rectSize/2, paint)
        canvas.restore()
        
        // Grid pattern
        val gridSize = size / 8f
        for (i in 1..7) {
            val pos = i * gridSize
            canvas.drawLine(pos, 0f, pos, size.toFloat(), paint)
            canvas.drawLine(0f, pos, size.toFloat(), pos, paint)
        }
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} ANGLE 2", centerX, centerY + 100f, paint)
    }
    
    /**
     * Create side view pattern
     */
    private fun createSideViewPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.BLACK
        
        // Side view rectangle
        val rectWidth = size / 3f
        val rectHeight = size / 6f
        val centerX = size / 2f
        val centerY = size / 2f
        
        canvas.drawRect(centerX - rectWidth/2, centerY - rectHeight/2, centerX + rectWidth/2, centerY + rectHeight/2, paint)
        
        // Side view indicators
        paint.strokeWidth = 6f
        canvas.drawLine(centerX - rectWidth/2, centerY, centerX + rectWidth/2, centerY, paint)
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} SIDE", centerX, centerY + 100f, paint)
    }
    
    /**
     * Create top view pattern
     */
    private fun createTopViewPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.BLACK
        
        // Top view circle
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 6f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Cross pattern for top view
        paint.strokeWidth = 8f
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paint)
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint)
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} TOP", centerX, centerY + 100f, paint)
    }
    
    /**
     * Create lighting 1 pattern (bright)
     */
    private fun createLighting1Pattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        // Bright background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        
        // High contrast pattern
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 8f
        
        // Multiple circles for bright lighting
        for (i in 1..5) {
            val currentRadius = radius * i
            canvas.drawCircle(centerX, centerY, currentRadius, paint)
        }
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} BRIGHT", centerX, centerY + 100f, paint)
    }
    
    /**
     * Create lighting 2 pattern (dim)
     */
    private fun createLighting2Pattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        // Dim background
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.LTGRAY
        
        // Subtle pattern for dim lighting
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 6f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Subtle grid
        val gridSize = size / 12f
        for (i in 1..11) {
            val pos = i * gridSize
            canvas.drawLine(pos, 0f, pos, size.toFloat(), paint)
            canvas.drawLine(0f, pos, size.toFloat(), pos, paint)
        }
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} DIM", centerX, centerY + 100f, paint)
    }
    
    /**
     * Create default pattern
     */
    private fun createDefaultPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        
        // Standard pattern
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 6f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Corner squares
        val cornerSize = size / 8f
        canvas.drawRect(0f, 0f, cornerSize, cornerSize, paint)
        canvas.drawRect(size - cornerSize, 0f, size.toFloat(), cornerSize, paint)
        canvas.drawRect(0f, size - cornerSize, cornerSize, size.toFloat(), paint)
        canvas.drawRect(size - cornerSize, size - cornerSize, size.toFloat(), size.toFloat(), paint)
        
        // Add text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} DEFAULT", centerX, centerY + 100f, paint)
    }
    
    /**
     * Enhanced image quality validation for multi-image recognition
     */
    private fun validateImageQuality(bitmap: android.graphics.Bitmap): Boolean {
        try {
            // Check minimum size
            if (bitmap.width < 512 || bitmap.height < 512) {
                return false
            }
            
            // Check contrast
            val contrast = calculateContrast(bitmap)
            if (contrast < 0.3) {
                return false
            }
            
            // Check features
            val featureCount = calculateFeatureCount(bitmap)
            if (featureCount < 80) {
                return false
            }
            
            return true
            
        } catch (e: Exception) {
            Log.e(tag, "Error validating image quality", e)
            return false
        }
    }
    
    /**
     * Calculate image contrast
     */
    private fun calculateContrast(bitmap: android.graphics.Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 5
        
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
     * Calculate feature count
     */
    private fun calculateFeatureCount(bitmap: android.graphics.Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        var featureCount = 0
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = calculateSobelGx(bitmap, x, y)
                val gy = calculateSobelGy(bitmap, x, y)
                val magnitude = sqrt((gx * gx + gy * gy).toDouble())
                
                if (magnitude > 50) {
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
     * Get grayscale value
     */
    private fun getGrayValue(pixel: Int): Int {
        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
    
    /**
     * Start multi-image tracking
     */
    private fun startMultiImageTracking() {
        try {
            session?.let { session ->
                session.resume()
                _isTracking.value = true
                _trackingState.value = TrackingState.TRACKING
                
                startMultiImageMonitoring()
                Log.d(tag, "Multi-image ARCore tracking started")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start multi-image tracking", e)
            _error.value = "Failed to start ARCore tracking: ${e.message}"
        }
    }
    
    /**
     * Start monitoring for multi-image recognition
     */
    private fun startMultiImageMonitoring() {
        GlobalScope.launch(Dispatchers.IO) {
            while (_isTracking.value) {
                try {
                    session?.let { session ->
                        val frame = session.update()
                        processMultiImageFrame(frame)
                    }
                    
                    delay(33) // ~30 FPS
                } catch (e: Exception) {
                    Log.e(tag, "Error in multi-image monitoring", e)
                    delay(100)
                }
            }
        }
    }
    
    /**
     * Process frame with multi-image recognition
     */
    private fun processMultiImageFrame(frame: Frame) {
        try {
            val camera = frame.camera
            val trackingState = camera.trackingState
            
            when (trackingState) {
                TrackingState.TRACKING -> {
                    processMultiImageRecognition(frame)
                }
                TrackingState.PAUSED -> {
                    Log.w(tag, "Multi-image tracking paused")
                }
                TrackingState.STOPPED -> {
                    Log.w(tag, "Multi-image tracking stopped")
                }
            }
            
            _trackingState.value = trackingState
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing multi-image frame", e)
        }
    }
    
    /**
     * Process multi-image recognition
     */
    private fun processMultiImageRecognition(frame: Frame) {
        try {
            val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            
            for (augmentedImage in augmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.TRACKING -> {
                        handleMultiImageRecognized(augmentedImage)
                    }
                    TrackingState.PAUSED -> {
                        Log.d(tag, "Multi-image tracking paused: ${augmentedImage.name}")
                    }
                    TrackingState.STOPPED -> {
                        Log.d(tag, "Multi-image tracking stopped: ${augmentedImage.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing multi-image recognition", e)
        }
    }
    
    /**
     * Handle multi-image recognition with confidence scoring
     */
    private fun handleMultiImageRecognized(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name ?: "unknown"
        
        if (recognizedImageCache.containsKey(imageName)) {
            return
        }
        
        try {
            // Calculate recognition confidence based on tracking quality
            val confidence = calculateRecognitionConfidence(augmentedImage)
            _recognitionConfidence.value = confidence
            
            // Update active reference images
            val objectName = getObjectNameFromImageName(imageName)
            val activeImages = _activeReferenceImages.value.toMutableList()
            if (!activeImages.contains(objectName)) {
                activeImages.add(objectName)
                _activeReferenceImages.value = activeImages
            }
            
            // Update recognition stats
            updateRecognitionStats(imageName, confidence)
            
            // Create image recognition result
            val imageRecognition = ImageRecognition(
                id = objectName,
                name = objectName,
                description = "Multi-image recognized: $objectName (confidence: ${(confidence * 100).toInt()}%)",
                imageUrl = "",
                dialogues = emptyList(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            recognizedImageCache[objectName] = imageRecognition
            
            val currentImages = _recognizedImages.value.toMutableList()
            currentImages.add(augmentedImage)
            _recognizedImages.value = currentImages
            
            Log.d(tag, "Multi-image recognized: $objectName with confidence: $confidence")
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling multi-image recognition", e)
        }
    }
    
    /**
     * Calculate recognition confidence
     */
    private fun calculateRecognitionConfidence(augmentedImage: AugmentedImage): Float {
        // Base confidence on tracking state and image quality
        val baseConfidence = when (augmentedImage.trackingState) {
            TrackingState.TRACKING -> 0.9f
            TrackingState.PAUSED -> 0.6f
            TrackingState.STOPPED -> 0.3f
        }
        
        // Adjust based on image name (different angles/lighting have different confidence)
        val confidenceMultiplier = when {
            augmentedImage.name?.contains("front") == true -> 1.0f
            augmentedImage.name?.contains("angle") == true -> 0.8f
            augmentedImage.name?.contains("lighting") == true -> 0.7f
            else -> 0.9f
        }
        
        return baseConfidence * confidenceMultiplier
    }
    
    /**
     * Get object name from image name
     */
    private fun getObjectNameFromImageName(imageName: String): String {
        return when {
            imageName.startsWith("talkar_logo") -> "talkar_logo"
            imageName.startsWith("ar_pattern") -> "ar_pattern"
            imageName.startsWith("test_object") -> "test_object"
            else -> imageName
        }
    }
    
    /**
     * Update recognition statistics
     */
    private fun updateRecognitionStats(imageName: String, confidence: Float) {
        val currentStats = _recognitionStats.value
        val newTotal = currentStats.totalRecognitions + 1
        val newSuccessful = currentStats.successfulMatches + if (confidence > 0.7f) 1 else 0
        val newAverage = (currentStats.averageConfidence * currentStats.totalRecognitions + confidence) / newTotal
        val newRate = newSuccessful.toFloat() / newTotal
        
        _recognitionStats.value = currentStats.copy(
            totalRecognitions = newTotal,
            successfulMatches = newSuccessful,
            averageConfidence = newAverage,
            bestPerformingImage = if (confidence > currentStats.averageConfidence) imageName else currentStats.bestPerformingImage,
            recognitionRate = newRate
        )
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
        _activeReferenceImages.value = emptyList()
        Log.d(tag, "Cleared multi-image recognition")
    }
    
    /**
     * Pause tracking
     */
    fun pauseTracking() {
        try {
            session?.pause()
            _isTracking.value = false
            _trackingState.value = TrackingState.PAUSED
            Log.d(tag, "Multi-image ARCore tracking paused")
        } catch (e: Exception) {
            Log.e(tag, "Failed to pause multi-image tracking", e)
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
            Log.d(tag, "Multi-image ARCore tracking resumed")
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume multi-image tracking", e)
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
            referenceImageGroups.clear()
            
            Log.d(tag, "Multi-image ARCore tracking stopped and cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop multi-image tracking", e)
        }
    }
    
    /**
     * Get multi-image recognition metrics
     */
    fun getMultiImageMetrics(): Map<String, Any> {
        return mapOf(
            "trackingState" to _trackingState.value.name,
            "recognitionConfidence" to _recognitionConfidence.value,
            "activeReferenceImages" to _activeReferenceImages.value,
            "recognitionStats" to _recognitionStats.value,
            "totalReferenceImages" to referenceImageGroups.values.flatten().size,
            "objectGroups" to referenceImageGroups.keys.toList()
        )
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
}

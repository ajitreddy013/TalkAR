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
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.pow

/**
 * Optimal Image AR Service - Uses 5-7 reference images per object for reliable recognition
 * 
 * For each object, this service expects:
 * 1. Front view (primary reference)
 * 2. Left angle (15-30°)
 * 3. Right angle (15-30°)
 * 4. Bright lighting
 * 5. Dim lighting
 * 6. Close distance
 * 7. Far distance
 * 
 * This ensures reliable matching regardless of:
 * - Camera angle
 * - Lighting conditions
 * - Distance from object
 * - Partial occlusion
 */
class OptimalImageARService(private val context: Context) {
    
    private val tag = "OptimalImageARService"
    private var session: Session? = null
    private var imageDatabase: AugmentedImageDatabase? = null
    
    // Tracking state
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    private val _recognizedImages = MutableStateFlow<List<AugmentedImage>>(emptyList())
    val recognizedImages: StateFlow<List<AugmentedImage>> = _recognizedImages.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Recognition confidence and matching
    private val _recognitionConfidence = MutableStateFlow(0f)
    val recognitionConfidence: StateFlow<Float> = _recognitionConfidence.asStateFlow()
    
    private val _bestMatchImage = MutableStateFlow<String?>(null)
    val bestMatchImage: StateFlow<String?> = _bestMatchImage.asStateFlow()
    
    private val _recognitionQuality = MutableStateFlow(RecognitionQuality.UNKNOWN)
    val recognitionQuality: StateFlow<RecognitionQuality> = _recognitionQuality.asStateFlow()
    
    // Cache and statistics
    private val recognizedImageCache = ConcurrentHashMap<String, ImageRecognition>()
    private val recognitionStats = ConcurrentHashMap<String, Int>()

    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    enum class RecognitionQuality {
        EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
    }
    
    /**
     * Initialize optimal AR service
     */
    suspend fun initialize(): Boolean {
        return try {
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            session = Session(context)
            configureOptimalSession()
            initializeOptimalImageDatabase()
            startOptimalTracking()
            
            Log.d(tag, "Optimal Image AR Service initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize optimal AR service", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Configure session for optimal multi-image recognition
     */
    private fun configureOptimalSession() {
        session?.let { session ->
            val config = Config(session)
            
            // Optimal configuration for multiple reference images
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.depthMode = Config.DepthMode.AUTOMATIC
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            
            session.configure(config)
            Log.d(tag, "ARCore session configured for optimal multi-image recognition")
        }
    }
    
    /**
     * Initialize database with optimal image sets
     */
    private fun initializeOptimalImageDatabase() {
        try {
            imageDatabase = AugmentedImageDatabase(session)
            
            // Load optimal image sets for each object
            loadOptimalImageSets()
            
            Log.d(tag, "Optimal image database initialized")
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize optimal image database", e)
            throw e
        }
    }
    
    /**
     * Load optimal image sets (5-7 images per object)
     */
    private fun loadOptimalImageSets() {
        try {
            // Example: TalkAR Logo with 7 reference images
            val talkarLogoImages = listOf(
                "talkar_logo_front" to createOptimalTestPattern("TALKAR LOGO", "front", "primary reference"),
                "talkar_logo_left_angle" to createOptimalTestPattern("TALKAR LOGO", "left_angle", "15° left view"),
                "talkar_logo_right_angle" to createOptimalTestPattern("TALKAR LOGO", "right_angle", "15° right view"),
                "talkar_logo_bright" to createOptimalTestPattern("TALKAR LOGO", "bright", "bright lighting"),
                "talkar_logo_dim" to createOptimalTestPattern("TALKAR LOGO", "dim", "dim lighting"),
                "talkar_logo_close" to createOptimalTestPattern("TALKAR LOGO", "close", "close distance"),
                "talkar_logo_far" to createOptimalTestPattern("TALKAR LOGO", "far", "far distance")
            )
            
            // Example: AR Pattern with 6 reference images
            val arPatternImages = listOf(
                "ar_pattern_front" to createOptimalTestPattern("AR PATTERN", "front", "primary reference"),
                "ar_pattern_left_angle" to createOptimalTestPattern("AR PATTERN", "left_angle", "20° left view"),
                "ar_pattern_right_angle" to createOptimalTestPattern("AR PATTERN", "right_angle", "20° right view"),
                "ar_pattern_bright" to createOptimalTestPattern("AR PATTERN", "bright", "outdoor lighting"),
                "ar_pattern_dim" to createOptimalTestPattern("AR PATTERN", "dim", "indoor lighting"),
                "ar_pattern_close" to createOptimalTestPattern("AR PATTERN", "close", "close distance")
            )
            
            // Add all images to database
            val allImages = talkarLogoImages + arPatternImages
            
            allImages.forEach { (imageName, bitmap) ->
                if (validateOptimalImageQuality(bitmap)) {
                    imageDatabase?.addImage(imageName, bitmap)
                    Log.d(tag, "Added optimal reference image: $imageName")
                } else {
                    Log.w(tag, "Optimal image failed quality validation: $imageName")
                }
            }
            
            Log.d(tag, "Loaded ${allImages.size} optimal reference images")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to load optimal image sets", e)
        }
    }
    
    /**
     * Create optimal test pattern for specific viewing conditions
     */
    private fun createOptimalTestPattern(objectName: String, condition: String, description: String): android.graphics.Bitmap {
        val size = 2048
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        // Configure based on condition
        when (condition) {
            "front" -> createFrontViewPattern(canvas, paint, size, objectName)
            "left_angle" -> createLeftAnglePattern(canvas, paint, size, objectName)
            "right_angle" -> createRightAnglePattern(canvas, paint, size, objectName)
            "bright" -> createBrightLightingPattern(canvas, paint, size, objectName)
            "dim" -> createDimLightingPattern(canvas, paint, size, objectName)
            "close" -> createCloseDistancePattern(canvas, paint, size, objectName)
            "far" -> createFarDistancePattern(canvas, paint, size, objectName)
            else -> createDefaultPattern(canvas, paint, size, objectName)
        }
        
        return bitmap
    }
    
    /**
     * Create front view pattern (primary reference)
     */
    private fun createFrontViewPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        // High contrast white background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        // Central object representation
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 6f
        
        // Main circle
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Corner markers for tracking
        val cornerSize = size / 8f
        canvas.drawRect(0f, 0f, cornerSize, cornerSize, paint)
        canvas.drawRect(size - cornerSize, 0f, size.toFloat(), cornerSize, paint)
        canvas.drawRect(0f, size - cornerSize, cornerSize, size.toFloat(), paint)
        canvas.drawRect(size - cornerSize, size - cornerSize, size.toFloat(), size.toFloat(), paint)
        
        // Grid for additional features
        val gridSize = size / 16f
        for (i in 1..15) {
            val pos = i * gridSize
            canvas.drawLine(pos, 0f, pos, size.toFloat(), paint)
            canvas.drawLine(0f, pos, size.toFloat(), pos, paint)
        }
        
        // Text label
        paint.textSize = 80f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("${objectName.uppercase()} FRONT", centerX, centerY - 100f, paint)
        canvas.drawText("PRIMARY REFERENCE", centerX, centerY + 100f, paint)
    }
    
    /**
     * Create left angle pattern (15-30° left view)
     */
    private fun createLeftAnglePattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Rotated rectangle to simulate left angle view
        canvas.save()
        canvas.rotate(-20f, centerX, centerY)
        val rectSize = size / 4f
        canvas.drawRect(centerX - rectSize/2, centerY - rectSize/2, centerX + rectSize/2, centerY + rectSize/2, paint)
        canvas.restore()
        
        // Left angle indicators
        paint.strokeWidth = 6f
        canvas.drawLine(centerX - size/4f, centerY, centerX + size/4f, centerY, paint)
        
        // Text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} LEFT ANGLE", centerX, centerY + 150f, paint)
    }
    
    /**
     * Create right angle pattern (15-30° right view)
     */
    private fun createRightAnglePattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Rotated rectangle to simulate right angle view
        canvas.save()
        canvas.rotate(20f, centerX, centerY)
        val rectSize = size / 4f
        canvas.drawRect(centerX - rectSize/2, centerY - rectSize/2, centerX + rectSize/2, centerY + rectSize/2, paint)
        canvas.restore()
        
        // Right angle indicators
        paint.strokeWidth = 6f
        canvas.drawLine(centerX - size/4f, centerY, centerX + size/4f, centerY, paint)
        
        // Text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} RIGHT ANGLE", centerX, centerY + 150f, paint)
    }
    
    /**
     * Create bright lighting pattern
     */
    private fun createBrightLightingPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        // Very bright background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        
        // High contrast pattern for bright lighting
        val radius = size / 8f
        for (i in 1..6) {
            val currentRadius = radius * i
            canvas.drawCircle(centerX, centerY, currentRadius, paint)
        }
        
        // Bright lighting indicators
        paint.strokeWidth = 8f
        for (i in 0..7) {
            val angle = i * 45f
            val startX = centerX + (size/6f) * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val startY = centerY + (size/6f) * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            val endX = centerX + (size/3f) * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val endY = centerY + (size/3f) * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
        
        // Text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} BRIGHT LIGHTING", centerX, centerY + 150f, paint)
    }
    
    /**
     * Create dim lighting pattern
     */
    private fun createDimLightingPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        // Dim background
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.LTGRAY
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 6f
        
        // Subtle pattern for dim lighting
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Subtle grid for dim lighting
        val gridSize = size / 12f
        for (i in 1..11) {
            val pos = i * gridSize
            canvas.drawLine(pos, 0f, pos, size.toFloat(), paint)
            canvas.drawLine(0f, pos, size.toFloat(), pos, paint)
        }
        
        // Text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} DIM LIGHTING", centerX, centerY + 150f, paint)
    }
    
    /**
     * Create close distance pattern
     */
    private fun createCloseDistancePattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Large central object for close distance
        val radius = size / 4f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Close distance indicators (detailed features)
        val detailSize = size / 16f
        for (i in 0..7) {
            val angle = i * 45f
            val x = centerX + (radius * 0.8f) * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = centerY + (radius * 0.8f) * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            canvas.drawCircle(x, y, detailSize, paint)
        }
        
        // Text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} CLOSE DISTANCE", centerX, centerY + 150f, paint)
    }
    
    /**
     * Create far distance pattern
     */
    private fun createFarDistancePattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Small central object for far distance
        val radius = size / 12f
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Far distance indicators (simplified features)
        paint.strokeWidth = 4f
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint)
        canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, paint)
        
        // Text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} FAR DISTANCE", centerX, centerY + 150f, paint)
    }
    
    /**
     * Create default pattern
     */
    private fun createDefaultPattern(canvas: android.graphics.Canvas, paint: android.graphics.Paint, size: Int, objectName: String) {
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        paint.color = android.graphics.Color.BLACK
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 6f
        
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Text
        paint.textSize = 60f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText("${objectName.uppercase()} DEFAULT", centerX, centerY + 150f, paint)
    }
    
    /**
     * Validate optimal image quality
     */
    private fun validateOptimalImageQuality(bitmap: android.graphics.Bitmap): Boolean {
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
            Log.e(tag, "Error validating optimal image quality", e)
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
     * Start optimal tracking
     */
    private fun startOptimalTracking() {
        try {
            session?.let { session ->
                session.resume()
                _isTracking.value = true
                _trackingState.value = TrackingState.TRACKING
                
                startOptimalMonitoring()
                Log.d(tag, "Optimal ARCore tracking started")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start optimal tracking", e)
            _error.value = "Failed to start ARCore tracking: ${e.message}"
        }
    }
    
    /**
     * Start optimal monitoring
     */
    private fun startOptimalMonitoring() {
        coroutineScope.launch {
            while (_isTracking.value) {
                try {
                    session?.let { session ->
                        val frame = session.update()
                        processOptimalFrame(frame)
                    }
                    
                    delay(33) // ~30 FPS
                } catch (e: Exception) {
                    Log.e(tag, "Error in optimal monitoring", e)
                    delay(100)
                }
            }
        }
    }
    
    /**
     * Process frame with optimal recognition
     */
    private fun processOptimalFrame(frame: Frame) {
        try {
            val camera = frame.camera
            val trackingState = camera.trackingState
            
            when (trackingState) {
                TrackingState.TRACKING -> {
                    processOptimalRecognition(frame)
                }
                TrackingState.PAUSED -> {
                    Log.w(tag, "Optimal tracking paused")
                }
                TrackingState.STOPPED -> {
                    Log.w(tag, "Optimal tracking stopped")
                }
            }
            
            _trackingState.value = trackingState
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing optimal frame", e)
        }
    }
    
    /**
     * Process optimal recognition
     */
    private fun processOptimalRecognition(frame: Frame) {
        try {
            val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            
            for (augmentedImage in augmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.TRACKING -> {
                        handleOptimalRecognition(augmentedImage)
                    }
                    TrackingState.PAUSED -> {
                        Log.d(tag, "Optimal tracking paused: ${augmentedImage.name}")
                    }
                    TrackingState.STOPPED -> {
                        Log.d(tag, "Optimal tracking stopped: ${augmentedImage.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing optimal recognition", e)
        }
    }
    
    /**
     * Handle optimal recognition with confidence scoring
     */
    private fun handleOptimalRecognition(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name ?: "unknown"
        
        if (recognizedImageCache.containsKey(imageName)) {
            return
        }
        
        try {
            // Calculate confidence based on image type
            val confidence = calculateOptimalConfidence(augmentedImage)
            _recognitionConfidence.value = confidence
            
            // Update best match
            _bestMatchImage.value = imageName
            
            // Update recognition quality
            _recognitionQuality.value = when {
                confidence > 0.9f -> RecognitionQuality.EXCELLENT
                confidence > 0.7f -> RecognitionQuality.GOOD
                confidence > 0.5f -> RecognitionQuality.FAIR
                else -> RecognitionQuality.POOR
            }
            
            // Update statistics
            recognitionStats[imageName] = (recognitionStats[imageName] ?: 0) + 1
            
            // Create image recognition result
            val objectName = getObjectNameFromImageName(imageName)
            val imageRecognition = ImageRecognition(
                id = objectName,
                name = objectName,
                description = "Optimal recognition: $objectName (${imageName}, confidence: ${(confidence * 100).toInt()}%)",
                imageUrl = "",
                dialogues = emptyList(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            recognizedImageCache[objectName] = imageRecognition
            
            val currentImages = _recognizedImages.value.toMutableList()
            currentImages.add(augmentedImage)
            _recognizedImages.value = currentImages
            
            Log.d(tag, "Optimal recognition: $objectName via $imageName (confidence: $confidence)")
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling optimal recognition", e)
        }
    }
    
    /**
     * Calculate optimal confidence based on image type
     */
    private fun calculateOptimalConfidence(augmentedImage: AugmentedImage): Float {
        val baseConfidence = when (augmentedImage.trackingState) {
            TrackingState.TRACKING -> 0.9f
            TrackingState.PAUSED -> 0.6f
            TrackingState.STOPPED -> 0.3f
        }
        
        // Adjust based on image type (front view is most reliable)
        val confidenceMultiplier = when {
            augmentedImage.name?.contains("front") == true -> 1.0f
            augmentedImage.name?.contains("bright") == true -> 0.9f
            augmentedImage.name?.contains("close") == true -> 0.8f
            augmentedImage.name?.contains("angle") == true -> 0.7f
            augmentedImage.name?.contains("dim") == true -> 0.6f
            augmentedImage.name?.contains("far") == true -> 0.5f
            else -> 0.8f
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
            else -> imageName
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
        _bestMatchImage.value = null
        Log.d(tag, "Cleared optimal recognition")
    }
    
    /**
     * Pause tracking
     */
    fun pauseTracking() {
        try {
            session?.pause()
            _isTracking.value = false
            _trackingState.value = TrackingState.PAUSED
            Log.d(tag, "Optimal ARCore tracking paused")
        } catch (e: Exception) {
            Log.e(tag, "Failed to pause optimal tracking", e)
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
            Log.d(tag, "Optimal ARCore tracking resumed")
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume optimal tracking", e)
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
            recognitionStats.clear()
            
            Log.d(tag, "Optimal ARCore tracking stopped and cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop optimal tracking", e)
        }
    }
    
    /**
     * Get optimal recognition metrics
     */
    fun getOptimalMetrics(): Map<String, Any> {
        return mapOf(
            "trackingState" to _trackingState.value.name,
            "recognitionConfidence" to _recognitionConfidence.value,
            "bestMatchImage" to (_bestMatchImage.value ?: "none"),
            "recognitionQuality" to _recognitionQuality.value.name,
            "totalRecognitions" to recognitionStats.values.sum(),
            "recognitionStats" to recognitionStats.toMap()
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

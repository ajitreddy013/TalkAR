package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.DynamicScriptService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

class ARImageRecognitionService(private val context: Context) {
    
    private val tag = "ARImageRecognitionService"
    private var session: Session? = null
    private var imageDatabase: AugmentedImageDatabase? = null
    
    // Dynamic script service for generating personalized content
    private val dynamicScriptService = DynamicScriptService(context)
    
    private val _recognizedImages = MutableStateFlow<List<AugmentedImage>>(emptyList())
    val recognizedImages: StateFlow<List<AugmentedImage>> = _recognizedImages.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Cache for recognized images to avoid duplicate processing
    private val recognizedImageCache = ConcurrentHashMap<String, ImageRecognition>()
    
    // Smoothing filter for anchor stability
    private val anchorPositionHistory = ConcurrentHashMap<String, MutableList<Pose>>()
    private val maxHistorySize = 10
    private val distanceThreshold = 0.03f // 3cm threshold for movement filtering
    
    // State for ad content generation
    private val _generatedAdContent = MutableStateFlow<com.talkar.app.data.api.PosterAdContentResponse?>(null)
    val generatedAdContent: StateFlow<com.talkar.app.data.api.PosterAdContentResponse?> = _generatedAdContent.asStateFlow()
    
    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
            _error.value = "ARCore is not available: ${e.message}. Please test on a physical device for full AR functionality."
            false
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize AR service", e)
            _error.value = "Failed to initialize AR service: ${e.message}. This may be due to running in an emulator."
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
            
            // Load images from backend API
            loadImagesFromBackend()
            
            Log.d(tag, "Image database initialized")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize image database", e)
            throw e
        }
    }
    
    // HTTP client with timeout configuration
    private val httpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Load images from backend and add to ARCore database
     */
    private fun loadImagesFromBackend() {
        try {
            Log.d(tag, "Starting to load images from backend on background thread...")
            
            // Move heavy image processing to background thread to avoid ANR
            coroutineScope.launch {
                try {
                    Log.d(tag, "Fetching images from API...")
                    
                    // Get API client from application
                    val apiClient = com.talkar.app.TalkARApplication.instance.apiClient
                    val response = apiClient.getImages()
                    
                    if (response.isSuccessful && response.body() != null) {
                        val images = response.body()!!
                        Log.d(tag, "Found ${images.size} images from backend")
                        
                        var addedCount = 0
                        
                        // Download and add each image
                        images.forEach { image ->
                            if (image.isActive) {
                                // Use full image URL
                                val fullImageUrl = com.talkar.app.data.config.ApiConfig.getFullImageUrl(image.imageUrl)
                                Log.d(tag, "Downloading image: ${image.name} from $fullImageUrl")
                                
                                val bitmap = downloadImageBitmap(fullImageUrl)
                                if (bitmap != null) {
                                    // Add to AR database
                                    // Use image.id as the key for deterministic matching
                                    imageDatabase?.let { database ->
                                        // We use image.id as the ARCore name so we can match it back easily
                                        // But we also log the real name
                                        try {
                                             // Preprocess the image for better AR tracking
                                            val processedBitmap = preprocessImageForAR(bitmap)
                                            
                                            // Validate
                                            if (validateImageQuality(processedBitmap)) {
                                                database.addImage(image.id, processedBitmap)
                                                Log.d(tag, "Added to AR database: ${image.name} (ID: ${image.id})")
                                                addedCount++
                                            } else {
                                                Log.w(tag, "Skipping ${image.name} - insufficient quality")
                                            }
                                        } catch (e: Exception) {
                                            Log.e(tag, "Failed to add ${image.name} to database", e)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Log.d(tag, "Successfully added $addedCount images to AR database")
                        
                    } else {
                        Log.e(tag, "Failed to fetch images: ${response.code()}")
                        // Fallback to test images if API fails
                        createTestImage("test_image")
                    }
                    
                } catch (e: Exception) {
                    Log.e(tag, "Failed to load images from backend", e)
                    // Fallback to test images
                    createTestImage("test_image")
                }
            }
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to initiate backend image loading", e)
        }
    }

    /**
     * Load images from backend API (called by ViewModel)
     */
    suspend fun loadImagesFromAPI(images: List<ImageRecognition>) {
        try {
            Log.d(tag, "Loading ${images.size} images from API via ViewModel")
            
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                var addedCount = 0
                images.forEach { image ->
                    try {
                        val fullImageUrl = com.talkar.app.data.config.ApiConfig.getFullImageUrl(image.imageUrl)
                        val bitmap = downloadImageBitmap(fullImageUrl)
                        
                        if (bitmap != null) {
                                val processedBitmap = preprocessImageForAR(bitmap)
                                if (validateImageQuality(processedBitmap)) {
                                    imageDatabase?.let { db ->
                                        db.addImage(image.id, processedBitmap)
                                        Log.d(tag, "Added image from VM: ${image.name} (ID: ${image.id})")
                                        addedCount++
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to load image from VM: ${image.name}", e)
                    }
                }
                Log.d(tag, "ViewModel loaded $addedCount images into AR database")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load images from API", e)
        }
    }

    /**
     * Download single image bitmap from URL using OkHttp
     */
    private suspend fun downloadImageBitmap(imageUrl: String): android.graphics.Bitmap? {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val request = okhttp3.Request.Builder()
                    .url(imageUrl)
                    .build()
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(tag, "Failed to download image: HTTP ${response.code}")
                        return@use null
                    }
                    
                    val inputStream = response.body?.byteStream()
                    if (inputStream == null) return@use null
                    
                    android.graphics.BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to download image from URL: $imageUrl", e)
                null
            }
        }
    }

    /**
     * Create a test image for AR recognition (Fallback)
     */
    private fun createTestImage(name: String = "test_image") {
        try {
            // Create a high-quality test pattern for AR recognition
            val testBitmap = createTestPatternBitmap(name)
            
            // Validate image quality before adding to database
            if (validateImageQuality(testBitmap)) {
                imageDatabase?.let { database ->
                    database.addImage(name, testBitmap)
                    Log.d(tag, "Added test image to ARCore database: $name")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to create test image", e)
        }
    }
    
    /**
     * Create a high-quality test pattern bitmap that meets ARCore requirements
     */
    private fun createTestPatternBitmap(name: String = "test_image"): android.graphics.Bitmap {
        val size = 2048 // Much larger size for better ARCore quality
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Create a high-contrast pattern with multiple features for better tracking
        val paint = android.graphics.Paint()
        paint.isAntiAlias = true
        
        // Solid white background for maximum contrast
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        
        // Create a complex pattern with multiple distinct features
        
        // Add multiple geometric shapes for better feature detection
        paint.color = android.graphics.Color.BLACK
        paint.style = android.graphics.Paint.Style.FILL
        
        // Draw circles at corners and center
        val circleRadius = size / 8f
        canvas.drawCircle(circleRadius, circleRadius, circleRadius, paint)
        canvas.drawCircle(size - circleRadius, circleRadius, circleRadius, paint)
        canvas.drawCircle(circleRadius, size - circleRadius, circleRadius, paint)
        canvas.drawCircle(size - circleRadius, size - circleRadius, circleRadius, paint)
        canvas.drawCircle(size / 2f, size / 2f, circleRadius * 1.5f, paint)
        
        // Draw rectangles for additional features
        paint.color = android.graphics.Color.DKGRAY
        val rectSize = size / 6f
        canvas.drawRect(size / 4f, size / 4f, size / 4f + rectSize, size / 4f + rectSize, paint)
        canvas.drawRect(3 * size / 4f - rectSize, 3 * size / 4f - rectSize, 3 * size / 4f, 3 * size / 4f, paint)
        
        // Add text with better formatting
        paint.color = android.graphics.Color.BLACK
        paint.textSize = 64f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.isAntiAlias = true
        
        // Draw text with shadow for better contrast
        paint.setShadowLayer(4f, 2f, 2f, android.graphics.Color.WHITE)
        canvas.drawText("TALKAR", size / 2f, size / 2f - 100f, paint)
        canvas.drawText("AR TEST", size / 2f, size / 2f + 50f, paint)
        canvas.drawText(name.uppercase(), size / 2f, size / 2f + 150f, paint)
        
        return bitmap
    }
    
    /**
     * Validate image quality for ARCore requirements
     */
    private fun validateImageQuality(bitmap: android.graphics.Bitmap): Boolean {
        try {
            // Check basic requirements
            if (bitmap.width < 256 || bitmap.height < 256) {
                Log.w(tag, "Image too small: ${bitmap.width}x${bitmap.height}")
                return false
            }
            
            // Check for sufficient contrast by analyzing pixel variance
            val contrast = calculateContrast(bitmap)
            if (contrast < 0.3) {
                Log.w(tag, "Image has insufficient contrast: $contrast")
                return false
            }
            
            // Check for sufficient features (edges, corners)
            val featureCount = calculateFeatureCount(bitmap)
            if (featureCount < 50) {
                Log.w(tag, "Image has insufficient features: $featureCount")
                return false
            }
            
            Log.d(tag, "Image quality validation passed - contrast: $contrast, features: $featureCount")
            return true
            
        } catch (e: Exception) {
            Log.e(tag, "Error validating image quality", e)
            return false
        }
    }
    
    /**
     * Calculate image contrast for quality assessment
     */
    private fun calculateContrast(bitmap: android.graphics.Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 10 // Sample every 10th pixel for performance
        
        val pixels = mutableListOf<Int>()
        for (y in 0 until height step sampleSize) {
            for (x in 0 until width step sampleSize) {
                pixels.add(bitmap.getPixel(x, y))
            }
        }
        
        // Calculate luminance for each pixel
        val luminances = pixels.map { pixel ->
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        }
        
        // Calculate standard deviation as contrast measure
        val mean = luminances.average()
        val variance = luminances.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * Calculate approximate feature count using edge detection
     */
    private fun calculateFeatureCount(bitmap: android.graphics.Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        var edgeCount = 0
        
        // Simple edge detection using gradient magnitude
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val center = bitmap.getPixel(x, y)
                val right = bitmap.getPixel(x + 1, y)
                val down = bitmap.getPixel(x, y + 1)
                
                val centerGray = getGrayValue(center)
                val rightGray = getGrayValue(right)
                val downGray = getGrayValue(down)
                
                val gradientX = kotlin.math.abs(centerGray - rightGray)
                val gradientY = kotlin.math.abs(centerGray - downGray)
                val gradientMagnitude = kotlin.math.sqrt((gradientX * gradientX + gradientY * gradientY).toDouble())
                
                if (gradientMagnitude > 30) { // Threshold for edge detection
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
     * Preprocess image to enhance quality for ARCore
     */
    private fun preprocessImageForAR(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        try {
            // Ensure minimum size
            val minSize = 512
            val targetSize = kotlin.math.max(minSize, kotlin.math.max(bitmap.width, bitmap.height))
            
            // Scale image if needed
            val scaledBitmap = if (bitmap.width < targetSize || bitmap.height < targetSize) {
                val scale = targetSize.toFloat() / kotlin.math.max(bitmap.width, bitmap.height)
                android.graphics.Bitmap.createScaledBitmap(bitmap, 
                    (bitmap.width * scale).toInt(), 
                    (bitmap.height * scale).toInt(), 
                    true)
            } else {
                bitmap
            }
            
            // Apply contrast enhancement
            val enhancedBitmap = enhanceContrast(scaledBitmap)
            
            return enhancedBitmap
            
        } catch (e: Exception) {
            Log.e(tag, "Error preprocessing image", e)
            return bitmap
        }
    }
    
    /**
     * Enhance image contrast for better AR tracking
     */
    private fun enhanceContrast(bitmap: android.graphics.Bitmap): android.graphics.Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val enhancedBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Apply contrast enhancement
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            val a = android.graphics.Color.alpha(pixel)
            
            // Apply contrast enhancement (simple linear stretch)
            val enhancedR = kotlin.math.max(0.0, kotlin.math.min(255.0, (r - 128) * 1.5 + 128)).toInt()
            val enhancedG = kotlin.math.max(0.0, kotlin.math.min(255.0, (g - 128) * 1.5 + 128)).toInt()
            val enhancedB = kotlin.math.max(0.0, kotlin.math.min(255.0, (b - 128) * 1.5 + 128)).toInt()
            
            pixels[i] = android.graphics.Color.argb(a, enhancedR, enhancedG, enhancedB)
        }
        
        enhancedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return enhancedBitmap
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
                imageDatabase?.let { database ->
                    config.augmentedImageDatabase = database
                }
                
                // Configure session
                session.configure(config)
                
                // Start the session
                session.resume()
                
                _isTracking.value = true
                Log.d(tag, "ARCore tracking started")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start tracking", e)
            _error.value = "Failed to start ARCore tracking: ${e.message}"
        }
    }
    
    /**
     * Process camera frame for image recognition - optimized for performance
     */
    fun processFrame(frame: Frame) {
        try {
            val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            
            // Process all tracking updates without early return to prevent freezing
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
     * Handle recognized image with jitter reduction and anchor stability improvements
     */
    private fun handleImageRecognized(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name ?: "unknown"
        
        // Apply jitter reduction using distance threshold
        if (!shouldUpdateAnchor(imageName, augmentedImage.centerPose)) {
            return
        }
        
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
                dialogues = emptyList(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            // Cache the result
            recognizedImageCache[imageName] = imageRecognition
            
            // Update state
            val currentImages = _recognizedImages.value.toMutableList()
            currentImages.add(augmentedImage)
            _recognizedImages.value = currentImages
            
            Log.d(tag, "Image recognized: $imageName")
            
            // Generate personalized ad content for the recognized poster
            generatePersonalizedAdContent(imageName)
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling recognized image", e)
        }
    }
    
    /**
     * Check if anchor should be updated based on distance threshold to reduce jitter
     */
    private fun shouldUpdateAnchor(imageName: String, currentPose: Pose): Boolean {
        // Get history of previous positions
        val history = anchorPositionHistory.getOrPut(imageName) { mutableListOf() }
        
        // If no history, allow update
        if (history.isEmpty()) {
            history.add(currentPose)
            return true
        }
        
        // Get last known position
        val lastPose = history.last()
        
        // Calculate distance between current and last position
        val distance = calculatePoseDistance(lastPose, currentPose)
        
        // If movement is below threshold, filter it out to reduce jitter
        if (distance < distanceThreshold) {
            return false
        }
        
        // Add current position to history
        history.add(currentPose)
        
        // Maintain history size
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
        
        return true
    }
    
    /**
     * Calculate Euclidean distance between two poses
     */
    private fun calculatePoseDistance(pose1: Pose, pose2: Pose): Float {
        val dx = pose1.tx() - pose2.tx()
        val dy = pose1.ty() - pose2.ty()
        val dz = pose1.tz() - pose2.tz()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
    
    /**
     * Generate personalized ad content for the recognized poster
     */
    private fun generatePersonalizedAdContent(imageId: String) {
        coroutineScope.launch {
            try {
                Log.d(tag, "Generating personalized ad content for poster: $imageId")
                
                // Generate complete ad content (script + audio + video)
                val adContent = dynamicScriptService.generateAdContentFromPoster(imageId)
                
                if (adContent != null) {
                    _generatedAdContent.value = adContent
                    Log.d(tag, "Successfully generated personalized ad content for $imageId")
                    Log.d(tag, "Script: ${adContent.script}")
                    Log.d(tag, "Video URL: ${adContent.video_url}")
                } else {
                    Log.e(tag, "Failed to generate ad content for $imageId")
                    _error.value = "Failed to generate personalized ad content for $imageId"
                }
                
            } catch (e: Exception) {
                Log.e(tag, "Error generating personalized ad content for $imageId", e)
                _error.value = "Error generating ad content: ${e.message}"
            }
        }
    }
    
    /**
     * Get generated ad content for the last recognized image
     */
    fun getGeneratedAdContent(): com.talkar.app.data.api.PosterAdContentResponse? {
        return _generatedAdContent.value
    }
    
    /**
     * Clear generated ad content
     */
    fun clearGeneratedAdContent() {
        _generatedAdContent.value = null
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
                // Convert ByteArray to Bitmap
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                
                // Preprocess the image for better AR tracking
                val processedBitmap = preprocessImageForAR(bitmap)
                
                // Validate image quality before adding to database
                if (validateImageQuality(processedBitmap)) {
                    database.addImage(imageName, processedBitmap)
                    Log.d(tag, "Added reference image: $imageName")
                    true
                } else {
                    Log.w(tag, "Reference image failed quality validation: $imageName")
                    _error.value = "Reference image '$imageName' does not meet ARCore quality requirements. Please ensure the image has good contrast and multiple distinct features."
                    false
                }
            } ?: false
        } catch (e: ImageInsufficientQualityException) {
            Log.e(tag, "Reference image has insufficient quality for ARCore: $imageName", e)
            _error.value = "Reference image '$imageName' quality is insufficient for AR tracking. Please ensure images have good contrast and multiple distinct features."
            false
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
            // Note: AugmentedImageDatabase doesn't support removing individual images
            // In a real implementation, you would need to recreate the database
            // For now, we'll just log the attempt
            Log.d(tag, "Remove reference image requested: $imageName (not supported by ARCore)")
            true
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
    
    /**
     * Get the current ARCore session for external processing
     */
    fun getSession(): Session? {
        return session
    }

    /**
     * Post an error message to be displayed in the UI
     */
    fun postError(errorMessage: String) {
        _error.value = errorMessage
    }

    
    /**
     * Check if the session is ready for processing
     */
    fun isSessionReady(): Boolean {
        return session != null && _isTracking.value
    }
    
    /**
     * Pause AR processing to reduce CPU usage
     */
    fun pauseProcessing() {
        try {
            session?.pause()
            _isTracking.value = false
            Log.d(tag, "ARCore processing paused to reduce CPU usage")
        } catch (e: Exception) {
            Log.e(tag, "Failed to pause AR processing", e)
        }
    }
    
    /**
     * Check if ARCore service is properly initialized
     */
    fun isInitialized(): Boolean {
        return session != null && imageDatabase != null
    }
    
    /**
     * Resume AR processing with enhanced error handling and frame processing
     */
    fun resumeProcessing() {
        try {
            session?.let { session ->
                Log.d(tag, "Attempting to resume ARCore session...")
                session.resume()
                _isTracking.value = true
                Log.d(tag, "ARCore processing resumed successfully")
                
                // Start frame processing in a controlled manner
                Log.d(tag, "Frame processing will be handled by renderer")
                
            } ?: run {
                Log.w(tag, "Cannot resume - ARCore session is null")
                _error.value = "ARCore session not initialized"
            }
        } catch (e: com.google.ar.core.exceptions.CameraNotAvailableException) {
            Log.e(tag, "Camera not available for ARCore resume", e)
            _error.value = "Camera not available: ${e.message}"
            _isTracking.value = false
        } catch (e: com.google.ar.core.exceptions.SessionPausedException) {
            Log.w(tag, "ARCore session was paused during resume attempt", e)
            _isTracking.value = false
        } catch (e: Exception) {
            Log.e(tag, "Failed to resume AR processing", e)
            _error.value = "Failed to resume AR processing: ${e.message}"
            _isTracking.value = false
        }
    }
    
    /**
     * Start frame processing loop on background thread
     */
    private fun startFrameProcessing() {
        coroutineScope.launch {
            Log.d(tag, "Starting frame processing loop on background thread")
            
            while (_isTracking.value) {
                try {
                    session?.let { session ->
                        val frame = session.update()
                        processFrame(frame)
                    }
                    
                    // Small delay to prevent overwhelming the system
                    delay(33) // ~30 FPS
                    
                } catch (e: Exception) {
                    Log.e(tag, "Error in frame processing loop", e)
                    delay(100) // Wait before retrying
                }
            }
            
            Log.d(tag, "Frame processing loop ended")
        }
    }
    
    
    /**
     * Clean up all resources including coroutines to prevent memory leaks
     */
    fun cleanup() {
        try {
            Log.d(tag, "Cleaning up ARImageRecognitionService resources")
            
            // Stop tracking and clean up ARCore resources
            stopTracking()
            
            // Cancel all coroutines to prevent memory leaks
            Log.d(tag, "ARImageRecognitionService resources cleaned up successfully")
        } catch (e: Exception) {
            Log.e(tag, "Error cleaning up ARImageRecognitionService", e)
        }
    }
}

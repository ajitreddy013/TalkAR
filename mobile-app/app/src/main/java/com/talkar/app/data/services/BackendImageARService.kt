package com.talkar.app.data.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.api.ApiClient
import com.talkar.app.TalkARApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.pow
import java.net.URL
import java.io.InputStream
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Backend Image AR Service - Downloads multiple images from backend for AR recognition
 * 
 * Workflow:
 * 1. Admin uploads 5-7 images to backend via admin dashboard
 * 2. Mobile app downloads images from backend API
 * 3. Images are added to ARCore database for recognition
 * 4. App uses multiple images for reliable recognition
 */
class BackendImageARService(private val context: Context) {
    
    private val tag = "BackendImageARService"
    private var session: Session? = null
    private var imageDatabase: AugmentedImageDatabase? = null
    
    // HTTP client with timeout configuration
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Backend API client - lazily retrieved from the Application instance to avoid
    // creating it during service field initialization (which can happen before
    // the application context is fully ready). Falls back to ApiClient.create()
    // if the Application instance is not available for some reason.
    private val apiClient by lazy {
        try {
            TalkARApplication.instance.apiClient
        } catch (e: Exception) {
            Log.w(tag, "TalkARApplication.instance not available - falling back to ApiClient.create()", e)
            ApiClient.create()
        }
    }
    
    // Tracking state
    private val _trackingState = MutableStateFlow(TrackingState.STOPPED)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Backend image management
    private val _downloadedImages = MutableStateFlow<List<BackendImage>>(emptyList())
    val downloadedImages: StateFlow<List<BackendImage>> = _downloadedImages.asStateFlow()
    
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()
    
    // Recognition state
    private val _recognizedImages = MutableStateFlow<List<AugmentedImage>>(emptyList())
    val recognizedImages: StateFlow<List<AugmentedImage>> = _recognizedImages.asStateFlow()
    
    private val _recognitionConfidence = MutableStateFlow(0f)
    val recognitionConfidence: StateFlow<Float> = _recognitionConfidence.asStateFlow()
    
    // Cache
    private val recognizedImageCache = ConcurrentHashMap<String, ImageRecognition>()
    private val imageBitmaps = ConcurrentHashMap<String, Bitmap>()

    // Coroutine scope for background operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Check if device has internet connectivity
     */
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e(tag, "Error checking network availability", e)
            false
        }
    }
    
    /**
     * Initialize service and download images from backend
     */
    suspend fun initialize(): Boolean {
        return try {
            // First check if we have network connectivity
            if (!isNetworkAvailable()) {
                Log.d(tag, "No network connectivity available, skipping backend initialization")
                _error.value = "No internet connection available"
                return false
            }
            
            if (!isARCoreSupported()) {
                _error.value = "ARCore is not supported on this device"
                return false
            }
            
            // Create ARCore session - this will handle camera initialization
            session = Session(context)
            configureSession()
            
            // Download images from backend
            val success = downloadImagesFromBackend()
            if (!success) {
                _error.value = "Failed to download images from backend"
                return false
            }
            
            // Initialize ARCore database with downloaded images
            initializeImageDatabase()
            
            // Start tracking
            startTracking()
            
            Log.d(tag, "Backend Image AR Service initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize backend image AR service", e)
            _error.value = "Failed to initialize AR service: ${e.message}"
            false
        }
    }
    
    /**
     * Download images from backend API
     */
    private suspend fun downloadImagesFromBackend(): Boolean {
        return try {
            Log.d(tag, "Downloading images from backend...")
            
            // Get images from backend API
            val response = withContext(Dispatchers.IO) {
                apiClient.getImages()
            }
            
            if (!response.isSuccessful || response.body() == null) {
                Log.w(tag, "Failed to get images from backend: ${response.code()}")
                return false
            }
            
            val images = response.body()!!
            if (images.isEmpty()) {
                Log.w(tag, "No images available from backend")
                return false
            }
            
            Log.d(tag, "Found ${images.size} images from backend")
            
            // Download image bitmaps
            val downloadedImages = downloadImageBitmaps(images)
            _downloadedImages.value = downloadedImages
            
            Log.d(tag, "Successfully downloaded ${downloadedImages.size} images")
            true
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to download images from backend", e)
            _error.value = "Failed to download images: ${e.message}"
            false
        }
    }
    
    /**
     * Download image bitmaps from URLs
     */
    private suspend fun downloadImageBitmaps(images: List<BackendImage>): List<BackendImage> {
        val downloadedImages = mutableListOf<BackendImage>()
        var progress = 0f
        
        for (image in images) {
            try {
                Log.d(tag, "Downloading image: ${image.name}")
                
                val bitmap = downloadImageBitmap(image.imageUrl)
                if (bitmap != null) {
                    // Store bitmap in cache
                    imageBitmaps[image.id] = bitmap
                    downloadedImages.add(image)
                    Log.d(tag, "Successfully downloaded: ${image.name}")
                } else {
                    Log.w(tag, "Failed to download: ${image.name}")
                }
                
                // Update progress
                progress = (downloadedImages.size.toFloat() / images.size) * 100f
                _downloadProgress.value = progress
                
            } catch (e: Exception) {
                Log.e(tag, "Error downloading image: ${image.name}", e)
            }
        }
        
        return downloadedImages
    }
    
    /**
     * Download single image bitmap from URL using OkHttp
     */
    private suspend fun downloadImageBitmap(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(tag, "Downloading image from URL: $imageUrl")
                
                val request = Request.Builder()
                    .url(imageUrl)
                    .build()
                
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(tag, "Failed to download image: HTTP ${response.code}")
                        return@withContext null
                    }
                    
                    val inputStream = response.body?.byteStream()
                    if (inputStream == null) {
                        Log.e(tag, "Failed to get input stream from response")
                        return@withContext null
                    }
                    
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    if (bitmap == null) {
                        Log.e(tag, "Failed to decode bitmap from input stream")
                        return@withContext null
                    }
                    
                    Log.d(tag, "Successfully decoded bitmap: ${bitmap.width}x${bitmap.height}")
                    bitmap
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to download image from URL: $imageUrl", e)
                null
            }
        }
    }
    
    /**
     * Initialize ARCore database with downloaded images
     */
    private fun initializeImageDatabase() {
        try {
            imageDatabase = AugmentedImageDatabase(session)
            
            val images = _downloadedImages.value
            var totalAdded = 0
            
            Log.d(tag, "Adding ${images.size} images to ARCore database")
            
            for (image in images) {
                val bitmap = imageBitmaps[image.id]
                if (bitmap != null) {
                    try {
                        // Validate image quality
                        if (validateImageQuality(bitmap)) {
                            imageDatabase?.addImage(image.name, bitmap)
                            totalAdded++
                            Log.d(tag, "Added to ARCore database: ${image.name}")
                        } else {
                            Log.w(tag, "Image failed quality validation: ${image.name}")
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to add image to ARCore database: ${image.name}", e)
                    }
                } else {
                    Log.w(tag, "No bitmap found for image: ${image.name}")
                }
            }
            
            Log.d(tag, "Successfully added $totalAdded images to ARCore database")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize image database", e)
            throw e
        }
    }
    
    /**
     * Validate image quality for ARCore
     */
    private fun validateImageQuality(bitmap: Bitmap): Boolean {
        try {
            // Check minimum size
            if (bitmap.width < 512 || bitmap.height < 512) {
                Log.w(tag, "Image too small: ${bitmap.width}x${bitmap.height}")
                return false
            }
            
            // Check contrast
            val contrast = calculateContrast(bitmap)
            if (contrast < 0.3) {
                Log.w(tag, "Image has insufficient contrast: $contrast")
                return false
            }
            
            // Check features
            val featureCount = calculateFeatureCount(bitmap)
            if (featureCount < 80) {
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
     * Calculate image contrast
     */
    private fun calculateContrast(bitmap: Bitmap): Double {
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
    private fun calculateFeatureCount(bitmap: Bitmap): Int {
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
    private fun calculateSobelGx(bitmap: Bitmap, x: Int, y: Int): Int {
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
    private fun calculateSobelGy(bitmap: Bitmap, x: Int, y: Int): Int {
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
     * Configure ARCore session
     */
    private fun configureSession() {
        session?.let { session ->
            val config = Config(session)
            config.focusMode = Config.FocusMode.AUTO
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            config.depthMode = Config.DepthMode.AUTOMATIC
            config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
            config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
            
            // Set image database
            imageDatabase?.let { database ->
                config.augmentedImageDatabase = database
            }
            
            session.configure(config)
            Log.d(tag, "ARCore session configured for backend images")
        }
    }
    
    /**
     * Start tracking
     */
    private fun startTracking() {
        try {
            session?.let { session ->
                session.resume()
                _isTracking.value = true
                _trackingState.value = TrackingState.TRACKING
                
                startFrameProcessing()
                Log.d(tag, "Backend image ARCore tracking started")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start tracking", e)
            _error.value = "Failed to start ARCore tracking: ${e.message}"
        }
    }
    
    /**
     * Start frame processing
     */
    private fun startFrameProcessing() {
        coroutineScope.launch {
            while (_isTracking.value) {
                try {
                    session?.let { session ->
                        val frame = session.update()
                        processFrame(frame)
                    }
                    
                    delay(33) // ~30 FPS
                } catch (e: Exception) {
                    Log.e(tag, "Error in frame processing", e)
                    delay(100)
                }
            }
        }
    }
    
    /**
     * Process frame for recognition
     */
    private fun processFrame(frame: Frame) {
        try {
            val camera = frame.camera
            val trackingState = camera.trackingState
            
            when (trackingState) {
                TrackingState.TRACKING -> {
                    processRecognition(frame)
                }
                TrackingState.PAUSED -> {
                    Log.w(tag, "Tracking paused")
                }
                TrackingState.STOPPED -> {
                    Log.w(tag, "Tracking stopped")
                }
            }
            
            _trackingState.value = trackingState
            
        } catch (e: Exception) {
            Log.e(tag, "Error processing frame", e)
        }
    }
    
    /**
     * Process recognition
     */
    private fun processRecognition(frame: Frame) {
        try {
            val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            
            for (augmentedImage in augmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.TRACKING -> {
                        handleRecognition(augmentedImage)
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
            Log.e(tag, "Error processing recognition", e)
        }
    }
    
    /**
     * Handle recognized image
     */
    private fun handleRecognition(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name ?: "unknown"
        
        if (recognizedImageCache.containsKey(imageName)) {
            return
        }
        
        try {
            // Calculate confidence based on image type
            val confidence = calculateConfidence(augmentedImage)
            _recognitionConfidence.value = confidence
            
            // Get object name from image name
            val objectName = getObjectNameFromImageName(imageName)
            
            // Create image recognition result
            val imageRecognition = ImageRecognition(
                id = objectName,
                name = objectName,
                description = "Backend image recognized: $objectName (${imageName}, confidence: ${(confidence * 100).toInt()}%)",
                imageUrl = "",
                dialogues = emptyList(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            recognizedImageCache[objectName] = imageRecognition
            
            val currentImages = _recognizedImages.value.toMutableList()
            currentImages.add(augmentedImage)
            _recognizedImages.value = currentImages
            
            Log.d(tag, "Backend image recognized: $objectName via $imageName (confidence: $confidence)")
            
        } catch (e: Exception) {
            Log.e(tag, "Error handling recognition", e)
        }
    }
    
    /**
     * Calculate recognition confidence
     */
    private fun calculateConfidence(augmentedImage: AugmentedImage): Float {
        val baseConfidence = when (augmentedImage.trackingState) {
            TrackingState.TRACKING -> 0.9f
            TrackingState.PAUSED -> 0.6f
            TrackingState.STOPPED -> 0.3f
        }
        
        // Adjust based on image type
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
        // Extract object name from image name (e.g., "TalkAR Logo Front" -> "TalkAR Logo")
        return imageName.split(" ").dropLast(1).joinToString(" ")
    }
    
    /**
     * Get the current ARCore session for external processing
     */
    fun getSession(): Session? {
        return session
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
        Log.d(tag, "Cleared backend image recognition")
    }
    
    /**
     * Pause tracking
     */
    fun pauseTracking() {
        try {
            session?.pause()
            _isTracking.value = false
            _trackingState.value = TrackingState.PAUSED
            Log.d(tag, "Backend image ARCore tracking paused")
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
            Log.d(tag, "Backend image ARCore tracking resumed")
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
            _trackingState.value = TrackingState.STOPPED
            _recognizedImages.value = emptyList()
            recognizedImageCache.clear()
            
            Log.d(tag, "Backend image ARCore tracking stopped and cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Failed to stop tracking", e)
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
     * Get backend image metrics
     */
    fun getBackendMetrics(): Map<String, Any> {
        return mapOf(
            "trackingState" to _trackingState.value.name,
            "recognitionConfidence" to _recognitionConfidence.value,
            "downloadedImageCount" to _downloadedImages.value.size,
            "downloadProgress" to _downloadProgress.value,
            "recognizedImagesCount" to _recognizedImages.value.size
        )
    }
}
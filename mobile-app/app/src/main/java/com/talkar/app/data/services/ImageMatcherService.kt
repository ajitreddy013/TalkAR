package com.talkar.app.data.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.talkar.app.data.models.BackendImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.net.URL
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancel


/**
 * Service for matching camera frames against backend reference images
 * Uses simple template matching without heavy ML libraries
 */
class ImageMatcherService(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageMatcherService"
        private const val MATCH_THRESHOLD = 0.50 // Threshold with dual verification
        private const val DEBOUNCE_MS = 1000L // Fast frame analysis check
        private const val COOLDOWN_MS = 8000L // 8s cooldown for same product
        private const val MAX_DIMENSION = 512 // Downscale images for faster comparison
    }
    
    data class ImageTemplate(
        val id: String,
        val name: String,
        val description: String?,
        val bitmap: Bitmap,
        val dialogues: List<Any>?
    )
    
    private val activeReaders = java.util.concurrent.atomic.AtomicInteger(0)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var preloadJob: kotlinx.coroutines.Job? = null

    init {
        // 🔥 Pre-load fallback templates immediately
        preloadJob = serviceScope.launch {
            loadTemplatesFromAssets()
            Log.d(TAG, "Fallback templates pre-loaded")
        }
    }
    
    data class MatchResult(
        val imageId: String,
        val imageName: String,
        val description: String?,
        val confidence: Float,
        val dialogues: List<Any>?
    )
    
    private val templates = mutableListOf<ImageTemplate>()
    private var toRecycle = mutableListOf<ImageTemplate>()
    private val mutex = Mutex()
    
    @Volatile
    private var lastDetectionTime: Long = 0
    
    @Volatile
    private var lastDetectedImageId: String? = null
    
    @Volatile
    var isUsingFallback: Boolean = false
        private set
    
    /**
     * Load reference images from backend API with fallback to local assets
     */
    suspend fun loadTemplatesFromBackend(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // If we already have local templates, we're good to go immediately
            // But still try to fetch fresh ones from backend
            Log.d(TAG, "Attempting to load reference images from backend...")
            val apiService = com.talkar.app.data.api.ApiClient.create()
            
            val response = try {
                apiService.getImages()
            } catch (e: Exception) {
                Log.w(TAG, "Backend network error, failing over to local templates: ${e.message}")
                return@withContext loadTemplatesFromAssets()
            }
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Backend returned error ${response.code()}, failing over to local templates")
                return@withContext loadTemplatesFromAssets()
            }
            
            val backendImages = response.body() ?: emptyList()
            if (backendImages.isEmpty()) {
                Log.w(TAG, "Backend returned empty image list, falling back to local assets")
                return@withContext loadTemplatesFromAssets()
            }

            Log.d(TAG, "Fetched ${backendImages.size} images from backend")
            
            mutex.withLock {
                // Do NOT recycle immediately. Copy to local list then clear main list.
                // This allows us to recycle safely outside the lock after readers finish.
                toRecycle = templates.toMutableList()
                templates.clear()
            }
            
            // Wait for readers then recycle
            try {
                while (activeReaders.get() > 0) {
                    kotlinx.coroutines.delay(10)
                }
                toRecycle.forEach { it.bitmap.recycle() }
            } catch (e: Exception) {
                Log.e(TAG, "Error recycling bitmaps", e)
            }
            
            // Download and prepare each image
            backendImages.forEach { backendImage ->
                try {
                    val bitmap = downloadAndPrepareBitmap(backendImage.imageUrl)
                    if (bitmap != null) {
                        mutex.withLock {
                            templates.add(
                                ImageTemplate(
                                    id = backendImage.id,
                                    name = backendImage.name,
                                    description = backendImage.description,
                                    bitmap = bitmap,
                                    dialogues = backendImage.dialogues as? List<Any>
                                )
                            )
                        }
                        Log.d(TAG, "Loaded backend template: ${backendImage.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading backend image ${backendImage.name}", e)
                }
            }
            
            if (getTemplateCount() == 0) {
                Log.w(TAG, "Failed to load any backend images. Trying asset fallback as last resort...")
                return@withContext loadTemplatesFromAssets()
            }

            Log.d(TAG, "Successfully loaded ${templates.size} images from backend")
            isUsingFallback = false
            Result.success(templates.size)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during backend load", e)
            Result.failure(e)
        }
    }

    /**
     * Load reference images from local assets (Offline Fallback)
     */
    suspend fun loadTemplatesFromAssets(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Loading fallback templates from local assets...")
            val jsonString = context.assets.open("fallbacks.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<BackendImage>>() {}.type
            val fallbackImages: List<BackendImage> = Gson().fromJson(jsonString, listType)
            
            mutex.withLock {
                 // Do NOT recycle immediately. Copy to local list then clear main list.
                toRecycle = templates.toMutableList()
                templates.clear()
            }
            
            // Wait for readers then recycle
            try {
                while (activeReaders.get() > 0) {
                    kotlinx.coroutines.delay(10)
                }
                toRecycle.forEach { it.bitmap.recycle() }
            } catch (e: Exception) {
                Log.e(TAG, "Error recycling bitmaps", e)
            }
            
            fallbackImages.forEach { item ->
                try {
                    val bitmap = context.assets.open(item.imageUrl).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                    
                    if (bitmap != null) {
                        val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
                        bitmap.recycle()
                        
                        mutex.withLock {
                            templates.add(
                                ImageTemplate(
                                    id = item.id,
                                    name = item.name,
                                    description = item.description,
                                    bitmap = scaled,
                                    dialogues = item.dialogues as? List<Any>
                                )
                            )
                        }
                        Log.d(TAG, "Loaded fallback template: ${item.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading asset fallback ${item.imageUrl}", e)
                }
            }
            
            Log.i(TAG, "Successfully loaded ${templates.size} local fallback templates")
            isUsingFallback = true
            Result.success(templates.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load any templates (backend or asset)", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download image from URL and prepare for matching
     */
    private suspend fun downloadAndPrepareBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Construct full URL using ApiConfig helper
            val url = com.talkar.app.data.config.ApiConfig.getFullImageUrl(imageUrl)
            
            Log.d(TAG, "Downloading image from: $url")
            val inputStream = URL(url).openStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap != null) {
                // Downscale for faster matching
                val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
                bitmap.recycle()
                scaled
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image from $imageUrl", e)
            null
        }
    }
    
    /**
     * Match camera frame against all templates
     */
    suspend fun matchFrame(frameBitmap: Bitmap): MatchResult? = withContext(Dispatchers.Default) {
        if (templates.isEmpty()) {
            return@withContext null
        }
        
        // Debounce - avoid spam detections
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDetectionTime < DEBOUNCE_MS) {
            return@withContext null
        }
        
        try {
            activeReaders.incrementAndGet()
            
            // Prepare frame for matching
            val scaledFrame = scaleBitmap(frameBitmap, MAX_DIMENSION)
            
            var bestMatch: MatchResult? = null
            var bestScore = 0f
            
            // Compare against all templates
            val currentTemplates = mutex.withLock { templates.toList() }
            
            currentTemplates.forEach { template ->
                val similarity = calculateSimilarity(scaledFrame, template.bitmap)
                Log.v(TAG, "Comparing frame against ${template.name}: score=${String.format("%.4f", similarity)}")
                
                if (similarity > bestScore && similarity >= MATCH_THRESHOLD) {
                    bestScore = similarity
                    bestMatch = MatchResult(
                        imageId = template.id,
                        imageName = template.name,
                        description = template.description,
                        confidence = similarity,
                        dialogues = template.dialogues
                    )
                }
            }
            
            scaledFrame.recycle()
            
            if (bestMatch != null) {
                // Hotfix: Strict Cooldown for the same product to prevent infinite play loops
                if (bestMatch!!.imageId == lastDetectedImageId && 
                    currentTime - lastDetectionTime < COOLDOWN_MS) {
                    Log.v(TAG, "Skipping re-detection of ${bestMatch!!.imageName} (Cooldown active)")
                    return@withContext null
                }

                // Only return if it's a different image or enough time has passed
                lastDetectionTime = currentTime
                lastDetectedImageId = bestMatch!!.imageId
                Log.d(TAG, "MATCH CONFIRMED: ${bestMatch!!.imageName} (ID: ${bestMatch!!.imageId}) confidence=${String.format("%.2f", bestMatch!!.confidence)}")
                return@withContext bestMatch
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error matching frame", e)
            null
        } finally {
            activeReaders.decrementAndGet()
        }
    }
    
    /**
     * Generates a difference hash (dHash) for a given bitmap.
     * dHash is much more robust to lighting changes as it compares adjacent pixel intensities.
     */
    private fun generateDifferenceHash(bitmap: Bitmap): BooleanArray {
        val hashWidth = 16 
        val hashHeight = 16
        // We need (width + 1) pixels to get 'width' differences
        val resized = Bitmap.createScaledBitmap(bitmap, hashWidth + 1, hashHeight, true)
        val pixels = IntArray((hashWidth + 1) * hashHeight)
        resized.getPixels(pixels, 0, hashWidth + 1, 0, 0, hashWidth + 1, hashHeight)
        
        val grayPixels = IntArray((hashWidth + 1) * hashHeight)
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF
            grayPixels[i] = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
        }
        
        val hash = BooleanArray(hashWidth * hashHeight)
        for (y in 0 until hashHeight) {
            for (x in 0 until hashWidth) {
                val left = grayPixels[y * (hashWidth + 1) + x]
                val right = grayPixels[y * (hashWidth + 1) + x + 1]
                // 1 if left > right, else 0
                hash[y * hashWidth + x] = left > right
            }
        }
        
        resized.recycle()
        return hash
    }

    /**
     * Calculate similarity between two bitmaps using average hash comparison (aHash).
     * Returns value between 0.0 (no match) and 1.0 (perfect match)
     */
    private fun calculateSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        val hash1 = generateDifferenceHash(bitmap1)
        val hash2 = generateDifferenceHash(bitmap2)
        
        var matchingBits = 0
        for (i in hash1.indices) {
            if (hash1[i] == hash2[i]) {
                matchingBits++
            }
        }
        
        val score = matchingBits.toFloat() / hash1.size.toFloat()
        Log.v(TAG, "Hash match score: $score vs threshold $MATCH_THRESHOLD")
        return score
    }
    
    /**
     * Scale bitmap to maximum dimension while maintaining aspect ratio
     */
    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        }
        
        val scale = if (width > height) {
            maxDimension.toFloat() / width.toFloat()
        } else {
            maxDimension.toFloat() / height.toFloat()
        }
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    /**
     * Clear all loaded templates
     */
    suspend fun clearTemplates() {
        mutex.withLock {
            toRecycle = templates.toMutableList()
            templates.clear()
        }
        
        // Wait for readers then recycle
        try {
                while (activeReaders.get() > 0) {
                kotlinx.coroutines.delay(10)
            }
            toRecycle.forEach { it.bitmap.recycle() }
        } catch (e: Exception) {
            Log.e(TAG, "Error recycling bitmaps", e)
        }
        Log.d(TAG, "Cleared all templates")
    }
    
    fun onVideoStarted() {
        // Extend cooldown to prevent detection DURING video
        lastDetectionTime = System.currentTimeMillis()
        Log.d(TAG, "Cooldown extended: Video started")
    }

    /**
     * Get number of loaded templates
     */
    fun getTemplateCount(): Int = templates.size

    fun destroy() {
        preloadJob?.cancel()
        serviceScope.cancel()
        
        // Synchronously clear native resources to prevent leaks
        runBlocking(Dispatchers.IO) { 
            val job = serviceScope.coroutineContext[kotlinx.coroutines.Job]
            job?.join()
            clearTemplates() 
        }
    }
}

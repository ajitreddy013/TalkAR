package com.talkar.app.data.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.talkar.app.data.models.BackendImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.talkar.app.ar.video.tracking.ReferencePoster
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
 * Uses robust hybrid matching: 8x8 Average RGB Color Grid + 8x8 Block-level dHash
 */
class ImageMatcherService(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageMatcherService"
        private const val STRONG_MATCH_THRESHOLD = 0.74f
        private const val STABLE_MATCH_THRESHOLD = 0.60f
        private const val STABLE_MATCH_COUNT = 2
        private const val STABLE_MATCH_WINDOW_MS = 5000L
        private const val DEBOUNCE_MS = 1000L // Fast frame analysis check
        private const val COOLDOWN_MS = 8000L // 8s cooldown for same product
        private const val GRID_SIZE = 8 // 8x8 grid size
    }
    
    data class ImageTemplate(
        val id: String,
        val name: String,
        val description: String?,
        val colorGrid: FloatArray, // 8x8x3 = 192 floats
        val blockHash: BooleanArray, // 8x8x2 = 128 bits
        val dialogues: List<Any>?
    )
    
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
    private val mutex = Mutex()
    
    @Volatile
    private var lastDetectionTime: Long = 0
    
    @Volatile
    private var lastDetectedImageId: String? = null

    @Volatile
    private var candidateImageId: String? = null

    @Volatile
    private var candidateCount: Int = 0

    @Volatile
    private var candidateFirstSeenTime: Long = 0
    
    @Volatile
    var isUsingFallback: Boolean = false
        private set
    
    /**
     * Load reference images from backend API with fallback to local assets
     */
    suspend fun loadTemplatesFromBackend(): Result<Int> = withContext(Dispatchers.IO) {
        try {
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
                templates.clear()
            }
            resetCandidate()
            
            // Download and prepare each image
            backendImages.forEach { backendImage ->
                try {
                    val bitmap = downloadImage(backendImage.imageUrl)
                    if (bitmap != null) {
                        val colorGrid = generateColorGrid(bitmap)
                        val blockHash = generateBlockdHash(bitmap)
                        bitmap.recycle() // Free memory immediately
                        
                        mutex.withLock {
                            templates.add(
                                ImageTemplate(
                                    id = backendImage.id,
                                    name = backendImage.name,
                                    description = backendImage.description,
                                    colorGrid = colorGrid,
                                    blockHash = blockHash,
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
                templates.clear()
            }
            resetCandidate()
            
            fallbackImages.forEach { item ->
                try {
                    val bitmap = context.assets.open(item.imageUrl).use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                    
                    if (bitmap != null) {
                        val colorGrid = generateColorGrid(bitmap)
                        val blockHash = generateBlockdHash(bitmap)
                        bitmap.recycle() // Free memory immediately
                        
                        mutex.withLock {
                            templates.add(
                                ImageTemplate(
                                    id = item.id,
                                    name = item.name,
                                    description = item.description,
                                    colorGrid = colorGrid,
                                    blockHash = blockHash,
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
     * Load reference posters directly
     */
    suspend fun loadTemplates(referencePosters: List<ReferencePoster>) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading ${referencePosters.size} reference posters into matcher...")
            
            mutex.withLock {
                templates.clear()
            }
            resetCandidate()
            
            referencePosters.forEach { poster ->
                try {
                    val bitmap = BitmapFactory.decodeByteArray(poster.imageData, 0, poster.imageData.size)
                    if (bitmap != null) {
                        val colorGrid = generateColorGrid(bitmap)
                        val blockHash = generateBlockdHash(bitmap)
                        bitmap.recycle() // Free memory immediately
                        
                        mutex.withLock {
                            templates.add(
                                ImageTemplate(
                                    id = poster.id,
                                    name = poster.name,
                                    description = null,
                                    colorGrid = colorGrid,
                                    blockHash = blockHash,
                                    dialogues = null
                                )
                            )
                        }
                        Log.d(TAG, "Loaded template from ReferencePoster: ${poster.name} (ID: ${poster.id})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding ReferencePoster ${poster.name}", e)
                }
            }
            
            Log.d(TAG, "Successfully loaded ${templates.size} templates into matcher")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load templates from ReferencePosters", e)
        }
    }
    
    /**
     * Download image from URL
     */
    private suspend fun downloadImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = com.talkar.app.data.config.ApiConfig.getFullImageUrl(imageUrl)
            Log.d(TAG, "Downloading image from: $url")
            val inputStream = URL(url).openStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading image from $imageUrl", e)
            null
        }
    }
    
    /**
     * Match camera frame against all templates using multi-scale crops
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
            // 1. Grayscale standard deviation check for low contrast/detail (blank or dark frames)
            val checkBitmap = Bitmap.createScaledBitmap(frameBitmap, 32, 32, true)
            val stdDev = calculateGrayscaleStdDev(checkBitmap)
            checkBitmap.recycle()
            
            if (stdDev < 15.0) {
                Log.v(TAG, "Skipping frame due to low contrast/detail (stdDev=${String.format("%.2f", stdDev)})")
                return@withContext null
            }
            
            // 2. Extract 8x8 features for 3 crops: 55%, 75%, 95%
            val crop55 = cropCenter(frameBitmap, 0.55f, 0.55f)
            val grid55 = generateColorGrid(crop55)
            val hash55 = generateBlockdHash(crop55)
            crop55.recycle()
            
            val crop75 = cropCenter(frameBitmap, 0.75f, 0.75f)
            val grid75 = generateColorGrid(crop75)
            val hash75 = generateBlockdHash(crop75)
            crop75.recycle()
            
            val crop95 = cropCenter(frameBitmap, 0.95f, 0.95f)
            val grid95 = generateColorGrid(crop95)
            val hash95 = generateBlockdHash(crop95)
            crop95.recycle()
            
            var bestMatch: MatchResult? = null
            var bestScore = 0f
            
            // Compare against all templates
            val currentTemplates = mutex.withLock { templates.toList() }
            
            currentTemplates.forEach { template ->
                // Crop 55% similarity
                val colSim55 = calculateGridSimilarity(grid55, template.colorGrid)
                val hashSim55 = calculateHashSimilarity(hash55, template.blockHash)
                val sim55 = 0.5f * colSim55 + 0.5f * hashSim55
                
                // Crop 75% similarity
                val colSim75 = calculateGridSimilarity(grid75, template.colorGrid)
                val hashSim75 = calculateHashSimilarity(hash75, template.blockHash)
                val sim75 = 0.5f * colSim75 + 0.5f * hashSim75
                
                // Crop 95% similarity
                val colSim95 = calculateGridSimilarity(grid95, template.colorGrid)
                val hashSim95 = calculateHashSimilarity(hash95, template.blockHash)
                val sim95 = 0.5f * colSim95 + 0.5f * hashSim95
                
                val similarity = Math.max(sim55, Math.max(sim75, sim95))
                
                Log.d(TAG, "Comparing frame against ${template.name}: maxScore=${String.format("%.4f", similarity)} " +
                        "(sim55=${String.format("%.4f", sim55)} [col=${String.format("%.4f", colSim55)}, hash=${String.format("%.4f", hashSim55)}], " +
                        "sim75=${String.format("%.4f", sim75)} [col=${String.format("%.4f", colSim75)}, hash=${String.format("%.4f", hashSim75)}], " +
                        "sim95=${String.format("%.4f", sim95)} [col=${String.format("%.4f", colSim95)}, hash=${String.format("%.4f", hashSim95)}])")
                
                if (similarity > bestScore) {
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
            
            if (bestMatch != null) {
                val isConfirmed = shouldConfirmMatch(bestMatch!!, currentTime)
                if (!isConfirmed) {
                    Log.d(
                        TAG,
                        "Candidate match pending: ${bestMatch!!.imageName} " +
                            "(ID: ${bestMatch!!.imageId}) confidence=${String.format("%.2f", bestMatch!!.confidence)} " +
                            "stableCount=$candidateCount"
                    )
                    return@withContext null
                }

                // Hotfix: Strict Cooldown for the same product to prevent infinite play loops
                if (bestMatch!!.imageId == lastDetectedImageId && 
                    currentTime - lastDetectionTime < COOLDOWN_MS) {
                    Log.v(TAG, "Skipping re-detection of ${bestMatch!!.imageName} (Cooldown active)")
                    return@withContext null
                }

                // Only return if it's a different image or enough time has passed
                lastDetectionTime = currentTime
                lastDetectedImageId = bestMatch!!.imageId
                resetCandidate()
                Log.d(TAG, "MATCH CONFIRMED: ${bestMatch!!.imageName} (ID: ${bestMatch!!.imageId}) confidence=${String.format("%.2f", bestMatch!!.confidence)}")
                return@withContext bestMatch
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error matching frame", e)
            null
        }
    }
    
    /**
     * Generates an 8x8 Color Grid (R, G, B averages for each of the 64 blocks)
     */
    private fun generateColorGrid(bitmap: Bitmap): FloatArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, GRID_SIZE, GRID_SIZE, true)
        val pixels = IntArray(GRID_SIZE * GRID_SIZE)
        scaled.getPixels(pixels, 0, GRID_SIZE, 0, 0, GRID_SIZE, GRID_SIZE)
        scaled.recycle()
        
        val grid = FloatArray(GRID_SIZE * GRID_SIZE * 3)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8) and 0xFF) / 255f
            val b = (color and 0xFF) / 255f
            
            grid[i * 3] = r
            grid[i * 3 + 1] = g
            grid[i * 3 + 2] = b
        }
        return grid
    }

    /**
     * Calculates similarity (1.0 - average L1 color difference) between two color grids
     */
    private fun calculateGridSimilarity(grid1: FloatArray, grid2: FloatArray): Float {
        var totalDiff = 0f
        val numBlocks = grid1.size / 3
        
        for (i in 0 until numBlocks) {
            val r1 = grid1[i * 3]
            val g1 = grid1[i * 3 + 1]
            val b1 = grid1[i * 3 + 2]
            
            val r2 = grid2[i * 3]
            val g2 = grid2[i * 3 + 1]
            val b2 = grid2[i * 3 + 2]
            
            val diff = (Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2)) / 3f
            totalDiff += diff
        }
        
        val avgDiff = totalDiff / numBlocks
        return 1f - avgDiff
    }

    /**
     * Generates an 8x8 block-level difference hash (dHash, 128 bits total)
     */
    private fun generateBlockdHash(bitmap: Bitmap): BooleanArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, GRID_SIZE + 1, GRID_SIZE + 1, true)
        val pixels = IntArray((GRID_SIZE + 1) * (GRID_SIZE + 1))
        scaled.getPixels(pixels, 0, GRID_SIZE + 1, 0, 0, GRID_SIZE + 1, GRID_SIZE + 1)
        scaled.recycle()
        
        val gray = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            gray[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }
        
        val hash = BooleanArray(GRID_SIZE * GRID_SIZE * 2)
        var bitIndex = 0
        
        // Horizontal differences
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val left = gray[y * (GRID_SIZE + 1) + x]
                val right = gray[y * (GRID_SIZE + 1) + x + 1]
                hash[bitIndex++] = left > right
            }
        }
        
        // Vertical differences
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val top = gray[y * (GRID_SIZE + 1) + x]
                val bottom = gray[(y + 1) * (GRID_SIZE + 1) + x]
                hash[bitIndex++] = top > bottom
            }
        }
        
        return hash
    }

    /**
     * Calculates the matching bit percentage between two boolean hashes
     */
    private fun calculateHashSimilarity(hash1: BooleanArray, hash2: BooleanArray): Float {
        var matches = 0
        for (i in hash1.indices) {
            if (hash1[i] == hash2[i]) {
                matches++
            }
        }
        return matches.toFloat() / hash1.size.toFloat()
    }

    /**
     * Calculates the standard deviation of grayscale pixels to check contrast/detail
     */
    private fun calculateGrayscaleStdDev(bitmap: Bitmap): Double {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var sum = 0.0
        val grayPixels = DoubleArray(pixels.size)
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xFF
            val g = (pixels[i] shr 8) and 0xFF
            val b = pixels[i] and 0xFF
            val gray = r * 0.299 + g * 0.587 + b * 0.114
            grayPixels[i] = gray
            sum += gray
        }
        val mean = sum / pixels.size
        
        var sumSquares = 0.0
        for (i in grayPixels.indices) {
            val diff = grayPixels[i] - mean
            sumSquares += diff * diff
        }
        val variance = sumSquares / pixels.size
        return Math.sqrt(variance)
    }

    /**
     * Crops the center region of a bitmap
     */
    private fun cropCenter(bitmap: Bitmap, widthPercent: Float, heightPercent: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = (width * widthPercent).toInt()
        val newHeight = (height * heightPercent).toInt()
        val x = (width - newWidth) / 2
        val y = (height - newHeight) / 2
        return Bitmap.createBitmap(bitmap, x, y, newWidth, newHeight)
    }
    
    /**
     * Clear all loaded templates
     */
    suspend fun clearTemplates() {
        mutex.withLock {
            templates.clear()
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

    private fun shouldConfirmMatch(match: MatchResult, currentTime: Long): Boolean {
        if (match.confidence >= STRONG_MATCH_THRESHOLD) {
            return true
        }

        if (match.confidence < STABLE_MATCH_THRESHOLD) {
            resetCandidate()
            return false
        }

        if (candidateImageId != match.imageId || currentTime - candidateFirstSeenTime > STABLE_MATCH_WINDOW_MS) {
            candidateImageId = match.imageId
            candidateCount = 1
            candidateFirstSeenTime = currentTime
        } else {
            candidateCount += 1
        }

        return candidateCount >= STABLE_MATCH_COUNT
    }

    private fun resetCandidate() {
        candidateImageId = null
        candidateCount = 0
        candidateFirstSeenTime = 0
    }
}

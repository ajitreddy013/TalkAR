package com.talkar.app.data.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.talkar.app.data.models.BackendImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

/**
 * Service for matching camera frames against backend reference images
 * Uses simple template matching without heavy ML libraries
 */
class ImageMatcherService(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageMatcherService"
        private const val MATCH_THRESHOLD = 0.50 // Balanced for dHash robustness
        private const val DEBOUNCE_MS = 1000L // Faster feedback
        private const val MAX_DIMENSION = 512 // Downscale images for faster comparison
    }
    
    data class ImageTemplate(
        val id: String,
        val name: String,
        val description: String?,
        val bitmap: Bitmap,
        val dialogues: List<Any>?
    )
    
    data class MatchResult(
        val imageId: String,
        val imageName: String,
        val description: String?,
        val confidence: Float,
        val dialogues: List<Any>?
    )
    
    private val templates = mutableListOf<ImageTemplate>()
    private var lastDetectionTime: Long = 0
    private var lastDetectedImageId: String? = null
    
    /**
     * Load reference images from backend API
     */
    suspend fun loadTemplatesFromBackend(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading reference images from backend...")
            val apiService = com.talkar.app.data.api.ApiClient.create()
            val response = apiService.getImages()
            
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch images: ${response.code()}")
                return@withContext Result.failure(IOException("API returned ${response.code()}"))
            }
            
            val backendImages = response.body() ?: emptyList()
            Log.d(TAG, "Fetched ${backendImages.size} images from backend")
            
            templates.clear()
            
            // Download and prepare each image
            backendImages.forEach { backendImage ->
                try {
                    val bitmap = downloadAndPrepareBitmap(backendImage.imageUrl)
                    if (bitmap != null) {
                        templates.add(
                            ImageTemplate(
                                id = backendImage.id,
                                name = backendImage.name,
                                description = backendImage.description,
                                bitmap = bitmap,
                                dialogues = backendImage.dialogues as? List<Any>
                            )
                        )
                        Log.d(TAG, "Loaded template: ${backendImage.name}")
                    } else {
                        Log.w(TAG, "Failed to download image: ${backendImage.name}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image ${backendImage.name}", e)
                }
            }
            
            Log.d(TAG, "Successfully loaded ${templates.size} reference images")
            Result.success(templates.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading templates from backend", e)
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
            // Prepare frame for matching
            val scaledFrame = scaleBitmap(frameBitmap, MAX_DIMENSION)
            
            var bestMatch: MatchResult? = null
            var bestScore = 0f
            
            // Compare against all templates
            templates.forEach { template ->
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
                // Only return if it's a different image or enough time has passed
                if (bestMatch!!.imageId != lastDetectedImageId || 
                    currentTime - lastDetectionTime > DEBOUNCE_MS * 2) {
                    lastDetectionTime = currentTime
                    lastDetectedImageId = bestMatch!!.imageId
                    Log.d(TAG, "Match found: ${bestMatch!!.imageName} (${(bestMatch!!.confidence * 100).toInt()}%)")
                    return@withContext bestMatch
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error matching frame", e)
            null
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
    fun clearTemplates() {
        templates.forEach { it.bitmap.recycle() }
        templates.clear()
        Log.d(TAG, "Cleared all templates")
    }
    
    /**
     * Get number of loaded templates
     */
    fun getTemplateCount(): Int = templates.size
}

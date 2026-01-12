package com.talkar.app.data.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.talkar.app.data.api.RetrofitClient
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
        private const val MATCH_THRESHOLD = 0.65 // Minimum similarity score (0.0 - 1.0)
        private const val DEBOUNCE_MS = 2000L // Minimum time between same image detections
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
            val apiService = RetrofitClient.getApiService(context)
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
                                dialogues = backendImage.dialogues
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
            // Handle both local and remote URLs
            val url = if (imageUrl.startsWith("http")) {
                imageUrl
            } else {
                // Local URL - construct full URL from API base
                val baseUrl = RetrofitClient.getBaseUrl(context)
                if (imageUrl.startsWith("/")) {
                    "$baseUrl$imageUrl"
                } else {
                    "$baseUrl/$imageUrl"
                }
            }
            
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
     * Calculate similarity between two bitmaps using histogram comparison
     * Returns value between 0.0 (no match) and 1.0 (perfect match)
     */
    private fun calculateSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        // Ensure same dimensions for comparison
        if (bitmap1.width != bitmap2.width || bitmap1.height != bitmap2.height) {
            return 0f
        }
        
        // Simple pixel-by-pixel comparison with color histogram
        val width = bitmap1.width
        val height = bitmap1.height
        val totalPixels = width * height
        
        var matchingPixels = 0
        val colorTolerance = 40 // RGB tolerance per channel
        
        // Sample pixels for performance (every 4th pixel)
        for (y in 0 until height step 4) {
            for (x in 0 until width step 4) {
                val pixel1 = bitmap1.getPixel(x, y)
                val pixel2 = bitmap2.getPixel(x, y)
                
                val r1 = (pixel1 shr 16) and 0xFF
                val g1 = (pixel1 shr 8) and 0xFF
                val b1 = pixel1 and 0xFF
                
                val r2 = (pixel2 shr 16) and 0xFF
                val g2 = (pixel2 shr 8) and 0xFF
                val b2 = pixel2 and 0xFF
                
                val rDiff = Math.abs(r1 - r2)
                val gDiff = Math.abs(g1 - g2)
                val bDiff = Math.abs(b1 - b2)
                
                if (rDiff < colorTolerance && gDiff < colorTolerance && bDiff < colorTolerance) {
                    matchingPixels++
                }
            }
        }
        
        val sampledPixels = (width / 4) * (height / 4)
        return if (sampledPixels > 0) {
            matchingPixels.toFloat() / sampledPixels.toFloat()
        } else {
            0f
        }
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

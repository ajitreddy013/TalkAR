package com.talkar.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.talkar.app.ar.video.tracking.ReferencePoster
import com.talkar.app.data.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL

/**
 * Repository for loading poster images for AR tracking.
 * 
 * Loads posters from backend API and converts them to ReferencePoster format
 * for ARTrackingManager.
 * 
 * Requirements: 1.1, 1.2
 */
class PosterRepository(
    private val apiClient: ApiService,
    private val imageRepository: ImageRepository,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "PosterRepository"
        private const val DEFAULT_POSTER_WIDTH_METERS = 0.3f // 30cm default poster width
    }
    
    /**
     * Load all posters with human faces from backend.
     * 
     * Fetches images from backend, downloads the image data, and converts
     * to ReferencePoster format for AR tracking.
     */
    suspend fun loadPosters(): Result<List<ReferencePoster>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Loading posters from backend...")
            
            val posters = mutableListOf<ReferencePoster>()
            
            // Get all images from backend
            imageRepository.getAllImages().collect { images ->
                Log.d(TAG, "Found ${images.size} images from backend")
                
                for (image in images) {
                    try {
                        // Download image data
                        val imageData = downloadImage(image.imageUrl)
                        if (imageData == null) {
                            Log.w(TAG, "Failed to download image: ${image.name}")
                            continue
                        }
                        
                        // Check if image has human face (simplified - assume all have faces for now)
                        // TODO: Integrate with face detection service
                        val hasHumanFace = true
                        
                        val poster = ReferencePoster(
                            id = image.id,
                            name = image.name,
                            imageData = imageData,
                            physicalWidthMeters = DEFAULT_POSTER_WIDTH_METERS,
                            hasHumanFace = hasHumanFace
                        )
                        
                        posters.add(poster)
                        Log.d(TAG, "✅ Loaded poster: ${image.name}")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process image: ${image.name}", e)
                    }
                }
            }
            
            Log.d(TAG, "✅ Successfully loaded ${posters.size} posters")
            Result.success(posters)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load posters", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download image from URL and convert to ByteArray.
     */
    private suspend fun downloadImage(imageUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Downloading image: $imageUrl")
            
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from: $imageUrl")
                return@withContext null
            }
            
            // Convert bitmap to ByteArray
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val imageData = outputStream.toByteArray()
            outputStream.close()
            
            Log.d(TAG, "✅ Downloaded image: ${imageData.size} bytes")
            imageData
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download image: $imageUrl", e)
            null
        }
    }
    
    /**
     * Load a test poster from assets for development/testing.
     */
    suspend fun loadTestPoster(): Result<ReferencePoster> = withContext(Dispatchers.IO) {
        try {
            // Try to load a test image from assets
            val assetManager = context.assets
            val inputStream = assetManager.open("test_poster.jpg")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val imageData = outputStream.toByteArray()
            outputStream.close()
            
            val poster = ReferencePoster(
                id = "test_poster",
                name = "Test Poster",
                imageData = imageData,
                physicalWidthMeters = DEFAULT_POSTER_WIDTH_METERS,
                hasHumanFace = true
            )
            
            Log.d(TAG, "✅ Loaded test poster from assets")
            Result.success(poster)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load test poster from assets", e)
            Result.failure(e)
        }
    }
}

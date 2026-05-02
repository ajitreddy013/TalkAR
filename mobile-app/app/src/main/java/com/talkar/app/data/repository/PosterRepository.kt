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
        
        // Development mode - set to false to use real backend data
        // Set to true only for testing without backend
        private const val USE_MOCK_DATA = false
    }
    
    /**
     * Load all posters with human faces from backend.
     * 
     * Fetches images from backend, downloads the image data, and converts
     * to ReferencePoster format for AR tracking.
     * 
     * In development mode (USE_MOCK_DATA=true), returns mock posters instead.
     */
    suspend fun loadPosters(): Result<List<ReferencePoster>> = withContext(Dispatchers.IO) {
        try {
            // Use mock data in development mode
            if (USE_MOCK_DATA) {
                Log.d(TAG, "🔧 Development mode: Using mock poster data")
                return@withContext loadMockPosters()
            }

            Log.d(TAG, "📦 Poster loading pipeline: backend -> bundled fallback -> test poster")

            val backendPosters = loadPostersFromBackend()
            if (backendPosters.isNotEmpty()) {
                Log.d(TAG, "✅ Using backend posters (${backendPosters.size})")
                return@withContext Result.success(backendPosters)
            }
            Log.w(TAG, "⚠️ Backend posters unavailable or empty, switching to bundled fallback posters")

            val bundledFallbackResult = loadBundledFallbackPosters()
            val bundledFallback = bundledFallbackResult.getOrNull().orEmpty()
            if (bundledFallback.isNotEmpty()) {
                Log.d(TAG, "✅ Using bundled fallback posters (${bundledFallback.size})")
                return@withContext Result.success(bundledFallback)
            }
            Log.w(TAG, "⚠️ Bundled fallback posters unavailable, switching to test poster fallback")

            val testPosterResult = loadTestPoster()
            val testPoster = testPosterResult.getOrNull()
            if (testPoster != null) {
                Log.d(TAG, "✅ Using test poster fallback")
                return@withContext Result.success(listOf(testPoster))
            }

            val failure = testPosterResult.exceptionOrNull()
                ?: bundledFallbackResult.exceptionOrNull()
                ?: IllegalStateException("No posters available from backend, bundled assets, or test fallback")
            Log.e(TAG, "❌ Poster loading failed in all fallback stages", failure)
            Result.failure(failure)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load posters", e)
            Result.failure(e)
        }
    }

    /**
     * Load production fallback posters bundled in app assets.
     */
    suspend fun loadBundledFallbackPosters(): Result<List<ReferencePoster>> = withContext(Dispatchers.IO) {
        try {
            val fallbackFiles = listOf(
                Triple("fallback_water_bottle", "Sunrich Water Bottle", "fallbacks/water-bottle.jpg"),
                Triple("fallback_backpack", "Adventure Backpack", "fallbacks/backpack.jpg"),
                Triple("fallback_shoes", "Stride Pro Shoes", "fallbacks/shoes.jpg")
            )

            val posters = mutableListOf<ReferencePoster>()
            fallbackFiles.forEach { (id, name, assetPath) ->
                try {
                    val imageData = context.assets.open(assetPath).use { it.readBytes() }
                    posters.add(
                        ReferencePoster(
                            id = id,
                            name = name,
                            imageData = imageData,
                            physicalWidthMeters = DEFAULT_POSTER_WIDTH_METERS,
                            hasHumanFace = true
                        )
                    )
                    Log.d(TAG, "✅ Bundled fallback loaded: $assetPath")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to load bundled fallback asset: $assetPath", e)
                }
            }

            if (posters.isEmpty()) {
                Result.failure(IllegalStateException("No bundled fallback posters could be loaded"))
            } else {
                Result.success(posters)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed loading bundled fallback posters", e)
            Result.failure(e)
        }
    }

    private suspend fun loadPostersFromBackend(): List<ReferencePoster> = withContext(Dispatchers.IO) {
        val posters = mutableListOf<ReferencePoster>()
        Log.d(TAG, "🔍 Loading posters from backend...")

        try {
            val indexResponse = apiClient.getPosterIndex()
            if (indexResponse.isSuccessful) {
                val activePosters = indexResponse.body()?.posters.orEmpty()
                Log.d(TAG, "📥 Found ${activePosters.size} posters from /posters/index")
                for (poster in activePosters) {
                    try {
                        val imageData = downloadImage(poster.imageUrl)
                        if (imageData == null) {
                            Log.w(TAG, "    ❌ Failed to download image: ${poster.name}")
                            continue
                        }
                        posters.add(
                            ReferencePoster(
                                id = poster.id,
                                name = poster.name,
                                imageData = imageData,
                                physicalWidthMeters = DEFAULT_POSTER_WIDTH_METERS,
                                hasHumanFace = poster.talkingPhoto.eligible || poster.talkingPhoto.confidence == null
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "    ❌ Failed to process index poster: ${poster.name}", e)
                    }
                }
                if (posters.isNotEmpty()) {
                    return@withContext posters
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ /posters/index unavailable, falling back to legacy images list", e)
        }

        imageRepository.getAllImages().collect { images ->
            Log.d(TAG, "📥 Found ${images.size} images from backend")

            for (image in images) {
                try {
                    Log.d(TAG, "  - Image: ${image.name} (${image.id})")
                    val imageData = downloadImage(image.imageUrl)
                    if (imageData == null) {
                        Log.w(TAG, "    ❌ Failed to download image: ${image.name}")
                        continue
                    }

                    posters.add(
                        ReferencePoster(
                            id = image.id,
                            name = image.name,
                            imageData = imageData,
                            physicalWidthMeters = DEFAULT_POSTER_WIDTH_METERS,
                            hasHumanFace = true
                        )
                    )
                    Log.d(TAG, "    ✅ Loaded backend poster: ${image.name}")
                } catch (e: Exception) {
                    Log.e(TAG, "    ❌ Failed to process backend image: ${image.name}", e)
                }
            }
        }

        posters
    }
    
    /**
     * Download image from URL and convert to ByteArray.
     */
    private suspend fun downloadImage(imageUrl: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            // Convert relative URL to absolute URL
            val fullUrl = if (imageUrl.startsWith("http")) {
                imageUrl
            } else {
                com.talkar.app.data.config.ApiConfig.getFullImageUrl(imageUrl)
            }
            
            Log.d(TAG, "Downloading image: $fullUrl")
            
            val url = URL(fullUrl)
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
     * If asset file doesn't exist, creates a programmatic test image.
     */
    suspend fun loadTestPoster(): Result<ReferencePoster> = withContext(Dispatchers.IO) {
        try {
            // Try to load a test image from assets
            val assetManager = context.assets
            val inputStream = try {
                assetManager.open("test_poster.jpg")
            } catch (e: java.io.FileNotFoundException) {
                Log.w(TAG, "test_poster.jpg not found in assets, creating programmatic test image")
                null
            }
            
            val bitmap = if (inputStream != null) {
                // Load from assets
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                bmp
            } else {
                // Create a simple test image programmatically
                createTestBitmap()
            }
            
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
            
            Log.d(TAG, "✅ Loaded test poster (${bitmap.width}x${bitmap.height})")
            Result.success(poster)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load test poster", e)
            Result.failure(e)
        }
    }
    
    /**
     * Creates a simple test bitmap programmatically.
     * Used when test_poster.jpg is not available in assets.
     */
    private fun createTestBitmap(): Bitmap {
        // Create a 512x512 bitmap with a simple pattern
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Draw gradient background
        val paint = android.graphics.Paint()
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 512f, 512f,
            android.graphics.Color.rgb(100, 150, 200),
            android.graphics.Color.rgb(200, 100, 150),
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, 512f, 512f, paint)
        
        // Draw text
        paint.shader = null
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 48f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.style = android.graphics.Paint.Style.FILL
        
        canvas.drawText("TEST POSTER", 256f, 200f, paint)
        canvas.drawText("Point camera here", 256f, 280f, paint)
        
        // Draw a simple face-like pattern
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 4f
        
        // Face circle
        canvas.drawCircle(256f, 380f, 80f, paint)
        
        // Eyes
        canvas.drawCircle(230f, 360f, 10f, paint)
        canvas.drawCircle(282f, 360f, 10f, paint)
        
        // Smile
        val path = android.graphics.Path()
        path.moveTo(220f, 390f)
        path.quadTo(256f, 420f, 292f, 390f)
        canvas.drawPath(path, paint)
        
        Log.d(TAG, "Created programmatic test bitmap (512x512)")
        return bitmap
    }
    
    /**
     * Load mock posters for development/testing.
     * Creates simple colored bitmaps as placeholders.
     */
    private suspend fun loadMockPosters(): Result<List<ReferencePoster>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating mock poster data...")
            
            val posters = mutableListOf<ReferencePoster>()
            
            // Try to load test poster from assets first
            val testPosterResult = loadTestPoster()
            if (testPosterResult.isSuccess) {
                testPosterResult.getOrNull()?.let { posters.add(it) }
            }
            
            // If no test poster, create simple colored bitmaps as mock data
            if (posters.isEmpty()) {
                Log.d(TAG, "No test poster in assets, creating colored mock posters")
                
                val mockPosters = listOf(
                    Triple("mock_poster_1", "Mock Poster 1", android.graphics.Color.RED),
                    Triple("mock_poster_2", "Mock Poster 2", android.graphics.Color.BLUE),
                    Triple("mock_poster_3", "Mock Poster 3", android.graphics.Color.GREEN)
                )
                
                for ((id, name, color) in mockPosters) {
                    try {
                        // Create a simple colored bitmap (512x512)
                        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(color)
                        
                        // Add text to identify the poster
                        val paint = android.graphics.Paint().apply {
                            this.color = android.graphics.Color.WHITE
                            textSize = 48f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        canvas.drawText(name, 256f, 256f, paint)
                        
                        // Convert to ByteArray
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        val imageData = outputStream.toByteArray()
                        outputStream.close()
                        
                        val poster = ReferencePoster(
                            id = id,
                            name = name,
                            imageData = imageData,
                            physicalWidthMeters = DEFAULT_POSTER_WIDTH_METERS,
                            hasHumanFace = true
                        )
                        
                        posters.add(poster)
                        Log.d(TAG, "✅ Created mock poster: $name")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create mock poster: $name", e)
                    }
                }
            }
            
            Log.d(TAG, "✅ Successfully loaded ${posters.size} mock posters")
            Result.success(posters)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load mock posters", e)
            Result.failure(e)
        }
    }
}

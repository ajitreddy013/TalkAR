package com.talkar.app.ar

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Session
import java.io.IOException

/**
 * Manages ARCore Augmented Image Database for stable 3D image tracking.
 * 
 * This class loads reference images from assets and creates an ARCore database
 * with physical width specifications for accurate 3D tracking.
 */
class AugmentedImageDatabaseManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AugmentedImageDB"
        
        // Physical width of printed posters in meters (80cm = 0.8m)
        private const val IMAGE_PHYSICAL_WIDTH_METERS = 0.8f
        
        // Reference image paths in assets
        private const val SUNRICH_IMAGE_PATH = "images/sunrich.jpg"
        // Add more images here as needed
        // private const val CHANEL_IMAGE_PATH = "images/chanel.png"
        // private const val LEBRON_IMAGE_PATH = "images/lebronboy.png"
    }
    
    /**
     * Creates an ARCore AugmentedImageDatabase with all reference images.
     * 
     * @param session Active ARCore session
     * @return Configured AugmentedImageDatabase ready for tracking
     * @throws IOException if image files cannot be loaded
     */
    fun createDatabase(session: Session): AugmentedImageDatabase {
        val database = AugmentedImageDatabase(session)
        var imageCount = 0
        
        try {
            // Load Sunrich Water Bottle image
            val sunrichIndex = addImageToDatabase(
                database = database,
                imageName = "sunrich",
                assetPath = SUNRICH_IMAGE_PATH,
                widthInMeters = IMAGE_PHYSICAL_WIDTH_METERS
            )
            
            if (sunrichIndex >= 0) {
                imageCount++
                Log.i(TAG, "✅ Added Sunrich image at index $sunrichIndex (width: ${IMAGE_PHYSICAL_WIDTH_METERS}m)")
            }
            
            // TODO: Add more reference images here
            // Example:
            // val chanelIndex = addImageToDatabase(
            //     database = database,
            //     imageName = "chanel",
            //     assetPath = CHANEL_IMAGE_PATH,
            //     widthInMeters = IMAGE_PHYSICAL_WIDTH_METERS
            // )
            
            Log.i(TAG, "✅ Database created successfully with $imageCount image(s)")
            
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to load reference images", e)
            throw e
        }
        
        return database
    }
    
    /**
     * Adds a single image to the database from assets.
     * 
     * @param database The AugmentedImageDatabase to add to
     * @param imageName Unique identifier for this image
     * @param assetPath Path to image in assets folder
     * @param widthInMeters Physical width of the printed image in meters
     * @return Index of added image, or -1 if failed
     */
    private fun addImageToDatabase(
        database: AugmentedImageDatabase,
        imageName: String,
        assetPath: String,
        widthInMeters: Float
    ): Int {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                if (bitmap == null) {
                    Log.e(TAG, "❌ Failed to decode bitmap from $assetPath")
                    return -1
                }
                
                // Add image with physical width for accurate tracking
                val index = database.addImage(imageName, bitmap, widthInMeters)
                
                Log.d(TAG, "Added image '$imageName' from $assetPath")
                Log.d(TAG, "  - Dimensions: ${bitmap.width}x${bitmap.height}")
                Log.d(TAG, "  - Physical width: ${widthInMeters}m")
                Log.d(TAG, "  - Database index: $index")
                
                // Clean up bitmap
                bitmap.recycle()
                
                index
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to load image from $assetPath", e)
            -1
        }
    }
    
    /**
     * Validates that all required reference images exist in assets.
     * Call this during app initialization to catch missing images early.
     * 
     * @return List of missing image paths, empty if all images exist
     */
    fun validateReferenceImages(): List<String> {
        val missingImages = mutableListOf<String>()
        
        val imagesToCheck = listOf(
            SUNRICH_IMAGE_PATH
            // Add more paths here as you add images
        )
        
        imagesToCheck.forEach { path ->
            try {
                context.assets.open(path).use { 
                    // Image exists and can be opened
                }
            } catch (e: IOException) {
                missingImages.add(path)
                Log.w(TAG, "⚠️ Missing reference image: $path")
            }
        }
        
        return missingImages
    }
    
    /**
     * Gets the expected physical width for reference images.
     * Use this when printing posters to ensure correct scale.
     * 
     * @return Physical width in meters (0.8m = 80cm)
     */
    fun getImagePhysicalWidth(): Float = IMAGE_PHYSICAL_WIDTH_METERS
}

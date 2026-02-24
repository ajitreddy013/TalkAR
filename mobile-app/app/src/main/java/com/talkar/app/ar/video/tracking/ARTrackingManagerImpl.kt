package com.talkar.app.ar.video.tracking

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingState as ARCoreTrackingState

/**
 * Implementation of ARTrackingManager using ARCore.
 *
 * Features:
 * - Single poster mode (tracks only one poster at a time)
 * - Human face detection filter
 * - 60fps tracking updates
 * - Out-of-frame detection
 * - Refresh scan functionality
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 6.1, 6.2, 6.3, 6.4
 */
class ARTrackingManagerImpl(
    private val context: Context,
    private val session: Session
) : ARTrackingManager {
    
    companion object {
        private const val TAG = "ARTrackingManager"
        private const val DETECTION_TIMEOUT_MS = 2000L // 2 seconds
    }
    
    private var imageDatabase: AugmentedImageDatabase? = null
    private var currentTrackedPoster: TrackedPoster? = null
    private var currentAnchor: Anchor? = null
    private var listener: TrackingListener? = null
    private var posterMap: Map<Int, ReferencePoster> = emptyMap()
    private var detectionStartTime: Long = 0
    
    override suspend fun initialize(posters: List<ReferencePoster>): Result<Unit> {
        return try {
            Log.d(TAG, "Initializing AR tracking with ${posters.size} posters")
            
            // Filter posters to only those with human faces
            val humanFacePosters = posters.filter { it.hasHumanFace }
            Log.d(TAG, "Filtered to ${humanFacePosters.size} posters with human faces")
            
            if (humanFacePosters.isEmpty()) {
                return Result.failure(Exception("No posters with human faces provided"))
            }
            
            // Create augmented image database
            val database = AugmentedImageDatabase(session)
            
            // Add each poster to the database
            humanFacePosters.forEachIndexed { index, poster ->
                val bitmap = BitmapFactory.decodeByteArray(
                    poster.imageData,
                    0,
                    poster.imageData.size
                )
                
                if (bitmap == null) {
                    Log.w(TAG, "Failed to decode image for poster: ${poster.id}")
                    return@forEachIndexed
                }
                
                val imageIndex = database.addImage(
                    poster.name,
                    bitmap,
                    poster.physicalWidthMeters
                )
                
                posterMap = posterMap + (imageIndex to poster)
                Log.d(TAG, "Added poster ${poster.id} at index $imageIndex")
            }
            
            imageDatabase = database
            detectionStartTime = System.currentTimeMillis()
            
            Log.d(TAG, "✅ AR tracking initialized successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize AR tracking", e)
            Result.failure(e)
        }
    }
    
    override fun processFrame(frame: Frame): TrackedPoster? {
        // Check for detection timeout
        if (currentTrackedPoster == null) {
            val elapsed = System.currentTimeMillis() - detectionStartTime
            if (elapsed > DETECTION_TIMEOUT_MS) {
                // Reset timer for next attempt
                detectionStartTime = System.currentTimeMillis()
            }
        }
        
        // Get all tracked augmented images
        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        
        for (augmentedImage in updatedAugmentedImages) {
            // Single poster mode: if we're already tracking a poster, ignore others
            if (currentTrackedPoster != null && 
                augmentedImage.index != posterMap.entries.find { 
                    it.value.id == currentTrackedPoster?.id 
                }?.key) {
                continue
            }
            
            when (augmentedImage.trackingState) {
                ARCoreTrackingState.TRACKING -> {
                    val poster = posterMap[augmentedImage.index]
                    if (poster == null) {
                        Log.w(TAG, "Unknown augmented image index: ${augmentedImage.index}")
                        continue
                    }
                    
                    // Create or update tracked poster
                    val trackedPoster = TrackedPoster(
                        id = poster.id,
                        name = poster.name,
                        anchor = augmentedImage.createAnchor(augmentedImage.centerPose),
                        trackingState = TrackingState.TRACKING,
                        extentX = augmentedImage.extentX,
                        extentZ = augmentedImage.extentZ
                    )
                    
                    if (currentTrackedPoster == null) {
                        // First detection
                        currentTrackedPoster = trackedPoster
                        currentAnchor = trackedPoster.anchor
                        listener?.onPosterDetected(trackedPoster)
                        Log.d(TAG, "✅ Poster detected: ${poster.id}")
                    } else {
                        // Update existing tracking
                        currentTrackedPoster = trackedPoster
                        currentAnchor = trackedPoster.anchor
                        listener?.onPosterTracking(trackedPoster)
                    }
                    
                    return trackedPoster
                }
                
                ARCoreTrackingState.PAUSED -> {
                    // Poster temporarily lost (out of frame)
                    if (currentTrackedPoster != null && 
                        posterMap[augmentedImage.index]?.id == currentTrackedPoster?.id) {
                        Log.d(TAG, "⏸️ Poster tracking paused: ${currentTrackedPoster?.id}")
                        listener?.onPosterLost(currentTrackedPoster!!.id)
                    }
                }
                
                ARCoreTrackingState.STOPPED -> {
                    // Poster tracking stopped
                    if (currentTrackedPoster != null && 
                        posterMap[augmentedImage.index]?.id == currentTrackedPoster?.id) {
                        Log.d(TAG, "⏹️ Poster tracking stopped: ${currentTrackedPoster?.id}")
                        listener?.onPosterLost(currentTrackedPoster!!.id)
                        currentTrackedPoster = null
                        currentAnchor = null
                    }
                }
            }
        }
        
        return currentTrackedPoster
    }
    
    override fun getCurrentAnchor(): Anchor? {
        return currentAnchor
    }
    
    override fun refreshScan() {
        Log.d(TAG, "🔄 Refresh scan - clearing current poster")
        
        // Clear current tracking
        val previousPosterId = currentTrackedPoster?.id
        currentTrackedPoster = null
        currentAnchor?.detach()
        currentAnchor = null
        
        // Reset detection timer
        detectionStartTime = System.currentTimeMillis()
        
        if (previousPosterId != null) {
            listener?.onPosterLost(previousPosterId)
        }
    }
    
    override fun setListener(listener: TrackingListener) {
        this.listener = listener
    }
    
    override fun release() {
        Log.d(TAG, "Releasing AR tracking resources")
        
        currentAnchor?.detach()
        currentAnchor = null
        currentTrackedPoster = null
        imageDatabase = null
        posterMap = emptyMap()
        listener = null
    }
}

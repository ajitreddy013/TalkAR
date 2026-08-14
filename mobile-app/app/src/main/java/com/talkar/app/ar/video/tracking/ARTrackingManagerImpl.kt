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
import com.google.ar.core.exceptions.SessionPausedException
import com.talkar.app.ar.video.errors.TalkingPhotoError

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
        private const val DETECTION_TIMEOUT_MS = 30000L // 30 seconds (Requirement 14.1)
    }
    
    private var imageDatabase: AugmentedImageDatabase? = null
    private var currentTrackedPoster: TrackedPoster? = null
    private var currentAnchor: Anchor? = null
    private var listener: TrackingListener? = null
    private var posterMap: Map<Int, ReferencePoster> = emptyMap()
    private var detectionStartTime: Long = 0
    private var hasReportedTimeout: Boolean = false
    private var hasProcessedScanFrame: Boolean = false
    // Per-image ARCore state from the previous frame — used to detect PAUSED→TRACKING transitions.
    private val previousTrackingStates = mutableMapOf<Int, ARCoreTrackingState>()
    // ARCore index of the poster that was being tracked when refreshScan() was called.
    // We skip re-detecting it until it leaves the frame (PAUSED), but accept any OTHER poster immediately.
    private var trackedIndexAtReset: Int = -1
    
    override fun getAugmentedImageDatabase(): AugmentedImageDatabase? = imageDatabase
    
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
            Log.d(TAG, "Created AugmentedImageDatabase, numImages=${database.numImages}")
            
            // Add each poster to the database
            humanFacePosters.forEachIndexed { index, poster ->
                try {
                    val bitmap = BitmapFactory.decodeByteArray(
                        poster.imageData,
                        0,
                        poster.imageData.size
                    )

                    if (bitmap == null) {
                        Log.w(TAG, "Failed to decode image for poster: ${poster.id}")
                        return@forEachIndexed
                    }

                    Log.d(TAG, "Bitmap for ${poster.id}: ${bitmap.width}×${bitmap.height}, physicalWidth=${poster.physicalWidthMeters}m → extentZ≈${poster.physicalWidthMeters * bitmap.height.toFloat() / bitmap.width}m")
                    val imageIndex = database.addImage(
                        poster.id, // Use poster ID as the unique key in the database
                        bitmap,
                        poster.physicalWidthMeters
                    )

                    posterMap = posterMap + (imageIndex to poster)
                    Log.d(TAG, "✅ Added poster ${poster.id} (name: ${poster.name}) at ARCore index $imageIndex. Database now has ${database.numImages} images.")
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping low-quality/invalid poster image: ${poster.id}", e)
                }
            }

            if (posterMap.isEmpty()) {
                return Result.failure(Exception("No usable poster images for AR tracking"))
            }
            
            imageDatabase = database
            detectionStartTime = 0
            hasReportedTimeout = false
            hasProcessedScanFrame = false
            
            Log.d(TAG, "✅ AR tracking initialized successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize AR tracking", e)
            Result.failure(e)
        }
    }
    
    override fun processFrame(frame: Frame): TrackedPoster? {
        try {
            // Check for detection timeout (Requirement 14.1)
            if (currentTrackedPoster == null) {
                if (!hasProcessedScanFrame) {
                    detectionStartTime = System.currentTimeMillis()
                    hasReportedTimeout = false
                    hasProcessedScanFrame = true
                }
                val elapsed = System.currentTimeMillis() - detectionStartTime
                if (elapsed > DETECTION_TIMEOUT_MS && !hasReportedTimeout) {
                    Log.w(TAG, "⚠️ Poster detection timeout after ${elapsed}ms")
                    listener?.onDetectionTimeout()
                    hasReportedTimeout = true
                    // Don't reset timer - let user take action
                    return null
                }
            }
            
            // Get all tracked augmented images
            val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            if (!updatedAugmentedImages.isEmpty()) {
                Log.d(TAG, "Frame update: found ${updatedAugmentedImages.size} augmented images")
            }

            for (augmentedImage in updatedAugmentedImages) {
                val prevState = previousTrackingStates[augmentedImage.index]
                val currState = augmentedImage.trackingState
                previousTrackingStates[augmentedImage.index] = currState

                // Single poster mode: if we're already tracking a poster, ignore others
                if (currentTrackedPoster != null &&
                    augmentedImage.index != posterMap.entries.find {
                        it.value.id == currentTrackedPoster?.id
                    }?.key) {
                    continue
                }

                when (currState) {
                    ARCoreTrackingState.TRACKING -> {
                        val poster = posterMap[augmentedImage.index]
                        if (poster == null) {
                            Log.w(TAG, "Unknown augmented image index: ${augmentedImage.index}")
                            continue
                        }

                        if (currentTrackedPoster == null) {
                            // After a reset, skip the same poster that was previously tracked
                            // while it stays continuously TRACKING (still in frame).
                            // Accept any OTHER poster immediately — no waiting for transitions.
                            if (trackedIndexAtReset == augmentedImage.index &&
                                prevState == ARCoreTrackingState.TRACKING) {
                                Log.d(TAG, "Skipping ${poster.name} — was tracked at reset, waiting for it to leave frame")
                                continue
                            }
                            trackedIndexAtReset = -1
                            val trackedPoster = TrackedPoster(
                                id = poster.id,
                                name = poster.name,
                                anchor = augmentedImage.createAnchor(augmentedImage.centerPose),
                                trackingState = TrackingState.TRACKING,
                                extentX = augmentedImage.extentX,
                                extentZ = augmentedImage.extentZ
                            )
                            currentTrackedPoster = trackedPoster
                            currentAnchor = trackedPoster.anchor
                            listener?.onPosterDetected(trackedPoster)
                            Log.d(TAG, "✅ Poster detected: ${poster.id} name=${poster.name}")
                        } else {
                            val trackedPoster = currentTrackedPoster!!.copy(
                                trackingState = TrackingState.TRACKING,
                                extentX = augmentedImage.extentX,
                                extentZ = augmentedImage.extentZ
                            )
                            currentTrackedPoster = trackedPoster
                            listener?.onPosterTracking(trackedPoster)
                        }

                        return currentTrackedPoster
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
        } catch (e: SessionPausedException) {
            Log.w(TAG, "ARCore session is paused. Cannot process frame.")
            return null
        }
    }
    
    override fun getCurrentAnchor(): Anchor? {
        return currentAnchor
    }
    
    override fun refreshScan() {
        Log.d(TAG, "🔄 Refresh scan - clearing current poster")
        val previousPosterId = currentTrackedPoster?.id
        // Record the ARCore index of the poster we were tracking so we can skip
        // re-detecting it immediately (it may still be visible in frame).
        trackedIndexAtReset = posterMap.entries
            .find { it.value.id == currentTrackedPoster?.id }?.key ?: -1
        Log.d(TAG, "  trackedIndexAtReset=$trackedIndexAtReset (poster=$previousPosterId)")
        currentTrackedPoster = null
        currentAnchor?.detach()
        currentAnchor = null
        detectionStartTime = System.currentTimeMillis()
        hasReportedTimeout = false
        hasProcessedScanFrame = false
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

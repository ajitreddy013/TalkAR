package com.talkar.app.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.TrackingState
import com.talkar.app.ar.ARGestureDetector
import com.talkar.app.ar.ARSessionConfig
import com.talkar.app.ar.ARVideoOverlay
import com.talkar.app.ar.AugmentedImageDatabaseManager
import com.talkar.app.ui.components.VideoOverlayView
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode

/**
 * Main AR view for TalkAR using Sceneview and ARCore Augmented Images.
 * 
 * This component:
 * - Displays AR camera feed
 * - Detects reference images (posters)
 * - Tracks images in 3D space
 * - Handles gesture interactions (long-press to play video)
 * - Manages video playback on detected images
 * - Provides callbacks for image detection/loss
 * 
 * @param modifier Compose modifier
 * @param onImageDetected Callback when an image is detected (imageName)
 * @param onImageLost Callback when an image is lost (imageName)
 * @param onImageLongPressed Callback when user long-presses on detected image (imageName)
 * @param videoUriToPlay URI of video to play (when set, plays on detected image)
 * @param onVideoCompleted Callback when video playback completes
 * @param onError Callback for errors
 */
@Composable
fun TalkARView(
    modifier: Modifier = Modifier,
    onImageDetected: (imageName: String) -> Unit = {},
    onImageLost: (imageName: String) -> Unit = {},
    onImageLongPressed: (imageName: String) -> Unit = {},
    videoUriToPlay: android.net.Uri? = null,
    onVideoCompleted: () -> Unit = {},
    onError: (error: String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // AR managers
    val dbManager = remember { AugmentedImageDatabaseManager(context) }
    val sessionConfig = remember { ARSessionConfig() }
    
    // Track detected images and their video overlays
    var trackedImageNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var arSceneView by remember { mutableStateOf<ARSceneView?>(null) }
    
    // Store video overlays for playback control
    val videoOverlaysRef = remember { mutableMapOf<String, ARVideoOverlay>() }
    
    // Current video overlay to display
    var currentOverlay by remember { mutableStateOf<ARVideoOverlay?>(null) }
    var overlayPosition by remember { mutableStateOf<ARVideoOverlay.OverlayPosition?>(null) }
    
    // Update overlay position every frame
    LaunchedEffect(currentOverlay, arSceneView) {
        currentOverlay?.let { overlay ->
            arSceneView?.let { view ->
                // Update position continuously
                kotlinx.coroutines.delay(16) // ~60fps
                while (true) {
                    overlay.updatePosition(view.width, view.height)
                    overlayPosition = overlay.position
                    kotlinx.coroutines.delay(16)
                }
            }
        }
    }
    
    // Play video when videoUriToPlay changes
    LaunchedEffect(videoUriToPlay) {
        Log.i(TAG, "========================================")
        Log.i(TAG, "🔄 LaunchedEffect triggered!")
        Log.i(TAG, "   videoUriToPlay: $videoUriToPlay")
        Log.i(TAG, "   trackedImageNames: $trackedImageNames")
        Log.i(TAG, "   videoOverlaysRef size: ${videoOverlaysRef.size}")
        Log.i(TAG, "   videoOverlaysRef keys: ${videoOverlaysRef.keys}")
        Log.i(TAG, "========================================")
        
        videoUriToPlay?.let { uri ->
            Log.i(TAG, "✅ Video URI is not null, attempting to play...")
            
            // Play video on the first tracked image
            trackedImageNames.firstOrNull()?.let { imageName ->
                Log.i(TAG, "✅ Found tracked image: $imageName")
                
                videoOverlaysRef[imageName]?.let { overlay ->
                    Log.i(TAG, "========================================")
                    Log.i(TAG, "🎬 PLAYING VIDEO ON AR OVERLAY")
                    Log.i(TAG, "   Image: $imageName")
                    Log.i(TAG, "   URI: $uri")
                    Log.i(TAG, "   Overlay instance: $overlay")
                    Log.i(TAG, "========================================")
                    
                    overlay.onVideoCompleted = {
                        Log.i(TAG, "Video completed callback triggered")
                        onVideoCompleted()
                    }
                    overlay.onVideoError = { error ->
                        Log.e(TAG, "Video error callback: $error")
                        onError(error)
                    }
                    
                    Log.i(TAG, "🚀 Calling overlay.loadVideo()...")
                    overlay.loadVideo(uri, autoPlay = true)
                    currentOverlay = overlay
                    Log.i(TAG, "✅ overlay.loadVideo() called successfully")
                } ?: run {
                    Log.e(TAG, "❌ Video overlay not found for image: $imageName")
                    Log.e(TAG, "   Available overlays: ${videoOverlaysRef.keys}")
                }
            } ?: run {
                Log.e(TAG, "❌ No tracked images found")
                Log.e(TAG, "   trackedImageNames is empty!")
            }
        } ?: run {
            Log.d(TAG, "ℹ️ Video URI is null, nothing to play")
        }
    }
    
    // Gesture detector for long-press interactions
    val gestureDetector = remember {
        ARGestureDetector(
            context = context,
            onLongPress = { x, y ->
                // When user long-presses, trigger video for the first tracked image
                trackedImageNames.firstOrNull()?.let { imageName ->
                    Log.d(TAG, "Long press on tracked image: $imageName")
                    onImageLongPressed(imageName)
                }
            }
        )
    }
    
    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // arSceneView?.pause() // TODO: Implement when Sceneview API is confirmed
                    Log.d(TAG, "AR view paused")
                }
                Lifecycle.Event.ON_RESUME -> {
                    // arSceneView?.resume() // TODO: Implement when Sceneview API is confirmed
                    Log.d(TAG, "AR view resumed")
                }
                Lifecycle.Event.ON_DESTROY -> {
                    arSceneView?.destroy()
                    Log.d(TAG, "AR view destroyed")
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            arSceneView?.destroy()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                createARSceneView(
                    context = ctx,
                    dbManager = dbManager,
                    sessionConfig = sessionConfig,
                    gestureDetector = gestureDetector,
                    videoOverlaysRef = videoOverlaysRef,
                    onImageDetected = { imageName ->
                        Log.i(TAG, "✅ Image detected: $imageName")
                        trackedImageNames = trackedImageNames + imageName
                        onImageDetected(imageName)
                    },
                    onImageLost = { imageName ->
                        Log.i(TAG, "❌ Image lost: $imageName")
                        trackedImageNames = trackedImageNames - imageName
                        onImageLost(imageName)
                    },
                    onError = onError,
                    onViewCreated = { view ->
                        arSceneView = view
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Video overlay that tracks the AR image
        overlayPosition?.let { position ->
            currentOverlay?.let { overlay ->
                VideoOverlayView(
                    player = overlay.getPlayer(),
                    x = position.x,
                    y = position.y,
                    width = position.width,
                    height = position.height,
                    visible = position.visible
                )
            }
        }
    }
}

/**
 * Creates and configures the ARSceneView.
 */
private fun createARSceneView(
    context: Context,
    dbManager: AugmentedImageDatabaseManager,
    sessionConfig: ARSessionConfig,
    gestureDetector: ARGestureDetector,
    videoOverlaysRef: MutableMap<String, ARVideoOverlay>,
    onImageDetected: (String) -> Unit,
    onImageLost: (String) -> Unit,
    onError: (String) -> Unit,
    onViewCreated: (ARSceneView) -> Unit
): ARSceneView {
    
    return ARSceneView(context).apply {
        // Store currently tracked images and their overlays
        val trackedImageOverlays = mutableMapOf<Int, Pair<String, ARVideoOverlay>>()
        
        // Set up gesture handling
        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        
        Log.d(TAG, "ARSceneView created, configuring session...")
        
        // Configure session after view is attached
        post {
            configureARSession(dbManager, onError)
        }
        
        // Set up frame callback to process AR frames
        onSessionUpdated = { session, frame ->
            try {
                processFrame(frame, trackedImageOverlays, videoOverlaysRef, onImageDetected, onImageLost)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame: ${e.message}", e)
            }
        }
        
        Log.i(TAG, "✅ ARSceneView setup complete")
        
        // Notify when view is created
        onViewCreated(this)
    }
}

/**
 * Configures the AR session with augmented image tracking.
 */
private fun ARSceneView.configureARSession(
    dbManager: AugmentedImageDatabaseManager,
    onError: (String) -> Unit
) {
    try {
        // Access session through session property (may be null initially)
        val currentSession = session
        if (currentSession == null) {
            Log.w(TAG, "AR session not ready yet, will retry...")
            postDelayed({ configureARSession(dbManager, onError) }, 500)
            return
        }
        
        Log.d(TAG, "AR session available, configuring...")
        
        // Validate reference images exist
        val missingImages = dbManager.validateReferenceImages()
        if (missingImages.isNotEmpty()) {
            val error = "Missing reference images: ${missingImages.joinToString()}"
            Log.e(TAG, error)
            onError(error)
            return
        }
        
        // Create augmented image database
        val database = dbManager.createDatabase(currentSession)
        Log.i(TAG, "✅ Image database created with ${database.numImages} image(s)")
        
        // Create new config with our settings
        val newConfig = Config(currentSession).apply {
            // Disable plane detection
            planeFindingMode = Config.PlaneFindingMode.DISABLED
            
            // Set the augmented image database
            augmentedImageDatabase = database
            
            // Use latest camera image for best tracking
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            
            // Auto focus
            focusMode = Config.FocusMode.AUTO
            
            // Enable depth if supported
            if (currentSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                depthMode = Config.DepthMode.AUTOMATIC
                Log.d(TAG, "✅ Depth mode enabled")
            }
            
            // Enable light estimation
            lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
        }
        
        // Apply the configuration
        currentSession.configure(newConfig)
        
        Log.i(TAG, "✅✅✅ AR session configured successfully with ${database.numImages} images!")
        Log.i(TAG, "Ready to detect images - point camera at Sunrich poster")
        
    } catch (e: Exception) {
        val error = "AR configuration failed: ${e.message}"
        Log.e(TAG, error, e)
        onError(error)
    }
}

/**
 * Processes each AR frame to detect and track images.
 */
private fun ARSceneView.processFrame(
    frame: Frame,
    trackedImageOverlays: MutableMap<Int, Pair<String, ARVideoOverlay>>,
    videoOverlaysRef: MutableMap<String, ARVideoOverlay>,
    onImageDetected: (String) -> Unit,
    onImageLost: (String) -> Unit
) {
    // Get all tracked images in this frame
    val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
    
    if (updatedImages.isNotEmpty()) {
        Log.d(TAG, "Frame has ${updatedImages.size} updated image(s)")
    }
    
    updatedImages.forEach { augmentedImage ->
        val imageIndex = augmentedImage.index
        val imageName = augmentedImage.name ?: "unknown"
        
        when (augmentedImage.trackingState) {
            TrackingState.TRACKING -> {
                // Image is being tracked
                if (!trackedImageOverlays.containsKey(imageIndex)) {
                    // Newly detected image
                    Log.i(TAG, "========================================")
                    Log.i(TAG, "🎯 NEW IMAGE DETECTED!")
                    Log.i(TAG, "   Name: $imageName")
                    Log.i(TAG, "   Index: $imageIndex")
                    Log.i(TAG, "   Size: ${augmentedImage.extentX}m x ${augmentedImage.extentZ}m")
                    Log.i(TAG, "========================================")
                    
                    // Create video overlay for this image
                    Log.i(TAG, "Creating ARVideoOverlay...")
                    val overlay = ARVideoOverlay(context, augmentedImage)
                    
                    // Store the overlay
                    trackedImageOverlays[imageIndex] = Pair(imageName, overlay)
                    videoOverlaysRef[imageName] = overlay
                    
                    Log.i(TAG, "========================================")
                    Log.i(TAG, "✅✅✅ VIDEO OVERLAY CREATED AND STORED!")
                    Log.i(TAG, "   Image: $imageName")
                    Log.i(TAG, "   Overlay: $overlay")
                    Log.i(TAG, "   videoOverlaysRef size: ${videoOverlaysRef.size}")
                    Log.i(TAG, "========================================")
                    
                    onImageDetected(imageName)
                } else {
                    // Update overlay position for tracked image
                    // Note: Position update will be handled in a separate render loop
                    // For now, we'll keep the overlay at the last known position
                }
            }
            
            TrackingState.PAUSED -> {
                // Image tracking paused (temporarily lost)
                if (trackedImageOverlays.containsKey(imageIndex)) {
                    Log.d(TAG, "⏸️ Image tracking paused: $imageName")
                }
            }
            
            TrackingState.STOPPED -> {
                // Image tracking stopped (lost)
                trackedImageOverlays[imageIndex]?.let { (name, overlay) ->
                    Log.i(TAG, "🛑 Image tracking stopped: $name")
                    
                    // Clean up overlay
                    overlay.cleanup()
                    videoOverlaysRef.remove(name)
                    
                    trackedImageOverlays.remove(imageIndex)
                    onImageLost(name)
                }
            }
        }
    }
}

private const val TAG = "TalkARView"

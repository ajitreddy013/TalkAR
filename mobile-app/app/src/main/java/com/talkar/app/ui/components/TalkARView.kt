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
import com.talkar.app.ar.AugmentedImageDatabaseManager
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AugmentedImageNode

/**
 * Main AR view for TalkAR using Sceneview and ARCore Augmented Images.
 * 
 * This component:
 * - Displays AR camera feed
 * - Detects reference images (posters)
 * - Tracks images in 3D space
 * - Handles gesture interactions (long-press to play video)
 * - Provides callbacks for image detection/loss
 * 
 * @param modifier Compose modifier
 * @param onImageDetected Callback when an image is detected (imageName)
 * @param onImageLost Callback when an image is lost (imageName)
 * @param onImageLongPressed Callback when user long-presses on detected image (imageName)
 * @param onError Callback for errors
 */
@Composable
fun TalkARView(
    modifier: Modifier = Modifier,
    onImageDetected: (imageName: String) -> Unit = {},
    onImageLost: (imageName: String) -> Unit = {},
    onImageLongPressed: (imageName: String) -> Unit = {},
    onError: (error: String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // AR managers
    val dbManager = remember { AugmentedImageDatabaseManager(context) }
    val sessionConfig = remember { ARSessionConfig() }
    
    // Track detected images and their names
    var trackedImageNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var arSceneView by remember { mutableStateOf<ARSceneView?>(null) }
    
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
    onImageDetected: (String) -> Unit,
    onImageLost: (String) -> Unit,
    onError: (String) -> Unit,
    onViewCreated: (ARSceneView) -> Unit
): ARSceneView {
    
    return ARSceneView(context).apply {
        // Store currently tracked images
        val trackedImageNodes = mutableMapOf<Int, Pair<String, AugmentedImageNode>>()
        
        // Set up gesture handling
        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        
        // Configure AR session
        configureSession { session, config ->
            try {
                // Validate reference images exist
                val missingImages = dbManager.validateReferenceImages()
                if (missingImages.isNotEmpty()) {
                    val error = "Missing reference images: ${missingImages.joinToString()}"
                    Log.e(TAG, error)
                    onError(error)
                    return@configureSession
                }
                
                // Create augmented image database
                val database = dbManager.createDatabase(session)
                Log.i(TAG, "✅ Image database created with ${database.numImages} image(s)")
                
                // Configure session for image tracking
                val configured = sessionConfig.configure(session, database)
                if (!configured) {
                    onError("Failed to configure AR session")
                    return@configureSession
                }
                
                Log.i(TAG, "✅ AR session configured successfully")
                
            } catch (e: Exception) {
                val error = "AR setup failed: ${e.message}"
                Log.e(TAG, error, e)
                onError(error)
            }
        }
        
        // Handle each AR frame
        onSessionUpdated = { session, frame ->
            processFrame(
                frame = frame,
                trackedImageNodes = trackedImageNodes,
                onImageDetected = onImageDetected,
                onImageLost = onImageLost
            )
        }
        
        // Notify when view is created
        onViewCreated(this)
    }
}

/**
 * Processes each AR frame to detect and track images.
 */
private fun processFrame(
    frame: Frame,
    trackedImageNodes: MutableMap<Int, Pair<String, AugmentedImageNode>>,
    onImageDetected: (String) -> Unit,
    onImageLost: (String) -> Unit
) {
    // Get all tracked images in this frame
    val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
    
    updatedImages.forEach { augmentedImage ->
        val imageIndex = augmentedImage.index
        val imageName = augmentedImage.name ?: "unknown"
        
        when (augmentedImage.trackingState) {
            TrackingState.TRACKING -> {
                // Image is being tracked
                if (!trackedImageNodes.containsKey(imageIndex)) {
                    // Newly detected image
                    Log.d(TAG, "🎯 New image detected: $imageName (index: $imageIndex)")
                    
                    // Create anchor for this image
                    val anchor = augmentedImage.createAnchorOrNull(augmentedImage.centerPose)
                    if (anchor != null) {
                        // TODO: Create AugmentedImageNode when we implement VideoAnchorNode
                        // For now, just track that we detected it
                        // trackedImageNodes[imageIndex] = imageName to node
                        
                        onImageDetected(imageName)
                    } else {
                        Log.w(TAG, "⚠️ Failed to create anchor for image: $imageName")
                    }
                }
                // Image continues to be tracked (no action needed)
            }
            
            TrackingState.PAUSED -> {
                // Image tracking paused (temporarily lost)
                if (trackedImageNodes.containsKey(imageIndex)) {
                    Log.d(TAG, "⏸️ Image tracking paused: $imageName")
                }
            }
            
            TrackingState.STOPPED -> {
                // Image tracking stopped (lost)
                if (trackedImageNodes.containsKey(imageIndex)) {
                    Log.d(TAG, "🛑 Image tracking stopped: $imageName")
                    trackedImageNodes.remove(imageIndex)
                    onImageLost(imageName)
                }
            }
        }
    }
}

private const val TAG = "TalkARView"

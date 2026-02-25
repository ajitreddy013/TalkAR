package com.talkar.app.ui.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.*
import com.talkar.app.ar.video.models.TrackingData
import com.talkar.app.ar.video.tracking.ARTrackingManager
import com.talkar.app.ar.video.tracking.ARTrackingManagerImpl
import com.talkar.app.ar.video.tracking.ReferencePoster
import com.talkar.app.ar.video.tracking.TrackingListener
import com.talkar.app.ar.video.tracking.TrackedPoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope

/**
 * Composable wrapper for AR Camera View with poster detection.
 * 
 * Integrates ARTrackingManager for poster detection and tracking.
 * 
 * Requirements: 1.1, 1.4, 6.1, 6.2
 */
@Composable
fun ArSceneViewComposable(
    modifier: Modifier = Modifier,
    onPosterDetected: (posterId: String, anchor: com.google.ar.core.Anchor) -> Unit = { _, _ -> },
    onPosterLost: (posterId: String) -> Unit = {},
    onTrackingUpdate: (trackingData: TrackingData) -> Unit = {},
    onError: (errorMessage: String) -> Unit = {}
) {
    val context = LocalContext.current
    
    var arTrackingManager by remember { mutableStateOf<ARTrackingManager?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }
    var isInitializing by remember { mutableStateOf(false) }
    var frameProcessingStarted by remember { mutableStateOf(false) }
    
    // Initialize ARCore session and tracking manager on background thread
    LaunchedEffect(Unit) {
        if (isInitializing) return@LaunchedEffect
        isInitializing = true
        
        withContext(Dispatchers.IO) {
            try {
                Log.d("ArSceneView", "Initializing ARCore session...")
                
                // Create ARCore session
                val arSession = Session(context)
                session = arSession
                
                // Configure session for augmented images
                val config = Config(arSession).apply {
                    focusMode = Config.FocusMode.AUTO
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                arSession.configure(config)
                
                // Create tracking manager
                val trackingManager = ARTrackingManagerImpl(context, arSession)
                arTrackingManager = trackingManager
                
                // Set up tracking listener
                trackingManager.setListener(object : TrackingListener {
                    override fun onPosterDetected(poster: TrackedPoster) {
                        Log.d("ArSceneView", "Poster detected: ${poster.id}")
                        onPosterDetected(poster.id, poster.anchor)
                    }
                    
                    override fun onPosterTracking(poster: TrackedPoster) {
                        val pose = poster.anchor.pose
                        val trackingData = TrackingData(
                            position = com.talkar.app.ar.video.models.Vector3(
                                pose.tx(),
                                pose.ty(),
                                pose.tz()
                            ),
                            rotation = com.talkar.app.ar.video.models.Quaternion(
                                pose.qx(),
                                pose.qy(),
                                pose.qz(),
                                pose.qw()
                            ),
                            scale = com.talkar.app.ar.video.models.Vector2(
                                poster.extentX,
                                poster.extentZ
                            ),
                            isTracking = true,
                            timestamp = System.currentTimeMillis()
                        )
                        onTrackingUpdate(trackingData)
                    }
                    
                    override fun onPosterLost(posterId: String) {
                        Log.d("ArSceneView", "Poster lost: $posterId")
                        onPosterLost(posterId)
                    }
                    
                    override fun onDetectionTimeout() {
                        Log.w("ArSceneView", "Poster detection timeout")
                        onError("No poster detected. Please point camera at a poster with a human face.")
                    }
                })
                
                // Load reference posters from backend
                Log.d("ArSceneView", "Loading posters from backend...")
                val posterRepository = com.talkar.app.data.repository.PosterRepository(
                    apiClient = com.talkar.app.TalkARApplication.instance.apiClient,
                    imageRepository = com.talkar.app.TalkARApplication.instance.imageRepository,
                    context = context
                )
                
                val postersResult = posterRepository.loadPosters()
                val posters: List<ReferencePoster> = postersResult.getOrElse {
                    Log.e("ArSceneView", "Failed to load posters from backend", it)
                    // Try to load test poster as fallback
                    val testPosterResult = posterRepository.loadTestPoster()
                    val testPoster = testPosterResult.getOrNull()
                    if (testPoster != null) {
                        listOf(testPoster)
                    } else {
                        Log.e("ArSceneView", "Failed to load test poster", it)
                        emptyList()
                    }
                }
                
                if (posters.isEmpty()) {
                    Log.w("ArSceneView", "⚠️ No posters loaded - detection will not work")
                    withContext(Dispatchers.Main) {
                        onError("No posters available. Please add posters to the backend.")
                    }
                } else {
                    Log.d("ArSceneView", "Initializing tracking with ${posters.size} posters")
                    val initResult = trackingManager.initialize(posters)
                    
                    if (initResult.isSuccess) {
                        // CRITICAL: Update ARCore session config with the image database
                        // This was missing - causing augmented_image_database: <null> in logs
                        try {
                            val sessionConfig = arSession.config
                            Log.d("ArSceneView", "Updating ARCore config with image database")
                            sessionConfig.augmentedImageDatabase = createImageDatabase(arSession, posters)
                            arSession.configure(sessionConfig)
                            Log.d("ArSceneView", "✅ ARCore config updated with ${posters.size} poster images")
                        } catch (e: Exception) {
                            Log.e("ArSceneView", "Failed to update ARCore config", e)
                        }
                        
                        Log.d("ArSceneView", "✅ ARCore initialized successfully with ${posters.size} posters")
                    } else {
                        Log.e("ArSceneView", "Failed to initialize tracking manager")
                        withContext(Dispatchers.Main) {
                            onError("Failed to initialize AR tracking")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("ArSceneView", "❌ Failed to initialize ARCore", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
            } finally {
                isInitializing = false
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d("ArSceneView", "Disposing ARCore resources")
            arTrackingManager?.release()
            session?.close()
        }
    }
    
    // Render AR camera view
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            createARCameraView(
                context = ctx,
                onError = onError
            )
        },
        update = { view ->
            // Update view when session and tracking manager are ready
            // Only start once to avoid multiple coroutines
            if (session != null && arTrackingManager != null && !isInitializing && !frameProcessingStarted) {
                Log.d("ArSceneView", "AR session ready, starting frame processing")
                frameProcessingStarted = true
                startFrameProcessing(view, session, arTrackingManager, onError)
            }
        }
    )
}

/**
 * Create ARCore augmented image database from posters.
 */
private fun createImageDatabase(
    session: Session,
    posters: List<ReferencePoster>
): AugmentedImageDatabase {
    val database = AugmentedImageDatabase(session)
    
    posters.forEach { poster ->
        try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                poster.imageData,
                0,
                poster.imageData.size
            )
            
            if (bitmap != null) {
                database.addImage(
                    poster.name,
                    bitmap,
                    poster.physicalWidthMeters
                )
                Log.d("ArSceneView", "Added poster to database: ${poster.name}")
            } else {
                Log.w("ArSceneView", "Failed to decode bitmap for poster: ${poster.name}")
            }
        } catch (e: Exception) {
            Log.e("ArSceneView", "Failed to add poster to database: ${poster.name}", e)
        }
    }
    
    return database
}

/**
 * Creates the AR camera view with ARCore rendering.
 */
private fun createARCameraView(
    context: Context,
    onError: (String) -> Unit
): android.view.View {
    val layout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
    }
    
    // Create SurfaceView for AR camera preview
    val surfaceView = android.view.SurfaceView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
    
    layout.addView(surfaceView)
    
    // Store surface view in layout tag for later access
    layout.tag = surfaceView
    
    return layout
}

/**
 * Start processing ARCore frames for tracking.
 * Called from AndroidView update block when session is ready.
 */
private fun startFrameProcessing(
    view: android.view.View,
    session: Session?,
    trackingManager: ARTrackingManager?,
    onError: (String) -> Unit
) {
    if (session == null || trackingManager == null) {
        Log.w("ArSceneView", "Cannot start frame processing - session or tracking manager is null")
        return
    }
    
    // Get the surface view from layout
    val layout = view as? FrameLayout ?: return
    val surfaceView = layout.tag as? android.view.SurfaceView ?: return
    
    Log.d("ArSceneView", "Starting frame processing with session and tracking manager")
    
    // Set up surface callback to configure ARCore session
    surfaceView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
            Log.d("ArSceneView", "Surface created")
            
            // Configure ARCore to render to this surface
            try {
                // Set the surface for ARCore to render camera feed
                session.setCameraTextureName(0)
                Log.d("ArSceneView", "✅ ARCore camera surface configured")
            } catch (e: Exception) {
                Log.e("ArSceneView", "Failed to configure camera surface", e)
                onError("Failed to configure camera: ${e.message}")
            }
        }
        
        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
            Log.d("ArSceneView", "Surface changed: ${width}x${height}")
            session.setDisplayGeometry(
                surfaceView.context.resources.configuration.orientation,
                width,
                height
            )
        }
        
        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
            Log.d("ArSceneView", "Surface destroyed")
        }
    })
    
    // Start frame processing loop
    CoroutineScope(Dispatchers.Default).launch {
        Log.d("ArSceneView", "Frame processing loop started")
        while (true) {
            try {
                // Update ARCore frame
                val frame = session.update()
                
                // Process frame for poster detection/tracking
                trackingManager.processFrame(frame)
                
                // Sleep to maintain ~60fps
                delay(16)
                
            } catch (e: com.google.ar.core.exceptions.CameraNotAvailableException) {
                Log.e("ArSceneView", "Camera not available", e)
                onError("Camera not available")
                break
            } catch (e: Exception) {
                Log.e("ArSceneView", "Error processing frame", e)
            }
        }
    }
}

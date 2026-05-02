package com.talkar.app.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.core.ArCoreApk
import com.talkar.app.ar.video.models.TrackingData
import com.talkar.app.ar.video.tracking.ARTrackingManager
import com.talkar.app.ar.video.tracking.ARTrackingManagerImpl
import com.talkar.app.ar.video.tracking.ReferencePoster
import com.talkar.app.ar.video.tracking.TrackingListener
import com.talkar.app.ar.video.tracking.TrackedPoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

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
    onPreviewReady: () -> Unit = {},
    onTrackingReady: () -> Unit = {},
    onPosterDetected: (posterId: String, anchor: com.google.ar.core.Anchor) -> Unit = { _, _ -> },
    onPosterLost: (posterId: String) -> Unit = {},
    onTrackingUpdate: (trackingData: TrackingData) -> Unit = {},
    onError: (errorMessage: String) -> Unit = {}
) {
    // Log composable entry
    Log.d("ArSceneView", "🎬 ArSceneViewComposable entered")
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var arTrackingManager by remember { mutableStateOf<ARTrackingManager?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }
    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }
    var isInitializing by remember { mutableStateOf(false) }
    var frameProcessingStarted by remember { mutableStateOf(false) }
    var isSessionRunning by remember { mutableStateOf(false) }
    var isResumingSession by remember { mutableStateOf(false) }
    var useBasicCameraFallback by remember { mutableStateOf(false) }
    var currentlyTrackedPosterId by remember { mutableStateOf<String?>(null) }
    var posterRepositoryRef by remember { mutableStateOf<com.talkar.app.data.repository.PosterRepository?>(null) }
    val latestViewMatrix = remember { FloatArray(16) { if (it % 5 == 0) 1f else 0f } }
    val latestProjectionMatrix = remember { FloatArray(16) { if (it % 5 == 0) 1f else 0f } }
    
    Log.d("ArSceneView", "📊 State: isInitializing=$isInitializing, session=${session != null}, frameProcessingStarted=$frameProcessingStarted")
    
    fun resumeSessionSafely(source: String) {
        val currentSession = session ?: return
        if (isSessionRunning || isResumingSession) return
        isResumingSession = true
        try {
            currentSession.resume()
            isSessionRunning = true
            Log.d("ArSceneView", "AR session resumed ($source)")
        } catch (e: SessionNotPausedException) {
            isSessionRunning = true
            Log.d("ArSceneView", "AR session already running ($source)")
        } catch (e: Exception) {
            Log.e("ArSceneView", "Failed to resume AR session ($source)", e)
            if (e is FatalException) {
                Log.e("ArSceneView", "Switching to CameraX fallback preview due to ARCore fatal resume failure")
                useBasicCameraFallback = true
            }
        } finally {
            isResumingSession = false
        }
    }

    // Lifecycle management for AR session
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("ArSceneView", "Lifecycle resumed")
                    glSurfaceViewRef?.onResume()
                    resumeSessionSafely("lifecycle")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("ArSceneView", "Lifecycle paused")
                    glSurfaceViewRef?.onPause()
                    val currentSession = session
                    if (currentSession != null && isSessionRunning) {
                        try {
                            currentSession.pause()
                            isSessionRunning = false
                            Log.d("ArSceneView", "AR session paused")
                        } catch (e: Exception) {
                            Log.e("ArSceneView", "Failed to pause AR session", e)
                        }
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Resume once after session becomes available while already in foreground.
    LaunchedEffect(session) {
        if (session != null && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            // Small delay avoids resume races during first composition/observer attachment.
            delay(120)
            resumeSessionSafely("session-ready")
        }
    }
    
    // Initialize ARCore session and tracking manager on background thread
    LaunchedEffect(Unit) {
        if (isInitializing) return@LaunchedEffect
        isInitializing = true
        
        withContext(Dispatchers.IO) {
            try {
                Log.d("ArSceneView", "Checking ARCore availability...")
                
                // Check if ARCore is supported and up to date
                val availability = ArCoreApk.getInstance().checkAvailability(context)
                when (availability) {
                    ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                        Log.d("ArSceneView", "✅ ARCore supported and installed")
                    }
                    ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                    ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                        Log.w("ArSceneView", "⚠️ ARCore needs update or installation")
                        withContext(Dispatchers.Main) {
                            onError("ARCore needs to be updated. Please update from Play Store.")
                        }
                        return@withContext
                    }
                    else -> {
                        Log.e("ArSceneView", "❌ ARCore not supported on this device")
                        withContext(Dispatchers.Main) {
                            onError("ARCore is not supported on this device.")
                        }
                        return@withContext
                    }
                }
                
                Log.d("ArSceneView", "Creating ARCore session...")
                
                // Create ARCore session with comprehensive error handling
                val arSession = try {
                    Session(context)
                } catch (e: UnavailableArcoreNotInstalledException) {
                    Log.e("ArSceneView", "❌ ARCore not installed", e)
                    withContext(Dispatchers.Main) {
                        onError("ARCore is not installed. Please install from Play Store.")
                    }
                    return@withContext
                } catch (e: UnavailableApkTooOldException) {
                    Log.e("ArSceneView", "❌ ARCore APK too old", e)
                    withContext(Dispatchers.Main) {
                        onError("ARCore is outdated. Please update from Play Store.")
                    }
                    return@withContext
                } catch (e: UnavailableSdkTooOldException) {
                    Log.e("ArSceneView", "❌ Android SDK too old", e)
                    withContext(Dispatchers.Main) {
                        onError("Your Android version is too old for ARCore.")
                    }
                    return@withContext
                } catch (e: UnavailableDeviceNotCompatibleException) {
                    Log.e("ArSceneView", "❌ Device not compatible with ARCore", e)
                    withContext(Dispatchers.Main) {
                        onError("Your device is not compatible with ARCore.")
                    }
                    return@withContext
                } catch (e: IllegalArgumentException) {
                    // Handle vendor-specific camera tag errors (e.g., OPPO tags on non-OPPO devices)
                    if (e.message?.contains("Could not find tag") == true) {
                        Log.w("ArSceneView", "⚠️ Vendor-specific camera tag not found (this is normal on non-vendor devices)")
                        Log.w("ArSceneView", "Continuing with standard ARCore features...")
                        try {
                            Session(context) // Try again, ARCore should continue despite the warning
                        } catch (retryException: Exception) {
                            Log.e("ArSceneView", "❌ Failed to create session on retry", retryException)
                            withContext(Dispatchers.Main) {
                                onError("Failed to initialize AR camera: ${retryException.message}")
                            }
                            return@withContext
                        }
                    } else {
                        Log.e("ArSceneView", "❌ Illegal argument creating session", e)
                        throw e
                    }
                } catch (e: Exception) {
                    Log.e("ArSceneView", "❌ Unexpected error creating ARCore session", e)
                    withContext(Dispatchers.Main) {
                        onError("Failed to initialize AR: ${e.message}")
                    }
                    return@withContext
                }
                
                Log.d("ArSceneView", "✅ ARCore session created successfully")
                
                // Configure session optimized for image tracking
                val config = Config(arSession).apply {
                    focusMode = Config.FocusMode.AUTO
                    
                    // Use BLOCKING mode to reduce frame skipping and timing issues
                    // This ensures we process every frame in order, preventing timestamp issues
                    updateMode = Config.UpdateMode.BLOCKING
                    
                    // Disable features that might cause performance issues
                    planeFindingMode = Config.PlaneFindingMode.DISABLED
                    lightEstimationMode = Config.LightEstimationMode.DISABLED
                    depthMode = Config.DepthMode.DISABLED
                    
                    // Enable instant placement mode for faster tracking
                    instantPlacementMode = Config.InstantPlacementMode.DISABLED
                    
                    Log.d("ArSceneView", "Configured ARCore with BLOCKING mode for stable tracking")
                }
                
                // Check if configuration is supported
                if (!arSession.isSupported(config)) {
                    Log.e("ArSceneView", "❌ ARCore configuration not supported on this device")
                    withContext(Dispatchers.Main) {
                        onError("AR configuration not supported on this device.")
                    }
                    arSession.close()
                    return@withContext
                }
                
                arSession.configure(config)
                Log.d("ArSceneView", "✅ ARCore session configured successfully")
                
                // Publish session/tracking manager on main thread for Compose state consistency.
                withContext(Dispatchers.Main) {
                    session = arSession
                }
                
                // Create tracking manager
                val trackingManager = ARTrackingManagerImpl(context, arSession)
                withContext(Dispatchers.Main) {
                    arTrackingManager = trackingManager
                }

                // Set up tracking listener
                trackingManager.setListener(object : TrackingListener {
                    override fun onPosterDetected(poster: TrackedPoster) {
                        Log.d("ArSceneView", "Poster detected: ${poster.id}")
                        currentlyTrackedPosterId = poster.id
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
                            timestamp = System.currentTimeMillis(),
                            viewMatrix = latestViewMatrix.copyOf(),
                            projectionMatrix = latestProjectionMatrix.copyOf()
                        )
                        onTrackingUpdate(trackingData)
                    }
                    
                    override fun onPosterLost(posterId: String) {
                        Log.d("ArSceneView", "Poster lost: $posterId")
                        if (currentlyTrackedPosterId == posterId) {
                            currentlyTrackedPosterId = null
                        }
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
                withContext(Dispatchers.Main) {
                    posterRepositoryRef = posterRepository
                }
                
                // Try to load posters with timeout
                val postersResult = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeout(10000) { // 10 second timeout
                            posterRepository.loadPosters()
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Log.w("ArSceneView", "Poster loading timed out; using bundled fallback posters")
                        val bundledFallback = posterRepository.loadBundledFallbackPosters()
                        if (bundledFallback.isSuccess) {
                            bundledFallback
                        } else {
                            Log.w("ArSceneView", "Bundled fallback unavailable after timeout; using test poster")
                            posterRepository.loadTestPoster().map { listOf(it) }
                        }
                    }
                }
                
                val posters: List<ReferencePoster> = postersResult.getOrElse {
                    Log.e("ArSceneView", "Failed to load posters from backend", it)
                    // Try to load test poster as fallback
                    val testPosterResult = posterRepository.loadTestPoster()
                    val testPoster = testPosterResult.getOrNull()
                    if (testPoster != null) {
                        Log.d("ArSceneView", "Using test poster as fallback")
                        listOf(testPoster)
                    } else {
                        Log.e("ArSceneView", "Failed to load test poster", it)
                        emptyList()
                    }
                }
                
                if (posters.isEmpty()) {
                    Log.w("ArSceneView", "⚠️ No posters loaded - detection will not work")
                    Log.w("ArSceneView", "Camera will still be visible for testing")
                } else {
                    Log.d("ArSceneView", "Initializing tracking with ${posters.size} posters")
                    val initResult = trackingManager.initialize(posters)
                    
                    if (initResult.isSuccess) {
                        // Update ARCore session config with the image database
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
                        withContext(Dispatchers.Main) { onTrackingReady() }
                    } else {
                        Log.e("ArSceneView", "Failed to initialize tracking manager with current poster set")
                        Log.d("ArSceneView", "Retrying tracking initialization with test poster fallback")
                        val testPoster = posterRepository.loadTestPoster().getOrNull()
                        if (testPoster != null) {
                            val retryResult = trackingManager.initialize(listOf(testPoster))
                            if (retryResult.isSuccess) {
                                try {
                                    val sessionConfig = arSession.config
                                    sessionConfig.augmentedImageDatabase = createImageDatabase(arSession, listOf(testPoster))
                                    arSession.configure(sessionConfig)
                                    Log.d("ArSceneView", "✅ ARCore initialized with test poster fallback")
                                    withContext(Dispatchers.Main) { onTrackingReady() }
                                } catch (e: Exception) {
                                    Log.e("ArSceneView", "Failed to update ARCore config for test poster fallback", e)
                                }
                            } else {
                                Log.e("ArSceneView", "Test poster fallback initialization failed")
                            }
                        } else {
                            Log.e("ArSceneView", "Test poster fallback unavailable")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e("ArSceneView", "❌ Failed to initialize ARCore", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isInitializing = false
                }
            }
        }
    }

    // Periodic backend poster index refresh while AR screen is active.
    // To avoid disrupting active playback/tracking, only reconfigure when no poster is currently tracked.
    LaunchedEffect(session, arTrackingManager, currentlyTrackedPosterId) {
        while (isActive) {
            delay(120_000) // 2 minutes
            val arSession = session ?: continue
            val trackingManager = arTrackingManager ?: continue
            val posterRepository = posterRepositoryRef ?: continue

            if (currentlyTrackedPosterId != null) {
                Log.d("ArSceneView", "Skipping periodic poster refresh; poster is currently tracked")
                continue
            }

            try {
                val refreshedPosters = withContext(Dispatchers.IO) {
                    posterRepository.loadPosters().getOrElse { emptyList() }
                }
                if (refreshedPosters.isEmpty()) {
                    Log.w("ArSceneView", "Periodic poster refresh returned empty list; keeping existing database")
                    continue
                }

                val initResult = withContext(Dispatchers.IO) {
                    trackingManager.initialize(refreshedPosters)
                }
                if (initResult.isSuccess) {
                    try {
                        val cfg = arSession.config
                        cfg.augmentedImageDatabase = createImageDatabase(arSession, refreshedPosters)
                        arSession.configure(cfg)
                        Log.d("ArSceneView", "Periodic poster refresh applied: ${refreshedPosters.size} posters")
                    } catch (e: Exception) {
                        Log.e("ArSceneView", "Failed applying periodic poster refresh config", e)
                    }
                } else {
                    Log.w("ArSceneView", "Periodic poster refresh initialize failed; existing config preserved")
                }
            } catch (e: Exception) {
                Log.e("ArSceneView", "Periodic poster refresh failed", e)
            }
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d("ArSceneView", "Disposing ARCore resources")
            try {
                if (isSessionRunning) {
                    session?.pause()
                    isSessionRunning = false
                }
            } catch (e: Exception) {
                Log.e("ArSceneView", "Error pausing session on dispose", e)
            }
            glSurfaceViewRef?.onPause()
            arTrackingManager?.release()
            session?.close()
        }
    }
    
    if (useBasicCameraFallback) {
        BasicCameraPreview(
            modifier = modifier,
            lifecycleOwner = lifecycleOwner,
            onPreviewReady = onPreviewReady,
            onError = onError
        )
        return
    }

    // Render AR camera view
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val layout = createARCameraView(
                context = ctx
            )
            val tagPair = layout.tag as? Pair<*, *>
            glSurfaceViewRef = tagPair?.first as? GLSurfaceView
            
            // Note: Renderer will be set later in setupCameraRenderer() when ARCore session is ready
            // We don't set it here to avoid IllegalStateException from calling setRenderer() twice
            
            layout
        },
        update = { view ->
            if (session != null) {
                onPreviewReady()
            }
            // Start camera renderer as soon as AR session is available.
            // Do not block preview on poster-loading/tracking-manager readiness.
            if (session != null && !frameProcessingStarted) {
                Log.d("ArSceneView", "AR session ready, setting up camera renderer")
                frameProcessingStarted = true
                
                // Set up the real camera renderer now that session is ready
                setupCameraRenderer(
                    view = view,
                    session = session!!,
                    trackingManagerProvider = { arTrackingManager },
                    isSessionRunningProvider = { isSessionRunning },
                    latestViewMatrix = latestViewMatrix,
                    latestProjectionMatrix = latestProjectionMatrix,
                    lifecycleOwner = lifecycleOwner,
                    onError = onError
                )
            }
        }
    )
}

@Composable
private fun BasicCameraPreview(
    modifier: Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onPreviewReady: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                    onPreviewReady()
                    Log.d("ArSceneView", "✅ CameraX fallback preview started")
                } catch (e: Exception) {
                    Log.e("ArSceneView", "Failed to start CameraX fallback preview", e)
                    onError("Camera preview unavailable: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
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
    
    Log.d("ArSceneView", "📸 Creating ARCore image database with ${posters.size} posters")
    
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
                Log.d("ArSceneView", "  ✅ Added poster to database: ${poster.name} (${bitmap.width}x${bitmap.height})")
            } else {
                Log.w("ArSceneView", "  ❌ Failed to decode bitmap for poster: ${poster.name}")
            }
        } catch (e: Exception) {
            Log.e("ArSceneView", "  ❌ Failed to add poster to database: ${poster.name}", e)
        }
    }
    
    Log.d("ArSceneView", "✅ Image database created with ${database.numImages} images")
    return database
}

/**
 * Delegating renderer that switches from placeholder to camera rendering.
 * This avoids calling setRenderer() twice which causes IllegalStateException.
 */
private class DelegatingRenderer : android.opengl.GLSurfaceView.Renderer {
    @Volatile
    private var delegateRenderer: android.opengl.GLSurfaceView.Renderer? = null
    
    private var surfaceCreatedCalled = false
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastGl: javax.microedition.khronos.opengles.GL10? = null
    private var lastConfig: javax.microedition.khronos.egl.EGLConfig? = null
    
    fun updateDelegate(newDelegate: android.opengl.GLSurfaceView.Renderer) {
        delegateRenderer = newDelegate
    }
    
    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        surfaceCreatedCalled = true
        lastGl = gl
        lastConfig = config
        
        delegateRenderer?.onSurfaceCreated(gl, config) ?: run {
            // Placeholder rendering
            android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            Log.d("ArSceneView", "Placeholder renderer: Surface created")
        }
    }
    
    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        lastWidth = width
        lastHeight = height
        lastGl = gl
        
        delegateRenderer?.onSurfaceChanged(gl, width, height) ?: run {
            // Placeholder rendering
            android.opengl.GLES20.glViewport(0, 0, width, height)
            Log.d("ArSceneView", "Placeholder renderer: Surface changed ${width}x${height}")
        }
    }
    
    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        delegateRenderer?.onDrawFrame(gl) ?: run {
            // Placeholder rendering
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
        }
    }
}

/**
 * Creates the AR camera view with ARCore rendering.
 * Uses a delegating renderer that can switch from placeholder to camera rendering.
 */
private fun createARCameraView(
    context: Context
): android.view.View {
    val layout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
    }
    
    // Create delegating renderer that we can reconfigure later
    val delegatingRenderer = DelegatingRenderer()
    
    // Create GLSurfaceView for AR camera preview with OpenGL rendering
    val glSurfaceView = android.opengl.GLSurfaceView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2) // OpenGL ES 2.0
        setEGLConfigChooser(8, 8, 8, 8, 16, 0) // RGBA_8888, 16-bit depth
        
        // Set the delegating renderer (only called once!)
        setRenderer(delegatingRenderer)
        
        // Use CONTINUOUSLY mode for AR tracking
        renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
    
    layout.addView(glSurfaceView)
    
    // Store both GL surface view and delegating renderer in layout tag for later access
    layout.tag = Pair(glSurfaceView, delegatingRenderer)
    
    Log.d("ArSceneView", "✅ GLSurfaceView created with delegating renderer")
    return layout
}

/**
 * Set up camera renderer for ARCore frames.
 * Called from AndroidView update block when session is ready.
 * Updates the delegating renderer to use camera rendering instead of placeholder.
 */
private fun setupCameraRenderer(
    view: android.view.View,
    session: Session,
    trackingManagerProvider: () -> ARTrackingManager?,
    isSessionRunningProvider: () -> Boolean,
    latestViewMatrix: FloatArray,
    latestProjectionMatrix: FloatArray,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onError: (String) -> Unit
) {
    // Get the GL surface view and delegating renderer from layout
    val layout = view as? FrameLayout ?: return
    val tagPair = layout.tag as? Pair<*, *> ?: return
    val glSurfaceView = tagPair.first as? android.opengl.GLSurfaceView ?: return
    val delegatingRenderer = tagPair.second as? DelegatingRenderer ?: return
    
    Log.d("ArSceneView", "Setting up camera renderer with ARCore session")
    
    // Create a renderer for ARCore camera feed with background rendering
    val cameraRenderer = object : android.opengl.GLSurfaceView.Renderer {
        private var cameraTextureConfigured = false
        private var cameraTextureId = -1
        private var shaderProgram = 0
        private var positionAttrib = 0
        private var texCoordAttrib = 0
        private var textureUniform = 0
        private val quadCoords = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
        private val quadTexCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )
        private val transformedTexCoords = FloatArray(8)
        private val vertexBuffer = java.nio.ByteBuffer.allocateDirect(quadCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(quadCoords)
                position(0)
            }
        private val inputUvBuffer = java.nio.ByteBuffer.allocateDirect(quadTexCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(quadTexCoords)
                position(0)
            }
        private val outputUvBuffer = java.nio.ByteBuffer.allocateDirect(transformedTexCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        private val texCoordBuffer = java.nio.ByteBuffer.allocateDirect(transformedTexCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        
        // Vertex shader for camera background
        private val vertexShaderCode = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()
        
        // Fragment shader for external OES texture (ARCore camera)
        private val fragmentShaderCode = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """.trimIndent()
        
        override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
            Log.d("ArSceneView", "Camera renderer: GL Surface created")
            try {
                // Set clear color
                android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                
                // Generate texture for camera
                val textures = IntArray(1)
                android.opengl.GLES20.glGenTextures(1, textures, 0)
                cameraTextureId = textures[0]
                
                // Bind and configure texture
                android.opengl.GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
                android.opengl.GLES20.glTexParameteri(
                    android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    android.opengl.GLES20.GL_TEXTURE_MIN_FILTER,
                    android.opengl.GLES20.GL_LINEAR
                )
                android.opengl.GLES20.glTexParameteri(
                    android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    android.opengl.GLES20.GL_TEXTURE_MAG_FILTER,
                    android.opengl.GLES20.GL_LINEAR
                )
                
                // Configure ARCore to use the camera texture
                if (!cameraTextureConfigured && cameraTextureId != -1) {
                    session.setCameraTextureName(cameraTextureId)
                    cameraTextureConfigured = true
                    Log.d("ArSceneView", "✅ ARCore camera texture configured: $cameraTextureId")
                }
                
                // Compile shaders
                val vertexShader = loadShader(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShaderCode)
                val fragmentShader = loadShader(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
                
                // Create shader program
                shaderProgram = android.opengl.GLES20.glCreateProgram()
                android.opengl.GLES20.glAttachShader(shaderProgram, vertexShader)
                android.opengl.GLES20.glAttachShader(shaderProgram, fragmentShader)
                android.opengl.GLES20.glLinkProgram(shaderProgram)
                
                // Get attribute and uniform locations
                positionAttrib = android.opengl.GLES20.glGetAttribLocation(shaderProgram, "a_Position")
                texCoordAttrib = android.opengl.GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord")
                textureUniform = android.opengl.GLES20.glGetUniformLocation(shaderProgram, "u_Texture")
                
                Log.d("ArSceneView", "✅ Camera renderer initialized with shaders")
            } catch (e: Exception) {
                Log.e("ArSceneView", "Failed to initialize camera renderer", e)
            }
        }
        
        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = android.opengl.GLES20.glCreateShader(type)
            android.opengl.GLES20.glShaderSource(shader, shaderCode)
            android.opengl.GLES20.glCompileShader(shader)
            return shader
        }
        
        override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
            Log.d("ArSceneView", "Camera renderer: GL Surface changed: ${width}x${height}")
            android.opengl.GLES20.glViewport(0, 0, width, height)
            
            try {
                session.setDisplayGeometry(
                    glSurfaceView.context.resources.configuration.orientation,
                    width,
                    height
                )
            } catch (e: Exception) {
                Log.e("ArSceneView", "Error setting display geometry", e)
            }
        }
        
        override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
            // Clear screen
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)
            
            try {
                // Check lifecycle state
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    return
                }

                // Check ARCore session state
                try {
                    session.cameraConfig
                } catch (e: Exception) {
                    return
                }
                
                // Update ARCore frame with error handling for timestamp issues
                val frame = try {
                    if (!isSessionRunningProvider()) {
                        return
                    }
                    session.update()
                } catch (e: SessionPausedException) {
                    // Session paused between lifecycle transitions; skip this frame safely.
                    return
                } catch (e: IllegalArgumentException) {
                    // Handle sensor timestamp errors gracefully
                    if (e.message?.contains("timestamp") == true || e.message?.contains("monotonic") == true) {
                        // Skip this frame and continue - ARCore will recover
                        return
                    }
                    throw e
                } catch (e: Exception) {
                    // Handle resource exhaustion
                    if (e.message?.contains("RESOURCE_EXHAUSTED") == true) {
                        // Skip this frame - system is overloaded
                        return
                    }
                    throw e
                }
                
                val camera = frame.camera
                
                // Always draw camera background; tracking may be PAUSED initially.
                drawCameraBackground(frame)

                // Process poster tracking only when camera tracking is active.
                if (camera.trackingState == TrackingState.TRACKING) {
                    camera.getViewMatrix(latestViewMatrix, 0)
                    camera.getProjectionMatrix(latestProjectionMatrix, 0, 0.1f, 100f)
                    trackingManagerProvider()?.processFrame(frame)
                }
                
            } catch (e: Exception) {
                val isSessionPaused = e is SessionPausedException || 
                                    e.javaClass.simpleName.contains("SessionPaused") || 
                                    e.message?.contains("SessionPaused") == true
                
                if (!isSessionPaused && e !is CameraNotAvailableException) {
                    Log.e("ArSceneView", "Error in draw frame: ${e.message}")
                }
            }
        }
        
        private fun drawCameraBackground(frame: Frame) {
            if (cameraTextureId == -1 || shaderProgram == 0) return
            
            try {
                // Use shader program
                android.opengl.GLES20.glUseProgram(shaderProgram)
                
                // Bind camera texture
                android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE0)
                android.opengl.GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
                android.opengl.GLES20.glUniform1i(textureUniform, 0)
                
                // Transform texture coordinates for ARCore (correct buffer flow).
                inputUvBuffer.position(0)
                outputUvBuffer.position(0)
                frame.transformDisplayUvCoords(
                    inputUvBuffer,
                    outputUvBuffer
                )
                outputUvBuffer.position(0)
                outputUvBuffer.get(transformedTexCoords)
                vertexBuffer.position(0)
                
                android.opengl.GLES20.glVertexAttribPointer(
                    positionAttrib, 2, android.opengl.GLES20.GL_FLOAT, false, 0, vertexBuffer
                )
                android.opengl.GLES20.glEnableVertexAttribArray(positionAttrib)
                
                // Set texture coordinates
                texCoordBuffer.position(0)
                texCoordBuffer.put(transformedTexCoords)
                texCoordBuffer.position(0)
                
                android.opengl.GLES20.glVertexAttribPointer(
                    texCoordAttrib, 2, android.opengl.GLES20.GL_FLOAT, false, 0, texCoordBuffer
                )
                android.opengl.GLES20.glEnableVertexAttribArray(texCoordAttrib)
                
                // Draw quad
                android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4)
                
                // Cleanup
                android.opengl.GLES20.glDisableVertexAttribArray(positionAttrib)
                android.opengl.GLES20.glDisableVertexAttribArray(texCoordAttrib)
                
            } catch (e: Exception) {
                Log.e("ArSceneView", "Error drawing camera background", e)
            }
        }
    }
    
    // Update the delegating renderer to use the camera renderer
    // This doesn't call setRenderer() again, just updates the delegate
    try {
        Log.d("ArSceneView", "Switching delegating renderer to camera renderer")
        glSurfaceView.queueEvent {
            delegatingRenderer.updateDelegate(cameraRenderer)
        }
        Log.d("ArSceneView", "✅ Camera renderer activated, camera feed should now be visible")
    } catch (e: Exception) {
        Log.e("ArSceneView", "❌ Error activating camera renderer", e)
        onError("Failed to activate camera renderer: ${e.message}")
    }
}

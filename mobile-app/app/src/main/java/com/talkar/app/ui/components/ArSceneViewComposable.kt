package com.talkar.app.ui.components

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    // Log composable entry
    Log.d("ArSceneView", "🎬 ArSceneViewComposable entered")
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var arTrackingManager by remember { mutableStateOf<ARTrackingManager?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }
    var isInitializing by remember { mutableStateOf(false) }
    var frameProcessingStarted by remember { mutableStateOf(false) }
    
    Log.d("ArSceneView", "📊 State: isInitializing=$isInitializing, session=${session != null}, frameProcessingStarted=$frameProcessingStarted")
    
    // Lifecycle management for AR session
    DisposableEffect(lifecycleOwner, session) {
        val observer = LifecycleEventObserver { _, event ->
            val currentSession = session ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        Log.d("ArSceneView", "Resuming AR session from lifecycle")
                        currentSession.resume()
                    } catch (e: Exception) {
                        Log.e("ArSceneView", "Error resuming session", e)
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        Log.d("ArSceneView", "Pausing AR session from lifecycle")
                        currentSession.pause()
                    } catch (e: Exception) {
                        Log.e("ArSceneView", "Error pausing session", e)
                    }
                }
                else -> {}
            }
        }
        
        // Immediate resume if session is already available and lifecycle is resumed.
        // This handles the case where the session is created while the app is already in foreground.
        if (session != null && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                Log.d("ArSceneView", "Lifecycle already resumed, manually resuming session")
                session?.resume()
            } catch (e: Exception) {
                Log.e("ArSceneView", "Error manually resuming session", e)
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                
                // Set the session before loading posters so lifecycle observer can resume it
                session = arSession
                
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
                
                // Try to load posters with timeout
                val postersResult = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeout(10000) { // 10 second timeout
                            posterRepository.loadPosters()
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Log.w("ArSceneView", "Poster loading timed out, trying test poster")
                        posterRepository.loadTestPoster().map { listOf(it) }
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
                    } else {
                        Log.e("ArSceneView", "Failed to initialize tracking manager")
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
            val layout = createARCameraView(
                context = ctx,
                onError = onError
            )
            
            // Note: Renderer will be set later in setupCameraRenderer() when ARCore session is ready
            // We don't set it here to avoid IllegalStateException from calling setRenderer() twice
            
            layout
        },
        update = { view ->
            // Update view when session and tracking manager are ready
            // Only start once to avoid multiple coroutines
            if (session != null && arTrackingManager != null && !isInitializing && !frameProcessingStarted) {
                Log.d("ArSceneView", "AR session ready, setting up camera renderer")
                frameProcessingStarted = true
                
                // Set up the real camera renderer now that session is ready
                setupCameraRenderer(view, session!!, arTrackingManager!!, scope, lifecycleOwner, onError)
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
        // If surface was already created, initialize the new delegate
        if (surfaceCreatedCalled) {
            Log.d("ArSceneView", "Initializing new delegate with existing surface")
            newDelegate.onSurfaceCreated(lastGl, lastConfig)
            if (lastWidth > 0 && lastHeight > 0) {
                newDelegate.onSurfaceChanged(lastGl, lastWidth, lastHeight)
            }
        }
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
    trackingManager: ARTrackingManager,
    scope: CoroutineScope,
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
                    session.update()
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
                
                // Only render if tracking
                if (camera.trackingState == TrackingState.TRACKING) {
                    // Draw camera background
                    drawCameraBackground(frame)
                    
                    // Process frame for poster detection/tracking
                    trackingManager.processFrame(frame)
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
                
                // Full-screen quad vertices
                val quadCoords = floatArrayOf(
                    -1.0f, -1.0f,  // Bottom left
                     1.0f, -1.0f,  // Bottom right
                    -1.0f,  1.0f,  // Top left
                     1.0f,  1.0f   // Top right
                )
                
                // Texture coordinates
                val quadTexCoords = floatArrayOf(
                    0.0f, 0.0f,  // Bottom left
                    1.0f, 0.0f,  // Bottom right
                    0.0f, 1.0f,  // Top left
                    1.0f, 1.0f   // Top right
                )
                
                // Transform texture coordinates for ARCore
                val transformedTexCoords = FloatArray(8)
                frame.transformDisplayUvCoords(
                    java.nio.ByteBuffer.allocateDirect(quadTexCoords.size * 4)
                        .order(java.nio.ByteOrder.nativeOrder())
                        .asFloatBuffer()
                        .put(quadTexCoords)
                        .position(0) as java.nio.FloatBuffer,
                    java.nio.ByteBuffer.allocateDirect(transformedTexCoords.size * 4)
                        .order(java.nio.ByteOrder.nativeOrder())
                        .asFloatBuffer()
                        .also { it.get(transformedTexCoords) }
                        .position(0) as java.nio.FloatBuffer
                )
                
                // Set vertex positions
                val vertexBuffer = java.nio.ByteBuffer.allocateDirect(quadCoords.size * 4)
                    .order(java.nio.ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(quadCoords)
                    .position(0) as java.nio.FloatBuffer
                
                android.opengl.GLES20.glVertexAttribPointer(
                    positionAttrib, 2, android.opengl.GLES20.GL_FLOAT, false, 0, vertexBuffer
                )
                android.opengl.GLES20.glEnableVertexAttribArray(positionAttrib)
                
                // Set texture coordinates
                val texCoordBuffer = java.nio.ByteBuffer.allocateDirect(transformedTexCoords.size * 4)
                    .order(java.nio.ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(transformedTexCoords)
                    .position(0) as java.nio.FloatBuffer
                
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
        delegatingRenderer.updateDelegate(cameraRenderer)
        Log.d("ArSceneView", "✅ Camera renderer activated, camera feed should now be visible")
    } catch (e: Exception) {
        Log.e("ArSceneView", "❌ Error activating camera renderer", e)
        onError("Failed to activate camera renderer: ${e.message}")
    }
}


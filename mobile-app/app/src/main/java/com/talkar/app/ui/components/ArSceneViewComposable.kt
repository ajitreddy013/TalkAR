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
import com.google.ar.core.exceptions.SessionPausedException
import com.google.ar.core.exceptions.CameraNotAvailableException
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var arTrackingManager by remember { mutableStateOf<ARTrackingManager?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }
    var isInitializing by remember { mutableStateOf(false) }
    var frameProcessingStarted by remember { mutableStateOf(false) }
    
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
                Log.d("ArSceneView", "Initializing ARCore session...")
                
                // Create ARCore session
                val arSession = Session(context)
                
                // Configure session for augmented images
                val config = Config(arSession).apply {
                    focusMode = Config.FocusMode.AUTO
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                }
                arSession.configure(config)
                
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
                startFrameProcessing(view, session!!, arTrackingManager!!, scope, lifecycleOwner, onError)
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
    
    // Create GLSurfaceView for AR camera preview with OpenGL rendering
    val glSurfaceView = android.opengl.GLSurfaceView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2) // OpenGL ES 2.0
        setEGLConfigChooser(8, 8, 8, 8, 16, 0) // RGBA_8888, 16-bit depth
    }
    
    layout.addView(glSurfaceView)
    
    // Store GL surface view in layout tag for later access
    layout.tag = glSurfaceView
    
    return layout
}

/**
 * Start processing ARCore frames for tracking.
 * Called from AndroidView update block when session is ready.
 */
private fun startFrameProcessing(
    view: android.view.View,
    session: Session,
    trackingManager: ARTrackingManager,
    scope: CoroutineScope,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onError: (String) -> Unit
) {
    // Get the GL surface view from layout
    val layout = view as? FrameLayout ?: return
    val glSurfaceView = layout.tag as? android.opengl.GLSurfaceView ?: return
    
    Log.d("ArSceneView", "Starting frame processing with session and tracking manager")
    
    // Create a renderer for ARCore camera feed with background rendering
    val renderer = object : android.opengl.GLSurfaceView.Renderer {
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
            Log.d("ArSceneView", "GL Surface created")
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
                
                Log.d("ArSceneView", "✅ GL renderer initialized with shaders")
            } catch (e: Exception) {
                Log.e("ArSceneView", "Failed to initialize GL renderer", e)
            }
        }
        
        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = android.opengl.GLES20.glCreateShader(type)
            android.opengl.GLES20.glShaderSource(shader, shaderCode)
            android.opengl.GLES20.glCompileShader(shader)
            return shader
        }
        
        override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
            Log.d("ArSceneView", "GL Surface changed: ${width}x${height}")
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
                
                // Update ARCore frame
                val frame = session.update()
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
    
    // Set the renderer and start rendering
    glSurfaceView.setRenderer(renderer)
    glSurfaceView.renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
    
    Log.d("ArSceneView", "✅ GL renderer attached, camera feed should be visible")
}


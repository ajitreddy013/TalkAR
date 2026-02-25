package com.talkar.app.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.talkar.app.ar.video.models.TrackingData
import com.talkar.app.ar.video.tracking.ARTrackingManager
import com.talkar.app.ar.video.tracking.ARTrackingManagerImpl
import com.talkar.app.ar.video.tracking.ReferencePoster
import com.talkar.app.ar.video.tracking.TrackingListener
import com.talkar.app.ar.video.tracking.TrackedPoster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

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
    
    // Initialize ARCore session and tracking manager
    LaunchedEffect(Unit) {
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
                
                // TODO: Load reference posters from backend/database
                // For now, initialize with empty list - will be populated when backend is ready
                val posters = emptyList<ReferencePoster>()
                trackingManager.initialize(posters)
                
                Log.d("ArSceneView", "✅ ARCore initialized successfully")
                
            } catch (e: Exception) {
                Log.e("ArSceneView", "❌ Failed to initialize ARCore", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
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
                session = session,
                trackingManager = arTrackingManager,
                onError = onError
            )
        }
    )
}

/**
 * Creates the AR camera view with OpenGL rendering.
 */
private fun createARCameraView(
    context: Context,
    session: Session?,
    trackingManager: ARTrackingManager?,
    onError: (String) -> Unit
): android.view.View {
    val layout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
    
    // Create GLSurfaceView for AR rendering
    val glSurfaceView = GLSurfaceView(context).apply {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
    }
    
    // Create renderer
    val renderer = TalkingPhotoARRenderer(
        context = context,
        session = session,
        trackingManager = trackingManager,
        onError = onError
    )
    
    glSurfaceView.setRenderer(renderer)
    glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    
    layout.addView(glSurfaceView)
    
    return layout
}

/**
 * OpenGL renderer for AR camera feed and poster tracking.
 */
private class TalkingPhotoARRenderer(
    private val context: Context,
    private val session: Session?,
    private val trackingManager: ARTrackingManager?,
    private val onError: (String) -> Unit
) : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "TalkingPhotoARRenderer"
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "Surface created")
        android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "Surface changed: ${width}x${height}")
        android.opengl.GLES20.glViewport(0, 0, width, height)
        
        // Update session display geometry
        session?.setDisplayGeometry(
            context.resources.configuration.orientation,
            width,
            height
        )
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        android.opengl.GLES20.glClear(
            android.opengl.GLES20.GL_COLOR_BUFFER_BIT or 
            android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
        )
        
        val currentSession = session ?: return
        val currentTrackingManager = trackingManager ?: return
        
        try {
            // Update ARCore frame
            val frame = currentSession.update()
            
            // Process frame for poster detection/tracking
            currentTrackingManager.processFrame(frame)
            
            // TODO: Render camera background texture
            // TODO: Render AR overlays if needed
            
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available", e)
            onError("Camera not available")
        } catch (e: Exception) {
            Log.e(TAG, "Error in draw frame", e)
        }
    }
}

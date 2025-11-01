package com.talkar.app.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.ARImageRecognitionService
import com.talkar.app.TalkARApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.widget.FrameLayout
import android.view.ViewGroup
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun ARCameraView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current

    // Initialize AR service
    val arService = remember { ARImageRecognitionService(context) }

    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ARCameraView", "Initializing AR service on background thread...")
                val initialized = arService.initialize()
                android.util.Log.d("ARCameraView", "AR service initialized: $initialized")
                if (initialized) {
                    loadImagesIntoARCore(arService)
                }
            } catch (e: Exception) {
                android.util.Log.e("ARCameraView", "Failed to initialize AR service", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
            }
        }
    }

    // Handle lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            try {
                android.util.Log.d("ARCameraView", "Disposing AR service")
                arService.pauseProcessing()
            } catch (e: Exception) {
                android.util.Log.e("ARCameraView", "Error disposing AR service", e)
            }
        }
    }

    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val error by arService.error.collectAsState()

    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        android.util.Log.d("ARCameraView", "Recognized images updated: ${recognizedImages.size}")
        recognizedImages.forEach { augmentedImage ->
            android.util.Log.d("ARCameraView", "Processing recognized image: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)

            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let {
                android.util.Log.d("ARCameraView", "Image recognition result: ${it.name}")
                onImageRecognized(it)
            }
        }
    }

    // Always show the camera view (don't hide when image detected)
    AndroidView(
        factory = { ctx ->
            createARCameraView(ctx, arService, onImageRecognized, onError)
        },
        modifier = modifier
    )

    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
        android.util.Log.e("ARCameraView", "AR Error: $errorMessage")
    }
}

private suspend fun loadImagesIntoARCore(arService: ARImageRecognitionService) {
    withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("ARCameraView", "Loading images from backend into ARCore...")
            val imageRepository = TalkARApplication.instance.imageRepository

            imageRepository.getAllImages().collect { images ->
                android.util.Log.d("ARCameraView", "Loaded ${images.size} images from backend")
                val limitedImages = images.take(3) // Limit for performance
                android.util.Log.d("ARCameraView", "Processing ${limitedImages.size} images")
            }
        } catch (e: Exception) {
            android.util.Log.e("ARCameraView", "Failed to load images into ARCore", e)
        }
    }
}

private fun createARCameraView(
    context: Context,
    arService: ARImageRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit
): android.view.View {
    
    val layout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    // Create ARCore GLSurfaceView for camera preview
    val glSurfaceView = GLSurfaceView(context).apply {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha, depth, stencil
        preserveEGLContextOnPause = true
    }

    // Create AR renderer
    val arRenderer = ARCameraRenderer(context, arService, onImageRecognized, onError)
    glSurfaceView.setRenderer(arRenderer)
    glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

    layout.addView(glSurfaceView)

    return layout
}

/**
 * OpenGL renderer that displays camera feed and handles AR tracking
 */
private class ARCameraRenderer(
    private val context: Context,
    private val arService: ARImageRecognitionService,
    private val onImageRecognized: (ImageRecognition) -> Unit,
    private val onError: (String) -> Unit
) : GLSurfaceView.Renderer {

    private var session: Session? = null
    private var displayRotationHelper: DisplayRotationHelper? = null
    private var backgroundRenderer: BackgroundRenderer? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("ARCameraRenderer", "Surface created")
        
        // Set clear color to black
        android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        try {
            // Initialize background renderer for camera feed
            backgroundRenderer = BackgroundRenderer()
            backgroundRenderer?.createOnGlThread(context)
            
            // Initialize display rotation helper
            displayRotationHelper = DisplayRotationHelper(context)
            
            Log.d("ARCameraRenderer", "OpenGL setup complete")
            
        } catch (e: Exception) {
            Log.e("ARCameraRenderer", "Failed to initialize OpenGL", e)
            onError("Failed to initialize camera preview: ${e.message}")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("ARCameraRenderer", "Surface changed: ${width}x${height}")
        android.opengl.GLES20.glViewport(0, 0, width, height)
        displayRotationHelper?.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)

        try {
            // Check if ARCore is initialized
            if (!arService.isInitialized()) {
                return
            }

            // Get ARCore session from the service instead of creating a new one
            val session = arService.getSession()
            session?.let { session ->
                
                // Update display rotation
                displayRotationHelper?.updateSessionIfNeeded(session)
                
                try {
                    // Update ARCore frame
                    val frame = session.update()
                    val camera = frame.camera
                    
                    // Draw camera background
                    backgroundRenderer?.draw(frame)
                    
                    // Process frame for image recognition using the service
                    arService.processFrame(frame)
                    
                    // Check for recognized images and draw AR content
                    val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    for (augmentedImage in augmentedImages) {
                        when (augmentedImage.trackingState) {
                            TrackingState.TRACKING -> {
                                Log.d("ARCameraRenderer", "Image tracked: ${augmentedImage.name}")
                                // Here you would draw AR content on top of the detected image
                                // For now, we'll just trigger the callback
                                val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
                                imageRecognition?.let { onImageRecognized(it) }
                            }
                            else -> {
                                // Image not being tracked
                            }
                        }
                    }
                    
                } catch (e: CameraNotAvailableException) {
                    Log.e("ARCameraRenderer", "Camera not available", e)
                } catch (e: Exception) {
                    Log.e("ARCameraRenderer", "Error in draw frame", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e("ARCameraRenderer", "Error in onDrawFrame", e)
        }
    }
}

/**
 * Helper class for display rotation
 */
private class DisplayRotationHelper(private val context: Context) {
    fun onSurfaceChanged(width: Int, height: Int) {
        Log.d("DisplayRotationHelper", "Surface changed: ${width}x${height}")
    }
    
    fun updateSessionIfNeeded(session: Session) {
        // Handle display rotation updates
    }
}

/**
 * Renderer for camera background
 */
private class BackgroundRenderer {
    fun createOnGlThread(context: Context) {
        Log.d("BackgroundRenderer", "Created on GL thread")
        // Initialize shaders and textures for camera background
    }
    
    fun draw(frame: Frame) {
        // Draw camera background texture
        // This would typically involve:
        // 1. Getting camera texture from ARCore frame
        // 2. Drawing it as background using OpenGL shaders
        Log.v("BackgroundRenderer", "Drawing camera background")
    }
}

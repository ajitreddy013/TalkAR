package com.talkar.app.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.BackendImageARService
import com.google.ar.core.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun CameraPreviewView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    // Ensure the preview fills the available space
    Box(modifier = modifier.fillMaxSize()) {
    val context = LocalContext.current
    
    // Initialize Backend AR service for real image detection
    val arService = remember { BackendImageARService(context) }
    
    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        try {
            val initialized = arService.initialize()
            Log.d("CameraPreviewView", "AR service initialized: $initialized")
        } catch (e: Exception) {
            Log.e("CameraPreviewView", "Failed to initialize AR service", e)
            onError("Failed to initialize AR: ${e.message}")
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            try {
                arService.pauseTracking()
            } catch (e: Exception) {
                Log.e("CameraPreviewView", "Error disposing AR service", e)
            }
        }
    }
    
    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()
    
    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        Log.d("CameraPreviewView", "Recognized images updated: ${recognizedImages.size}")
        recognizedImages.forEach { augmentedImage ->
            Log.d("CameraPreviewView", "Processing recognized image: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)
            
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { 
                Log.d("CameraPreviewView", "Image recognition result: ${it.name}")
                onImageRecognized(it) 
            }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            createCameraXPreviewView(ctx, onError)
        },
        modifier = Modifier.fillMaxSize()
    )
    
    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
        Log.e("CameraPreviewView", "AR Error: $errorMessage")
    }
    }
}

private fun createCameraXPreviewView(
    context: Context,
    onError: (String) -> Unit
): android.view.View {
    val previewView = PreviewView(context)
    previewView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(context as LifecycleOwner, cameraSelector, preview)
        } catch (e: Exception) {
            onError(e.message ?: "CameraX error")
        }
    }, ContextCompat.getMainExecutor(context))

    return previewView
}

/**
 * OpenGL renderer that displays camera feed and handles AR tracking for BackendImageARService
 */
private class BackendCameraRendererV2(
    private val context: Context,
    private val arService: BackendImageARService,
    private val onImageRecognized: (ImageRecognition) -> Unit,
    private val onError: (String) -> Unit
) : GLSurfaceView.Renderer {

    private var displayRotationHelper: BackendDisplayRotationHelperV2? = null
    private var backgroundRenderer: BackendBackgroundRendererV2? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("BackendCameraRendererV2", "Surface created")
        
        // Set clear color to black
        android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        try {
            // Initialize background renderer for camera feed
            backgroundRenderer = BackendBackgroundRendererV2()
            backgroundRenderer?.createOnGlThread(context)
            
            // Initialize display rotation helper
            displayRotationHelper = BackendDisplayRotationHelperV2(context)
            
            Log.d("BackendCameraRendererV2", "OpenGL setup complete")
            
        } catch (e: Exception) {
            Log.e("BackendCameraRendererV2", "Failed to initialize OpenGL", e)
            onError("Failed to initialize camera preview: ${e.message}")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("BackendCameraRendererV2", "Surface changed: ${width}x${height}")
        android.opengl.GLES20.glViewport(0, 0, width, height)
        displayRotationHelper?.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        Log.d("BackendCameraRendererV2", "onDrawFrame called – rendering frame")
        // Clear screen
        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)

        try {
            // Check if ARCore is initialized
            if (!arService.isInitialized()) {
                return
            }

            // Get ARCore session from the service
            val session = arService.getSession()
            session?.let { session ->
            android.util.Log.d("BackendCameraRendererV2", "Session update start")
                
                // Update display rotation
                displayRotationHelper?.updateSessionIfNeeded(session)
                
                try {
                    // Update ARCore frame
                    val frame = session.update()
            android.util.Log.d("BackendCameraRendererV2", "Frame obtained")
                    val camera = frame.camera
                    
                    // Draw camera background
                    backgroundRenderer?.draw(frame)
                    
                    // Process frame for image recognition using the service
                    // The BackendImageARService handles its own frame processing
                    
                    // Check for recognized images and trigger callbacks
                    val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    // The service already updates its state, so we don't need to do anything here
                    // The composable will observe the state changes
                    
                } catch (e: com.google.ar.core.exceptions.CameraNotAvailableException) {
                    Log.e("BackendCameraRendererV2", "Camera not available", e)
                } catch (e: Exception) {
                    Log.e("BackendCameraRendererV2", "Error in draw frame", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e("BackendCameraRendererV2", "Error in onDrawFrame", e)
        }
    }
}

/**
 * Helper class for display rotation
 */
private class BackendDisplayRotationHelperV2(private val context: Context) {
    fun onSurfaceChanged(width: Int, height: Int) {
        Log.d("BackendDisplayRotationHelperV2", "Surface changed: ${width}x${height}")
    }
    
    fun updateSessionIfNeeded(session: Session) {
        // Handle display rotation updates
    }
}

/**
 * Renderer for camera background
 */
private class BackendBackgroundRendererV2 {
    fun createOnGlThread(context: Context) {
        Log.d("BackendBackgroundRendererV2", "Created on GL thread")
        // Initialize shaders and textures for camera background
        // In a full implementation, this would set up the actual OpenGL rendering
    }
    
    fun draw(frame: Frame) {
        // Draw camera background texture
        // This would typically involve:
        // 1. Getting camera texture from ARCore frame
        // 2. Drawing it as background using OpenGL shaders
        Log.v("BackendBackgroundRendererV2", "Drawing camera background")
    }
}
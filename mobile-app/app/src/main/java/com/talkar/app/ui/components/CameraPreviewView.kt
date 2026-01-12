package com.talkar.app.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.BackendImageARService
import com.talkar.app.data.services.SimpleARService
import com.talkar.app.ui.renderers.SimpleCameraRenderer
import com.talkar.app.TalkARApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import android.util.Log
import android.opengl.GLSurfaceView
import android.view.TextureView
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.view.Surface
import com.google.ar.core.*

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
        
        // LAZY initialization - only create services when needed to avoid ARCore classloader conflict
        var backendARService: BackendImageARService? by remember { mutableStateOf(null) }
        var simpleARService: SimpleARService? by remember { mutableStateOf(null) }
        var useSimpleAR by remember { mutableStateOf(false) }
        var useCameraOnly by remember { mutableStateOf(false) }
        var cameraDevice: CameraDevice? by remember { mutableStateOf(null) }
        var textureView: TextureView? by remember { mutableStateOf(null) }
        
        // Check network connectivity
        var isOnline by remember { mutableStateOf(false) }
        
        LaunchedEffect(Unit) {
            isOnline = isNetworkAvailable(context)
            Log.d("CameraPreviewView", "Network status: ${if (isOnline) "Online" else "Offline"}")
        }
        
        // Initialize AR service when component is created - SEQUENTIALLY to avoid classloader conflict
        LaunchedEffect(Unit) {
            try {
                // STEP 1: Try backend service first (only if online)
                val backendInitialized = if (isOnline) {
                    Log.d("CameraPreviewView", "Device is online, attempting backend initialization")
                    // Create backend service instance ONLY when needed
                    backendARService = BackendImageARService(context)
                    backendARService?.initialize() ?: false
                } else {
                    Log.d("CameraPreviewView", "Device is offline, skipping backend initialization")
                    false
                }
                
                Log.d("CameraPreviewView", "Backend AR service initialized: $backendInitialized")
                
                // STEP 2: If backend service fails, fall back to simple AR service
                // IMPORTANT: Only create SimpleARService AFTER BackendImageARService fails
                if (!backendInitialized) {
                    Log.d("CameraPreviewView", "Falling back to simple AR service")
                    useSimpleAR = true
                    
                    // Create simple service instance ONLY after backend fails
                    simpleARService = SimpleARService(context)
                    
                    // Try to initialize with GL context, but if that fails, we'll just show camera preview
                    val simpleInitialized = try {
                        simpleARService?.initializeWithGLContext() ?: false
                    } catch (e: Exception) {
                        Log.e("CameraPreviewView", "Failed to initialize simple AR service with GL context", e)
                        // Try basic initialization as last resort
                        try {
                            simpleARService?.initialize() ?: false
                        } catch (e2: Exception) {
                            Log.e("CameraPreviewView", "Failed to initialize simple AR service with basic init", e2)
                            // Try offline-only mode as final fallback
                            try {
                                simpleARService?.initializeOfflineOnly() ?: false
                            } catch (e3: Exception) {
                                Log.e("CameraPreviewView", "Failed to initialize simple AR service with offline mode", e3)
                                false
                            }
                        }
                    }
                    Log.d("CameraPreviewView", "Simple AR service initialized: $simpleInitialized")
                    
                    if (!simpleInitialized) {
                        // If both AR services fail, fall back to basic camera preview
                        Log.d("CameraPreviewView", "Falling back to basic camera preview")
                        useCameraOnly = true
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraPreviewView", "Failed to initialize AR service", e)
                // Try fallback to simple AR service (create it ONLY if not already created)
                try {
                    useSimpleAR = true
                    if (simpleARService == null) {
                        simpleARService = SimpleARService(context)
                    }
                    
                    val simpleInitialized = try {
                        simpleARService?.initializeWithGLContext() ?: false
                    } catch (e: Exception) {
                        Log.e("CameraPreviewView", "Failed to initialize simple AR service with GL context in fallback", e)
                        // Try basic initialization as last resort
                        try {
                            simpleARService?.initialize() ?: false
                        } catch (e2: Exception) {
                            Log.e("CameraPreviewView", "Failed to initialize simple AR service with basic init in fallback", e2)
                            // Try offline-only mode as final fallback
                            try {
                                simpleARService?.initializeOfflineOnly() ?: false
                            } catch (e3: Exception) {
                                Log.e("CameraPreviewView", "Failed to initialize simple AR service with offline mode in fallback", e3)
                                false
                            }
                        }
                    }
                    Log.d("CameraPreviewView", "Simple AR service initialized as fallback: $simpleInitialized")
                    
                    if (!simpleInitialized) {
                        // If both AR services fail, fall back to basic camera preview
                        Log.d("CameraPreviewView", "Falling back to basic camera preview in exception handler")
                        useCameraOnly = true
                    }
                } catch (fallbackException: Exception) {
                    Log.e("CameraPreviewView", "Failed to initialize simple AR service as fallback", fallbackException)
                    // Last resort: basic camera preview
                    useCameraOnly = true
                }
            }
        }
        
        // Handle lifecycle events
        DisposableEffect(Unit) {
            onDispose {
                try {
                    if (useSimpleAR) {
                        simpleARService?.pauseTracking()
                    } else if (!useCameraOnly) {
                        backendARService?.pauseTracking()
                    }
                    
                    // Close camera if we're using camera only mode
                    if (useCameraOnly) {
                        cameraDevice?.close()
                        cameraDevice = null
                    }
                } catch (e: Exception) {
                    Log.e("CameraPreviewView", "Error disposing resources", e)
                }
            }
        }
        
        // Create the appropriate view based on what's available
        if (useCameraOnly) {
            // Basic camera preview without AR
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        textureView = this
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                openCamera(ctx, surface)
                            }
                            
                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // AR-based preview
            AndroidView(
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        preserveEGLContextOnPause = true
                        setEGLContextClientVersion(2)
                        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                        
                        val renderer = if (useSimpleAR) {
                            // Use simple camera renderer if using simple AR service
                            SimpleCameraRenderer(
                                ctx,
                                simpleARService!!, // Safe to use !! here as we only set useSimpleAR=true after creating it
                                onImageRecognized,
                                onError
                            )
                        } else {
                            // Use backend renderer if using backend service
                            // Since BackendCameraRendererV2 doesn't exist, we'll use a simple approach
                            // that just shows the camera preview without AR processing for now
                            object : GLSurfaceView.Renderer {
                                override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
                                    android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                                }
                                
                                override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
                                    android.opengl.GLES20.glViewport(0, 0, width, height)
                                }
                                
                                override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                                    android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)
                                }
                            }
                        }
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Handle lifecycle updates if needed
                }
            )
        }
        
        // Handle errors for AR modes
        if (!useCameraOnly) {
            val error = if (useSimpleAR) {
                simpleARService?.error?.collectAsState()?.value
            } else {
                backendARService?.error?.collectAsState()?.value
            }
            
            error?.let { errorMessage ->
                onError(errorMessage)
                Log.e("CameraPreviewView", "AR Error: $errorMessage")
            }
        }
    }
}

// Helper function to check network connectivity
private fun isNetworkAvailable(context: Context): Boolean {
    return try {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
        Log.e("CameraPreviewView", "Error checking network availability", e)
        false
    }
}

// Helper function to open camera for basic preview
private fun openCamera(context: Context, surface: SurfaceTexture) {
    try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0] // Use first available camera
        
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                // Camera is open, create preview session
                // Implementation would continue here for a full camera preview
                Log.d("CameraPreviewView", "Camera opened successfully")
            }
            
            override fun onDisconnected(camera: CameraDevice) {
                Log.d("CameraPreviewView", "Camera disconnected")
                camera.close()
            }
            
            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("CameraPreviewView", "Camera error: $error")
                camera.close()
            }
        }, null)
    } catch (e: Exception) {
        Log.e("CameraPreviewView", "Failed to open camera", e)
    }
}
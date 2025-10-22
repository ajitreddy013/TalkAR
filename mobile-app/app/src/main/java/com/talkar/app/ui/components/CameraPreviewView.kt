package com.talkar.app.ui.components

import android.content.Context
import android.hardware.camera2.*
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.BackendImageARService
import kotlinx.coroutines.launch

@Composable
fun CameraPreviewView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize Backend AR service for real image detection
    val arService = remember { BackendImageARService(context) }
    
    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        try {
            val initialized = arService.initialize()
            android.util.Log.d("CameraPreviewView", "AR service initialized: $initialized")
        } catch (e: Exception) {
            android.util.Log.e("CameraPreviewView", "Failed to initialize AR service", e)
            onError("Failed to initialize AR: ${e.message}")
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            try {
                arService.pauseTracking()
            } catch (e: Exception) {
                android.util.Log.e("CameraPreviewView", "Error disposing AR service", e)
            }
        }
    }
    
    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()
    
    // Handle session resumption
    LaunchedEffect(isTracking) {
        if (isTracking) {
            android.util.Log.d("CameraPreviewView", "ARCore session resumed, resuming processing")
            try {
                arService.resumeTracking()
            } catch (e: Exception) {
                android.util.Log.e("CameraPreviewView", "Error resuming processing", e)
            }
        }
    }
    
    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        android.util.Log.d("CameraPreviewView", "Recognized images updated: ${recognizedImages.size}")
        recognizedImages.forEach { augmentedImage ->
            android.util.Log.d("CameraPreviewView", "Processing recognized image: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)
            
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { 
                android.util.Log.d("CameraPreviewView", "Image recognition result: ${it.name}")
                onImageRecognized(it) 
            }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            createCameraPreviewView(ctx, arService, onImageRecognized)
        },
        modifier = modifier
    )
    
    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
        android.util.Log.e("CameraPreviewView", "AR Error: $errorMessage")
    }
}

private fun createCameraPreviewView(
    context: Context,
    arService: BackendImageARService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
): android.view.View {
    // Create a layout to hold camera preview and overlays
    val layout = android.widget.FrameLayout(context)
    
    // Create TextureView for camera preview
    val textureView = android.view.TextureView(context)
    textureView.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("CameraPreviewView", "Surface texture available: ${width}x${height}")
            // Initialize camera when surface is available
            initializeCamera(textureView, arService, onImageRecognized)
        }
        
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("CameraPreviewView", "Surface texture size changed: ${width}x${height}")
        }
        
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            android.util.Log.d("CameraPreviewView", "Surface texture destroyed")
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // This is called continuously when the camera is working
        }
    }
    
    layout.addView(textureView)
    
    // Add scanning indicator overlay
    val scanningOverlay = android.widget.TextView(context).apply {
        text = "🎯 Point camera at image to scan"
        textSize = 18f
        setTextColor(android.graphics.Color.WHITE)
        gravity = android.view.Gravity.CENTER
        setPadding(20, 20, 20, 20)
        setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
    }
    
    val overlayParams = android.widget.FrameLayout.LayoutParams(
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
    )
    overlayParams.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER
    overlayParams.topMargin = 100
    layout.addView(scanningOverlay, overlayParams)
    
    // No test button - real image detection only
    
    return layout
}

private fun initializeCamera(
    textureView: android.view.TextureView, 
    arService: BackendImageARService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("CameraPreviewView", "Initializing camera with ARCore integration...")
        
        // Create camera manager
        val cameraManager = textureView.context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraIdList = cameraManager.cameraIdList
        
        if (cameraIdList.isEmpty()) {
            android.util.Log.e("CameraPreviewView", "No cameras available")
            return
        }
        
        val cameraId = cameraIdList[0] // Use first available camera
        android.util.Log.d("CameraPreviewView", "Using camera: $cameraId")
        
        // Check camera permissions
        val hasPermission = textureView.context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            android.util.Log.e("CameraPreviewView", "Camera permission not granted")
            return
        }
        
        cameraManager.openCamera(cameraId, object : android.hardware.camera2.CameraDevice.StateCallback() {
            override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("CameraPreviewView", "Camera opened successfully")
                
                try {
                    // Create capture session
                    val surface = android.view.Surface(textureView.surfaceTexture)
                    val captureRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)
                    
                    camera.createCaptureSession(listOf(surface), object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.d("CameraPreviewView", "Camera session configured")
                            try {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                                
                                // BackendImageARService handles AR processing internally
                                android.util.Log.d("CameraPreviewView", "Camera preview started - AR processing handled by BackendImageARService")
                            } catch (e: Exception) {
                                android.util.Log.e("CameraPreviewView", "Failed to start repeating request", e)
                            }
                        }
                        
                        override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.e("CameraPreviewView", "Camera session configuration failed")
                        }
                    }, null)
                } catch (e: Exception) {
                    android.util.Log.e("CameraPreviewView", "Failed to create capture session", e)
                }
            }
            
            override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("CameraPreviewView", "Camera disconnected")
            }
            
            override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                android.util.Log.e("CameraPreviewView", "Camera error: $error")
            }
        }, null)
        
    } catch (e: Exception) {
        android.util.Log.e("CameraPreviewView", "Failed to initialize camera", e)
    }
}

// BackendImageARService handles all AR processing internally
// No manual frame processing needed

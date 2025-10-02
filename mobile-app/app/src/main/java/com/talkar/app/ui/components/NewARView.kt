package com.talkar.app.ui.components

import android.content.Context
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

@Composable
fun NewARView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize AR service
    val arService = remember { ARImageRecognitionService(context) }
    
    // Initialize AR service when component is created - on background thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("NewARView", "Initializing AR service on background thread...")
                val initialized = arService.initialize()
                android.util.Log.d("NewARView", "AR service initialized: $initialized")
                if (initialized) {
                    // Load images from backend into ARCore asynchronously
                    loadImagesIntoARCore(arService)
                }
            } catch (e: Exception) {
                android.util.Log.e("NewARView", "Failed to initialize AR service", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
            }
        }
    }
    
    // Handle lifecycle events with proper camera/ARCore coordination
    DisposableEffect(Unit) {
        onDispose {
            try {
                android.util.Log.d("NewARView", "Disposing AR service - pausing processing first")
                // Pause ARCore session before disposing to avoid camera conflicts
                arService.pauseProcessing()
                android.util.Log.d("NewARView", "AR service paused successfully")
            } catch (e: Exception) {
                android.util.Log.e("NewARView", "Error disposing AR service", e)
            }
        }
    }
    
    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()
    
    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        android.util.Log.d("NewARView", "Recognized images updated: ${recognizedImages.size}")
        recognizedImages.forEach { augmentedImage ->
            android.util.Log.d("NewARView", "Processing recognized image: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)
            
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { 
                android.util.Log.d("NewARView", "Image recognition result: ${it.name}")
                onImageRecognized(it) 
            }
        }
    }
    
    // Hide the view when image is detected to allow overlays to show
    if (!isImageDetected) {
        AndroidView(
            factory = { ctx ->
                createNewARView(ctx, arService, onImageRecognized)
            },
            modifier = modifier
        )
    }
    
    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
        android.util.Log.e("NewARView", "AR Error: $errorMessage")
    }
}

private suspend fun loadImagesIntoARCore(arService: ARImageRecognitionService) {
    withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("NewARView", "Loading images from backend into ARCore on background thread...")
            val imageRepository = TalkARApplication.instance.imageRepository
            
            imageRepository.getAllImages().collect { images ->
                android.util.Log.d("NewARView", "Loaded ${images.size} images from backend")
                
                // Limit the number of images to prevent memory issues
                val maxImages = 5 // Limit to 5 images for better performance
                val limitedImages = images.take(maxImages)
                
                android.util.Log.d("NewARView", "Processing ${limitedImages.size} images (limited from ${images.size} for performance)")
                
                limitedImages.forEach { image ->
                    try {
                        // Process each image on background thread
                        withContext(Dispatchers.IO) {
                            android.util.Log.d("NewARView", "Processing image for ARCore: ${image.name}")
                            
                            // For now, we'll use the existing test images in ARCore
                            // In production, you would:
                            // 1. Download image bytes from backend URL
                            // 2. Optimize image size and quality
                            // 3. Add to ARCore database: arService.addReferenceImage(image.name, imageBytes)
                            
                            android.util.Log.d("NewARView", "Image ${image.name} ready for recognition")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewARView", "Failed to process image ${image.name}", e)
                    }
                }
                
                android.util.Log.d("NewARView", "Completed processing ${limitedImages.size} images")
            }
        } catch (e: Exception) {
            android.util.Log.e("NewARView", "Failed to load images into ARCore", e)
        }
    }
}

private fun createNewARView(
    context: Context,
    arService: ARImageRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit
): android.view.View {
    // Create a camera preview with actual camera feed
    val layout = android.widget.FrameLayout(context).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK) // Black background for camera
    }
    
    // Create TextureView for camera preview
    val textureView = android.view.TextureView(context)
    textureView.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("NewARView", "Surface texture available: ${width}x${height}")
            initializeCamera(textureView, arService, onImageRecognized)
        }
        
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("NewARView", "Surface texture size changed: ${width}x${height}")
        }
        
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            android.util.Log.d("NewARView", "Surface texture destroyed")
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // This is called continuously when the camera is working
        }
    }
    
    layout.addView(textureView)
    
    // Add scanning indicator overlay
    val scanningOverlay = android.widget.TextView(context).apply {
        text = "📷 Camera Preview\n\nPoint camera at an image to scan"
        textSize = 18f
        setTextColor(android.graphics.Color.WHITE)
        gravity = android.view.Gravity.CENTER
        setPadding(40, 40, 40, 40)
        setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
    }
    
    val overlayParams = android.widget.FrameLayout.LayoutParams(
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
    )
    overlayParams.gravity = android.view.Gravity.CENTER
    layout.addView(scanningOverlay, overlayParams)
    
    return layout
}

private fun initializeCamera(
    textureView: android.view.TextureView,
    arService: ARImageRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("NewARView", "Initializing camera...")
        
        // Create camera manager
        val cameraManager = textureView.context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraIdList = cameraManager.cameraIdList
        
        if (cameraIdList.isEmpty()) {
            android.util.Log.e("NewARView", "No cameras available")
            return
        }
        
        val cameraId = cameraIdList[0] // Use first available camera
        android.util.Log.d("NewARView", "Using camera: $cameraId")
        
        // Check camera permissions
        val permission = android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPermission = textureView.context.checkSelfPermission(android.Manifest.permission.CAMERA) == permission
        
        if (!hasPermission) {
            android.util.Log.e("NewARView", "Camera permission not granted")
            return
        }
        
        cameraManager.openCamera(cameraId, object : android.hardware.camera2.CameraDevice.StateCallback() {
            override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("NewARView", "Camera opened successfully")
                
                try {
                    // Create capture session
                    val surface = android.view.Surface(textureView.surfaceTexture)
                    val captureRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)
                    
                    camera.createCaptureSession(listOf(surface), object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.d("NewARView", "Camera session configured")
                            try {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                                android.util.Log.d("NewARView", "Camera preview started")
                                
                                // Start ARCore frame processing
                                startARFrameProcessing(arService, onImageRecognized)
                            } catch (e: Exception) {
                                android.util.Log.e("NewARView", "Failed to start repeating request", e)
                            }
                        }
                        
                        override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.e("NewARView", "Camera session configuration failed")
                        }
                    }, null)
                } catch (e: Exception) {
                    android.util.Log.e("NewARView", "Failed to create capture session", e)
                }
            }
            
            override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("NewARView", "Camera disconnected")
            }
            
            override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                android.util.Log.e("NewARView", "Camera error: $error")
            }
        }, null)
        
    } catch (e: Exception) {
        android.util.Log.e("NewARView", "Failed to initialize camera", e)
    }
}

private fun startARFrameProcessing(
    arService: ARImageRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("NewARView", "Starting optimized ARCore frame processing with error handling...")
        
        // Start ARCore processing on a background thread to avoid blocking UI
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            try {
                // Add delay to ensure camera is fully initialized before ARCore resume
                kotlinx.coroutines.delay(1000) // Wait 1 second for camera to stabilize
                
                android.util.Log.d("NewARView", "Attempting to resume ARCore processing...")
                
                // Resume ARCore processing with proper error handling
                arService.resumeProcessing()
                android.util.Log.d("NewARView", "ARCore frame processing started successfully on background thread")
                
            } catch (e: com.google.ar.core.exceptions.CameraNotAvailableException) {
                android.util.Log.e("NewARView", "Camera not available for ARCore", e)
                // Retry after a delay
                kotlinx.coroutines.delay(2000)
                try {
                    arService.resumeProcessing()
                    android.util.Log.d("NewARView", "ARCore processing resumed after retry")
                } catch (retryException: Exception) {
                    android.util.Log.e("NewARView", "Failed to resume ARCore after retry", retryException)
                }
            } catch (e: com.google.ar.core.exceptions.SessionPausedException) {
                android.util.Log.w("NewARView", "ARCore session was paused, will retry when resumed", e)
            } catch (e: Exception) {
                android.util.Log.e("NewARView", "Failed to resume AR processing on background thread", e)
            }
        }
        
    } catch (e: Exception) {
        android.util.Log.e("NewARView", "Failed to start AR processing", e)
    }
}

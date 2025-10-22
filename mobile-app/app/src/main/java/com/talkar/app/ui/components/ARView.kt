package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Frame
// Removed Sceneform imports - using modern ARCore approach
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.ARImageRecognitionService
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

@Composable
fun ARView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize AR service
    val arService = remember { ARImageRecognitionService(context) }
    
    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        val initialized = arService.initialize()
        android.util.Log.d("ARView", "AR service initialized: $initialized")
    }
    
    // Handle lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            // Pause AR processing when component is disposed to reduce CPU usage
            arService.pauseProcessing()
        }
    }
    
    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()
    
    // Handle session resumption
    LaunchedEffect(isTracking) {
        if (isTracking) {
            android.util.Log.d("ARView", "ARCore session resumed, resuming processing")
            arService.resumeProcessing()
        }
    }
    
    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        android.util.Log.d("ARView", "Recognized images updated: ${recognizedImages.size}")
        recognizedImages.forEach { augmentedImage ->
            android.util.Log.d("ARView", "Processing recognized image: ${augmentedImage.name}")
            // Pass the augmented image for AR overlay positioning
            onAugmentedImageRecognized(augmentedImage)
            
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { 
                android.util.Log.d("ARView", "Image recognition result: ${it.name}")
                onImageRecognized(it) 
            }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            createARView(ctx, arService, onImageRecognized)
        },
        modifier = modifier
    )
    
    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
        println("AR Error: $errorMessage")
    }
}

private fun createARView(
    context: Context,
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
): android.view.View {
    return try {
        // Create ARCore-powered view
        createARCoreView(context, arService, onImageRecognized)
    } catch (e: Exception) {
        android.util.Log.e("ARView", "Failed to create ARCore view", e)
        // Fallback to a placeholder view with instructions
        createFallbackView(context)
    }
}

private fun createARCoreView(
    context: Context,
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
): android.view.View {
    // Create camera preview with ARCore integration
    return createCameraPreviewView(context, arService, onImageRecognized)
}

private fun simulateImageDetection(
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("ARView", "Simulating image detection for testing")
        // Create a mock image recognition for testing
        val mockImageRecognition = com.talkar.app.data.models.ImageRecognition(
            id = "test_image_1",
            name = "test_image_1", 
            description = "Test image for AR recognition",
            imageUrl = "",
            dialogues = emptyList(),
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        )
        
        android.util.Log.d("ARView", "Mock image recognition created: ${mockImageRecognition.name}")
        
        // Trigger the recognition callback
        onImageRecognized(mockImageRecognition)
        
    } catch (e: Exception) {
        android.util.Log.e("ARView", "Error simulating image detection", e)
    }
}

private fun createCameraPreviewView(
    context: Context,
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
): android.view.View {
    // Create a layout to hold both camera preview and AR overlays
    val layout = android.widget.FrameLayout(context)
    
    // Create TextureView for camera preview
    val textureView = android.view.TextureView(context)
    textureView.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("ARView", "Surface texture available: ${width}x${height}")
            // Initialize camera when surface is available
            initializeCamera(textureView, arService, onImageRecognized)
        }
        
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("ARView", "Surface texture size changed: ${width}x${height}")
        }
        
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            android.util.Log.d("ARView", "Surface texture destroyed")
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // This is called continuously when the camera is working
        }
    }
    
    layout.addView(textureView)
    
    // Add a test button overlay for debugging
    val testButton = android.widget.Button(context)
    testButton.text = "Test Detection"
    testButton.setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
    testButton.setTextColor(android.graphics.Color.WHITE)
    testButton.setOnClickListener {
        android.util.Log.d("ARView", "Test button clicked - simulating image detection")
        simulateImageDetection(arService, onImageRecognized)
    }
    
    val buttonParams = android.widget.FrameLayout.LayoutParams(
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
    )
    buttonParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER
    buttonParams.bottomMargin = 50
    layout.addView(testButton, buttonParams)
    
    return layout
}

private fun initializeCamera(
    textureView: android.view.TextureView, 
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("ARView", "Initializing camera with ARCore integration...")
        
        // Create a simple camera preview
        val cameraManager = textureView.context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraIdList = cameraManager.cameraIdList
        
        if (cameraIdList.isEmpty()) {
            android.util.Log.e("ARView", "No cameras available")
            return
        }
        
        val cameraId = cameraIdList[0] // Use first available camera
        android.util.Log.d("ARView", "Using camera: $cameraId")
        
        // Check camera permissions
        val hasPermission = textureView.context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            android.util.Log.e("ARView", "Camera permission not granted")
            return
        }
        
        cameraManager.openCamera(cameraId, object : android.hardware.camera2.CameraDevice.StateCallback() {
            override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("ARView", "Camera opened successfully")
                
                try {
                    // Create capture session
                    val surface = android.view.Surface(textureView.surfaceTexture)
                    val captureRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)
                    
                    camera.createCaptureSession(listOf(surface), object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.d("ARView", "Camera session configured")
                            try {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                                
                                // Start ARCore frame processing
                                startARFrameProcessing(arService, onImageRecognized)
                            } catch (e: Exception) {
                                android.util.Log.e("ARView", "Failed to start repeating request", e)
                            }
                        }
                        
                        override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.e("ARView", "Camera session configuration failed")
                        }
                    }, null)
                } catch (e: Exception) {
                    android.util.Log.e("ARView", "Failed to create capture session", e)
                }
            }
            
            override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("ARView", "Camera disconnected")
            }
            
            override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                val errorMessage = when (error) {
                    android.hardware.camera2.CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> "Camera is already in use."
                    android.hardware.camera2.CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> "Maximum number of cameras are in use."
                    android.hardware.camera2.CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> "Camera is disabled by a device policy."
                    android.hardware.camera2.CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> "Camera device has encountered a fatal error."
                    android.hardware.camera2.CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> "Camera service has encountered a fatal error."
                    else -> "An unknown camera error occurred: $error"
                }
                android.util.Log.e("ARView", "Camera error: $errorMessage")
                arService.postError("Camera error: $errorMessage")
            }
        }, null)
    } catch (e: Exception) {
        android.util.Log.e("ARView", "Failed to initialize camera", e)
    }
}

private fun startARFrameProcessing(
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("ARView", "Starting AR frame processing...")
        
        // Create a coroutine to continuously process ARCore frames
        CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
            var frameCount = 0
            while (true) {
                try {
                    // Get the current ARCore session
                    val session = arService.getSession()
                    
                    // Check if session exists and is ready
                    if (session == null) {
                        android.util.Log.d("ARView", "No ARCore session available, waiting...")
                        kotlinx.coroutines.delay(3000L)
                        return@launch
                    }
                    
                    // Check if session is tracking
                    if (!arService.isTracking.value) {
                        android.util.Log.d("ARView", "Session not tracking, waiting...")
                        kotlinx.coroutines.delay(3000L)
                        return@launch
                    }
                    
                    // Try to update the session
                    try {
                        val frame = session.update()
                        
                        // Process the frame for image recognition
                        arService.processFrame(frame)
                        
                        frameCount++
                        
                        // Adaptive delay based on processing load
                        val delay = if (frameCount % 10 == 0) {
                            // Every 10th frame, take a longer break to reduce CPU load
                            200L
                        } else {
                            // Normal processing delay
                            150L
                        }
                        kotlinx.coroutines.delay(delay)
                        
                    } catch (e: com.google.ar.core.exceptions.SessionPausedException) {
                        android.util.Log.d("ARView", "ARCore session is paused, waiting for resume...")
                        // Don't pause the service, just wait longer
                        kotlinx.coroutines.delay(5000L)
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("ARView", "Error in AR frame processing", e)
                    kotlinx.coroutines.delay(3000L)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("ARView", "Failed to start AR frame processing", e)
    }
}

private fun createFallbackView(context: Context): android.view.View {
    // Create a fallback view that looks like a camera preview
    val layout = android.widget.FrameLayout(context)
    layout.setBackgroundColor(android.graphics.Color.BLACK)
    
    // Add a subtle pattern to simulate camera feed
    val patternView = android.view.View(context)
    patternView.setBackgroundColor(android.graphics.Color.DKGRAY)
    layout.addView(patternView)
    
    // Add a centered message
    val textView = android.widget.TextView(context)
    textView.text = "📷 AR Camera\n\nScanning for images..."
    textView.setTextColor(android.graphics.Color.WHITE)
    textView.textSize = 16f
    textView.gravity = android.view.Gravity.CENTER
    textView.setPadding(32, 32, 32, 32)
    
    val textLayout = android.widget.LinearLayout(context)
    textLayout.orientation = android.widget.LinearLayout.VERTICAL
    textLayout.gravity = android.view.Gravity.CENTER
    textLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    textLayout.addView(textView)
    
    layout.addView(textLayout)
    
    return layout
}


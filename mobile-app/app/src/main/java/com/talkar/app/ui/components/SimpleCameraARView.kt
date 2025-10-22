package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.ARImageRecognitionService
import com.talkar.app.TalkARApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import android.util.Log
import android.widget.FrameLayout
import android.view.ViewGroup
import android.view.TextureView
import android.hardware.camera2.*
import android.view.Surface

@Composable
fun SimpleCameraARView(
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
                android.util.Log.d("SimpleCameraARView", "Initializing AR service...")
                val initialized = arService.initialize()
                android.util.Log.d("SimpleCameraARView", "AR service initialized: $initialized")
            } catch (e: Exception) {
                android.util.Log.e("SimpleCameraARView", "Failed to initialize AR service", e)
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
                arService.pauseProcessing()
            } catch (e: Exception) {
                android.util.Log.e("SimpleCameraARView", "Error disposing AR service", e)
            }
        }
    }

    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val error by arService.error.collectAsState()

    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        recognizedImages.forEach { augmentedImage ->
            android.util.Log.d("SimpleCameraARView", "Image recognized: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)

            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let {
                android.util.Log.d("SimpleCameraARView", "Triggering image recognition: ${it.name}")
                onImageRecognized(it)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview with AR processing
        AndroidView(
            factory = { ctx ->
                createCameraPreviewWithAR(ctx, arService, onError)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Camera viewfinder overlay (always visible)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Viewfinder frame
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                // Corner brackets for viewfinder
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw viewfinder corners here if needed
                }
                
                Text(
                    text = if (isImageDetected) "✅ Image Detected!" else "🎯 Point camera at image",
                    color = if (isImageDetected) Color.Green else Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                )
            }
        }

        // AR overlay content (when image is detected)
        if (isImageDetected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎬 AR Experience Active",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Lip sync and head movement will start here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
    }
}

private fun createCameraPreviewWithAR(
    context: Context,
    arService: ARImageRecognitionService,
    onError: (String) -> Unit
): android.view.View {
    
    val layout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
    }

    // Create TextureView for camera preview
    val textureView = TextureView(context)
    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("SimpleCameraARView", "Camera surface available: ${width}x${height}")
            initializeCameraWithAR(textureView, arService, onError)
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("SimpleCameraARView", "Camera surface size changed: ${width}x${height}")
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            Log.d("SimpleCameraARView", "Camera surface destroyed")
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Camera frame updated - this is where we could process frames
        }
    }

    layout.addView(textureView)
    return layout
}

private fun initializeCameraWithAR(
    textureView: TextureView,
    arService: ARImageRecognitionService,
    onError: (String) -> Unit
) {
    try {
        Log.d("SimpleCameraARView", "Initializing camera with AR processing...")

        val cameraManager = textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList

        if (cameraIdList.isEmpty()) {
            onError("No cameras available")
            return
        }

        val cameraId = cameraIdList[0] // Use back camera
        Log.d("SimpleCameraARView", "Using camera: $cameraId")

        // Check camera permission
        val hasPermission = textureView.context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            onError("Camera permission not granted")
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("SimpleCameraARView", "Camera opened successfully")
                startCameraPreview(camera, textureView, arService)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.d("SimpleCameraARView", "Camera disconnected")
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("SimpleCameraARView", "Camera error: $error")
                camera.close()
                onError("Camera error: $error")
            }
        }, null)

    } catch (e: Exception) {
        Log.e("SimpleCameraARView", "Failed to initialize camera", e)
        onError("Failed to initialize camera: ${e.message}")
    }
}

private fun startCameraPreview(
    camera: CameraDevice,
    textureView: TextureView,
    arService: ARImageRecognitionService
) {
    try {
        val surface = Surface(textureView.surfaceTexture)
        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequest.addTarget(surface)

        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Log.d("SimpleCameraARView", "Camera session configured")
                try {
                    session.setRepeatingRequest(captureRequest.build(), null, null)
                    Log.d("SimpleCameraARView", "Camera preview started")
                    
                    // Start AR processing after camera is ready
                    startARProcessing(arService)
                    
                } catch (e: Exception) {
                    Log.e("SimpleCameraARView", "Failed to start camera preview", e)
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("SimpleCameraARView", "Camera session configuration failed")
            }
        }, null)

    } catch (e: Exception) {
        Log.e("SimpleCameraARView", "Failed to start camera preview", e)
    }
}

private fun startARProcessing(arService: ARImageRecognitionService) {
    CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
        try {
            // Wait for AR service to be fully initialized
            var attempts = 0
            while (attempts < 10 && !arService.isInitialized()) {
                kotlinx.coroutines.delay(1000)
                attempts++
                Log.d("SimpleCameraARView", "Waiting for AR initialization (attempt $attempts)")
            }
            
            if (arService.isInitialized()) {
                Log.d("SimpleCameraARView", "Starting AR processing...")
                arService.resumeProcessing()
                Log.d("SimpleCameraARView", "AR processing started")
            } else {
                Log.w("SimpleCameraARView", "AR service not initialized after waiting")
            }
            
        } catch (e: Exception) {
            Log.e("SimpleCameraARView", "Failed to start AR processing", e)
        }
    }
}

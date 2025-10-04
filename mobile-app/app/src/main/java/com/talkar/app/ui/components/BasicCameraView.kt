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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.widget.FrameLayout
import android.view.ViewGroup
import android.view.TextureView
import android.hardware.camera2.*
import android.view.Surface

@Composable
fun BasicCameraView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    var cameraInitialized by remember { mutableStateOf(false) }

    // Initialize AR service for image recognition (without frame processing)
    val arService = remember { ARImageRecognitionService(context) }

    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("BasicCameraView", "Initializing AR service...")
                val initialized = arService.initialize()
                android.util.Log.d("BasicCameraView", "AR service initialized: $initialized")
            } catch (e: Exception) {
                android.util.Log.e("BasicCameraView", "Failed to initialize AR service", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
            }
        }
    }

    // Simulate image detection for testing (since ARCore frame processing is problematic)
    LaunchedEffect(cameraInitialized) {
        if (cameraInitialized) {
            // Wait a few seconds then simulate detection
            kotlinx.coroutines.delay(5000)
            
            // Simulate detecting a test image
            val simulatedImage = ImageRecognition(
                id = "test-image-1",
                name = "Test Object",
                description = "Simulated detection for testing",
                imageUrl = "",
                dialogues = emptyList(),
                createdAt = System.currentTimeMillis().toString(),
                updatedAt = System.currentTimeMillis().toString()
            )
            
            android.util.Log.d("BasicCameraView", "Simulating image detection: ${simulatedImage.name}")
            onImageRecognized(simulatedImage)
        }
    }

    // Handle lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            try {
                arService.pauseProcessing()
            } catch (e: Exception) {
                android.util.Log.e("BasicCameraView", "Error disposing AR service", e)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                createBasicCameraPreview(ctx, onError) { initialized ->
                    cameraInitialized = initialized
                }
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
                            text = "Lip sync and head movement starting...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Camera status indicator
        if (!cameraInitialized) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Blue.copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = "📷 Initializing camera...",
                        modifier = Modifier.padding(12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun createBasicCameraPreview(
    context: Context,
    onError: (String) -> Unit,
    onCameraInitialized: (Boolean) -> Unit
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
            Log.d("BasicCameraView", "Camera surface available: ${width}x${height}")
            initializeBasicCamera(textureView, onError, onCameraInitialized)
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("BasicCameraView", "Camera surface size changed: ${width}x${height}")
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            Log.d("BasicCameraView", "Camera surface destroyed")
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Camera frame updated - smooth operation
        }
    }

    layout.addView(textureView)
    return layout
}

private fun initializeBasicCamera(
    textureView: TextureView,
    onError: (String) -> Unit,
    onCameraInitialized: (Boolean) -> Unit
) {
    try {
        Log.d("BasicCameraView", "Initializing basic camera...")

        val cameraManager = textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList

        if (cameraIdList.isEmpty()) {
            onError("No cameras available")
            return
        }

        val cameraId = cameraIdList[0] // Use back camera
        Log.d("BasicCameraView", "Using camera: $cameraId")

        // Check camera permission
        val permission = android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPermission = textureView.context.checkSelfPermission(android.Manifest.permission.CAMERA) == permission

        if (!hasPermission) {
            onError("Camera permission not granted")
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("BasicCameraView", "Camera opened successfully")
                startBasicCameraPreview(camera, textureView, onCameraInitialized)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.d("BasicCameraView", "Camera disconnected")
                camera.close()
                onCameraInitialized(false)
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("BasicCameraView", "Camera error: $error")
                camera.close()
                onError("Camera error: $error")
                onCameraInitialized(false)
            }
        }, null)

    } catch (e: Exception) {
        Log.e("BasicCameraView", "Failed to initialize camera", e)
        onError("Failed to initialize camera: ${e.message}")
        onCameraInitialized(false)
    }
}

private fun startBasicCameraPreview(
    camera: CameraDevice,
    textureView: TextureView,
    onCameraInitialized: (Boolean) -> Unit
) {
    try {
        val surface = Surface(textureView.surfaceTexture)
        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequest.addTarget(surface)

        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Log.d("BasicCameraView", "Camera session configured")
                try {
                    session.setRepeatingRequest(captureRequest.build(), null, null)
                    Log.d("BasicCameraView", "Camera preview started successfully")
                    onCameraInitialized(true)
                    
                } catch (e: Exception) {
                    Log.e("BasicCameraView", "Failed to start camera preview", e)
                    onCameraInitialized(false)
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("BasicCameraView", "Camera session configuration failed")
                onCameraInitialized(false)
            }
        }, null)

    } catch (e: Exception) {
        Log.e("BasicCameraView", "Failed to start camera preview", e)
        onCameraInitialized(false)
    }
}

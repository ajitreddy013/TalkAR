package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.SimpleARService
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
fun SimpleCameraView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    var cameraInitialized by remember { mutableStateOf(false) }

    // Initialize simple AR service
    val arService = remember { SimpleARService(context) }

    // Collect AR service states
    val trackingState by arService.trackingState.collectAsState()
    val recognitionConfidence by arService.recognitionConfidence.collectAsState()
    val recognizedImages by arService.recognizedImages.collectAsState()
    val error by arService.error.collectAsState()

    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("SimpleCameraView", "Initializing simple AR service...")
                val initialized = arService.initialize()
                Log.d("SimpleCameraView", "Simple AR service initialized: $initialized")
            } catch (e: Exception) {
                Log.e("SimpleCameraView", "Failed to initialize simple AR service", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
            }
        }
    }

    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        recognizedImages.forEach { augmentedImage ->
            Log.d("SimpleCameraView", "Image recognized: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)

            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let {
                Log.d("SimpleCameraView", "Triggering image recognition: ${it.name}")
                onImageRecognized(it)
            }
        }
    }

    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
    }

    // Handle lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            try {
                arService.pauseTracking()
            } catch (e: Exception) {
                Log.e("SimpleCameraView", "Error disposing simple AR service", e)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                createSimpleCameraPreview(ctx, onError) { initialized ->
                    cameraInitialized = initialized
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Simple tracking status overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            SimpleTrackingStatusCard(
                trackingState = trackingState,
                recognitionConfidence = recognitionConfidence
            )
        }

        // Camera viewfinder overlay (center)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Simple viewfinder frame
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                // Simple corner brackets
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cornerLength = 30f
                    val strokeWidth = 4f
                    val cornerColor = when (trackingState) {
                        com.google.ar.core.TrackingState.TRACKING -> Color.Green
                        com.google.ar.core.TrackingState.PAUSED -> Color.Yellow
                        com.google.ar.core.TrackingState.STOPPED -> Color.Red
                    }
                    
                    // Top-left corner
                    drawLine(
                        color = cornerColor,
                        start = Offset(0f, cornerLength),
                        end = Offset(0f, 0f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = cornerColor,
                        start = Offset(0f, 0f),
                        end = Offset(cornerLength, 0f),
                        strokeWidth = strokeWidth
                    )
                    
                    // Top-right corner
                    drawLine(
                        color = cornerColor,
                        start = Offset(size.width - cornerLength, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = cornerColor,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, cornerLength),
                        strokeWidth = strokeWidth
                    )
                    
                    // Bottom-left corner
                    drawLine(
                        color = cornerColor,
                        start = Offset(0f, size.height - cornerLength),
                        end = Offset(0f, size.height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = cornerColor,
                        start = Offset(0f, size.height),
                        end = Offset(cornerLength, size.height),
                        strokeWidth = strokeWidth
                    )
                    
                    // Bottom-right corner
                    drawLine(
                        color = cornerColor,
                        start = Offset(size.width - cornerLength, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
                    drawLine(
                        color = cornerColor,
                        start = Offset(size.width, size.height - cornerLength),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth
                    )
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
                            text = "🎬 Simple AR Active",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Basic image recognition working",
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
                        text = "📷 Initializing simple camera...",
                        modifier = Modifier.padding(12.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SimpleTrackingStatusCard(
    trackingState: com.google.ar.core.TrackingState,
    recognitionConfidence: Float
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        modifier = Modifier.width(250.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Simple AR Status",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tracking State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "State:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = trackingState.name,
                    color = when (trackingState) {
                        com.google.ar.core.TrackingState.TRACKING -> Color.Green
                        com.google.ar.core.TrackingState.PAUSED -> Color.Yellow
                        com.google.ar.core.TrackingState.STOPPED -> Color.Red
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Recognition Confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Confidence:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${(recognitionConfidence * 100).toInt()}%",
                    color = when {
                        recognitionConfidence > 0.8f -> Color.Green
                        recognitionConfidence > 0.5f -> Color.Yellow
                        else -> Color.Red
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun createSimpleCameraPreview(
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
            Log.d("SimpleCameraView", "Camera surface available: ${width}x${height}")
            initializeSimpleCamera(textureView, onError, onCameraInitialized)
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("SimpleCameraView", "Camera surface size changed: ${width}x${height}")
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            Log.d("SimpleCameraView", "Camera surface destroyed")
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Camera frame updated - smooth operation
        }
    }

    layout.addView(textureView)
    return layout
}

private fun initializeSimpleCamera(
    textureView: TextureView,
    onError: (String) -> Unit,
    onCameraInitialized: (Boolean) -> Unit
) {
    try {
        Log.d("SimpleCameraView", "Initializing simple camera...")

        val cameraManager = textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList

        if (cameraIdList.isEmpty()) {
            onError("No cameras available")
            return
        }

        val cameraId = cameraIdList[0] // Use back camera
        Log.d("SimpleCameraView", "Using camera: $cameraId")

        // Check camera permission
        val permission = android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasPermission = textureView.context.checkSelfPermission(android.Manifest.permission.CAMERA) == permission

        if (!hasPermission) {
            onError("Camera permission not granted")
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("SimpleCameraView", "Camera opened successfully")
                startSimpleCameraPreview(camera, textureView, onCameraInitialized)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.d("SimpleCameraView", "Camera disconnected")
                camera.close()
                onCameraInitialized(false)
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("SimpleCameraView", "Camera error: $error")
                camera.close()
                onError("Camera error: $error")
                onCameraInitialized(false)
            }
        }, null)

    } catch (e: Exception) {
        Log.e("SimpleCameraView", "Failed to initialize simple camera", e)
        onError("Failed to initialize camera: ${e.message}")
        onCameraInitialized(false)
    }
}

private fun startSimpleCameraPreview(
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
                Log.d("SimpleCameraView", "Simple camera session configured")
                try {
                    session.setRepeatingRequest(captureRequest.build(), null, null)
                    Log.d("SimpleCameraView", "Simple camera preview started successfully")
                    onCameraInitialized(true)
                    
                } catch (e: Exception) {
                    Log.e("SimpleCameraView", "Failed to start simple camera preview", e)
                    onCameraInitialized(false)
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("SimpleCameraView", "Simple camera session configuration failed")
                onCameraInitialized(false)
            }
        }, null)

    } catch (e: Exception) {
        Log.e("SimpleCameraView", "Failed to start simple camera preview", e)
        onCameraInitialized(false)
    }
}

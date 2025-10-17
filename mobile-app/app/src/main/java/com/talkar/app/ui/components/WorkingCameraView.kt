package com.talkar.app.ui.components

import android.content.Context
import android.hardware.camera2.*
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
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
import com.talkar.app.data.services.ARImageRecognitionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WorkingCameraView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    var cameraInitialized by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Initialize camera when component is created
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("WorkingCameraView", "Initializing camera...")
                delay(100) // Small delay to ensure context is ready
                withContext(Dispatchers.Main) {
                    cameraInitialized = true
                    Log.d("WorkingCameraView", "Camera initialization completed")
                }
            } catch (e: Exception) {
                Log.e("WorkingCameraView", "Failed to initialize camera", e)
                withContext(Dispatchers.Main) {
                    cameraError = "Failed to initialize camera: ${e.message}"
                    onError("Camera error: ${e.message}")
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(
            factory = { ctx ->
                createWorkingCameraPreview(ctx) { error ->
                    cameraError = error
                    onError(error)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Status overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            CameraStatusCard(
                isInitialized = cameraInitialized,
                error = cameraError,
                isImageDetected = isImageDetected
            )
        }

        // Viewfinder overlay (center)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ViewfinderOverlay(
                isImageDetected = isImageDetected,
                isCameraReady = cameraInitialized
            )
        }

        // Error overlay
        cameraError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📷 Camera Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onError,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraStatusCard(
    isInitialized: Boolean,
    error: String?,
    isImageDetected: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Camera Status",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Camera Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Camera:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = when {
                        error != null -> "Error"
                        isInitialized -> "Ready"
                        else -> "Initializing..."
                    },
                    color = when {
                        error != null -> Color.Red
                        isInitialized -> Color.Green
                        else -> Color.Yellow
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Image Detection Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Detection:",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (isImageDetected) "Detected" else "Scanning",
                    color = if (isImageDetected) Color.Green else Color.Yellow,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Error message if any
            error?.let { errorMessage ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ViewfinderOverlay(
    isImageDetected: Boolean,
    isCameraReady: Boolean
) {
    Box(
        modifier = Modifier
            .size(250.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Simple viewfinder frame
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(Color.Transparent)
        ) {
            // Corner brackets
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cornerLength = 30f
                val strokeWidth = 4f
                val cornerColor = when {
                    isImageDetected -> Color.Green
                    isCameraReady -> Color.White
                    else -> Color.Gray
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
        }
        
        // Center text
        Text(
            text = when {
                isImageDetected -> "✅ Image Detected!"
                isCameraReady -> "📷 Point camera at image"
                else -> "🔄 Initializing camera..."
            },
            color = when {
                isImageDetected -> Color.Green
                isCameraReady -> Color.White
                else -> Color.Gray
            },
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

private fun createWorkingCameraPreview(
    context: Context,
    onError: (String) -> Unit
): android.view.View {
    
    val layout = FrameLayout(context).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
    }

    // Create TextureView for camera preview
    val textureView = TextureView(context)
    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("WorkingCameraView", "Camera surface available: ${width}x${height}")
            initializeWorkingCamera(context, textureView, onError)
        }

        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("WorkingCameraView", "Camera surface size changed: ${width}x${height}")
        }

        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            Log.d("WorkingCameraView", "Camera surface destroyed")
            return true
        }

        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // Camera frame updated
        }
    }

    layout.addView(textureView)
    return layout
}

private fun initializeWorkingCamera(
    context: Context,
    textureView: TextureView,
    onError: (String) -> Unit
) {
    try {
        Log.d("WorkingCameraView", "Initializing working camera...")

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList

        if (cameraIdList.isEmpty()) {
            onError("No cameras available")
            return
        }

        // Use back camera (usually index 0)
        val cameraId = cameraIdList[0]
        Log.d("WorkingCameraView", "Using camera: $cameraId")

        // Check camera permission
        val hasPermission = context.checkSelfPermission(android.Manifest.permission.CAMERA) == 
                           android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            onError("Camera permission not granted")
            return
        }

        // Open camera
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.d("WorkingCameraView", "Camera opened successfully")
                startWorkingCameraPreview(camera, textureView, onError)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.d("WorkingCameraView", "Camera disconnected")
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.e("WorkingCameraView", "Camera error: $error")
                camera.close()
                onError("Camera error: $error")
            }
        }, null)

    } catch (e: Exception) {
        Log.e("WorkingCameraView", "Failed to initialize working camera", e)
        onError("Failed to initialize camera: ${e.message}")
    }
}

private fun startWorkingCameraPreview(
    camera: CameraDevice,
    textureView: TextureView,
    onError: (String) -> Unit
) {
    try {
        val surface = Surface(textureView.surfaceTexture)
        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequest.addTarget(surface)

        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                Log.d("WorkingCameraView", "Camera session configured")
                try {
                    session.setRepeatingRequest(captureRequest.build(), null, null)
                    Log.d("WorkingCameraView", "Camera preview started successfully")
                } catch (e: Exception) {
                    Log.e("WorkingCameraView", "Failed to start camera preview", e)
                    onError("Failed to start camera preview: ${e.message}")
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("WorkingCameraView", "Camera session configuration failed")
                onError("Camera session configuration failed")
            }
        }, null)

    } catch (e: Exception) {
        Log.e("WorkingCameraView", "Failed to start working camera preview", e)
        onError("Failed to start camera preview: ${e.message}")
    }
}

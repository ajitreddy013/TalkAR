package com.talkar.app.ui.components

import android.content.Context
import android.hardware.camera2.*
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.launch

@Composable
fun SimpleRealCameraView(
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
        try {
            Log.d("SimpleRealCameraView", "Initializing real camera...")
            cameraInitialized = true
            Log.d("SimpleRealCameraView", "Camera initialization completed")
        } catch (e: Exception) {
            Log.e("SimpleRealCameraView", "Failed to initialize camera", e)
            cameraError = "Failed to initialize camera: ${e.message}"
            onError("Camera error: ${e.message}")
        }
    }

    // Camera cleanup reference to be set by the AndroidView factory
    val cameraCleanupRef = remember { mutableStateOf<(() -> Unit)?>(null) }

    // Handle lifecycle events - ensure camera resources are cleaned up when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            Log.d("SimpleRealCameraView", "Cleaning up camera resources")
            cameraCleanupRef.value?.invoke()
        }
    }

    AndroidView(
        factory = { ctx ->
            createRealCameraView(ctx, onImageRecognized, onError, cameraCleanupRef)
        },
        modifier = modifier.fillMaxSize()
    )

    // Handle errors
    cameraError?.let { errorMessage ->
        onError(errorMessage)
    }
}



private fun createRealCameraView(
    context: Context,
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    cameraCleanupRef: MutableState<(() -> Unit)?>
): android.view.View {
    // Create a layout to hold camera preview
    val layout = android.widget.FrameLayout(context)
    
    // Create TextureView for camera preview
    val textureView = TextureView(context)

    // Holder for camera resources so we can close them later
    val cameraHolder = CameraHolder()

    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("SimpleRealCameraView", "Surface texture available: ${width}x${height}")
            initializeCamera(textureView, onImageRecognized, onError, cameraHolder)
        }
        
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("SimpleRealCameraView", "Surface texture size changed: ${width}x${height}")
        }
        
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            Log.d("SimpleRealCameraView", "Surface texture destroyed - cleaning up camera resources")
            // Ensure we close camera resources when surface is destroyed
            cameraHolder.close()
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // This is called continuously when the camera is working
        }
    }
    
    layout.addView(textureView)

    // Expose cleanup via the composable so it can be invoked on dispose
    cameraCleanupRef.value = {
        try {
            Log.d("SimpleRealCameraView", "Composable requested camera cleanup")
            cameraHolder.close()
        } catch (e: Exception) {
            Log.e("SimpleRealCameraView", "Error during camera cleanup", e)
        }
    }
    
    // Add scanning indicator overlay
    val scanningOverlay = android.widget.TextView(context).apply {
        text = "📷 Point camera at image to scan"
        textSize = 18f
        setTextColor(android.graphics.Color.WHITE)
        gravity = android.view.Gravity.CENTER
        setPadding(20, 20, 20, 20)
        setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
    }
    
    val overlayParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
    overlayParams.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER
    overlayParams.topMargin = 100
    layout.addView(scanningOverlay, overlayParams)
    
    // Add manual detection button (for testing without ARCore)
    val detectButton = android.widget.Button(context).apply {
        text = "🔍 Detect Image"
        setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
        setTextColor(android.graphics.Color.WHITE)
        setPadding(20, 15, 20, 15)
        setOnClickListener {
            Log.d("SimpleRealCameraView", "Manual detection button clicked")
            // For now, simulate detection - in production this would use ML Kit or similar
            simulateImageDetection(onImageRecognized)
        }
    }
    
    val buttonParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
    buttonParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER
    buttonParams.bottomMargin = 100
    layout.addView(detectButton, buttonParams)
    
    return layout
}

private fun initializeCamera(
    textureView: TextureView,
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    cameraHolder: CameraHolder
) {
    try {
        val cameraManager = textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0] // Use first available camera
        
        Log.d("SimpleRealCameraView", "Opening camera: $cameraId")
        
        // Check for camera permission before opening
        val permission = android.content.pm.PackageManager.PERMISSION_GRANTED
        val context = textureView.context
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == permission) {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("SimpleRealCameraView", "Camera opened successfully")

                    try {
                        // Create capture session
                        val surface = Surface(textureView.surfaceTexture)
                        val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        captureRequest.addTarget(surface)

                        // populate holder
                        cameraHolder.cameraDevice = camera
                        cameraHolder.surface = surface

                        camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                Log.d("SimpleRealCameraView", "Camera session configured")
                                try {
                                    cameraHolder.captureSession = session
                                    session.setRepeatingRequest(captureRequest.build(), null, null)
                                    Log.d("SimpleRealCameraView", "Camera preview started")
                                } catch (e: Exception) {
                                    Log.e("SimpleRealCameraView", "Failed to start repeating request", e)
                                    onError("Failed to start camera preview: ${e.message}")
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.e("SimpleRealCameraView", "Camera session configuration failed")
                                onError("Camera session configuration failed")
                            }
                        }, android.os.Handler(android.os.Looper.getMainLooper()))
                    } catch (e: Exception) {
                        Log.e("SimpleRealCameraView", "Failed to create capture session", e)
                        onError("Failed to create capture session: ${e.message}")
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d("SimpleRealCameraView", "Camera disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("SimpleRealCameraView", "Camera error: $error")
                    onError("Camera error: $error")
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        } else {
            Log.e("SimpleRealCameraView", "Camera permission not granted")
            onError("Camera permission not granted")
        }
        
    } catch (e: Exception) {
        Log.e("SimpleRealCameraView", "Failed to initialize camera", e)
        onError("Failed to initialize camera: ${e.message}")
    }
}

private fun simulateImageDetection(onImageRecognized: (ImageRecognition) -> Unit) {
    try {
        Log.d("SimpleRealCameraView", "Simulating image detection for testing")
        
        // Create a mock image recognition result
        val mockImageRecognition = ImageRecognition(
            id = "test-image-001",
            name = "Test Object",
            description = "Detected test object for lip sync",
            imageUrl = "https://example.com/test-image.jpg",
            dialogues = listOf(
                com.talkar.app.data.models.Dialogue(
                    id = "dialogue_001",
                    text = "Hello! I'm a test object. This is a simulated detection.",
                    language = "en",
                    voiceId = "voice_001",
                    emotion = null // Default emotion
                )
            ),
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        )
        
        Log.d("SimpleRealCameraView", "Simulated image recognition: ${mockImageRecognition.name}")
        onImageRecognized(mockImageRecognition)
        
    } catch (e: Exception) {
        Log.e("SimpleRealCameraView", "Error simulating image detection", e)
    }
}

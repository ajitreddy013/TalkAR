package com.talkar.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.camera2.*
import android.util.Log
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
import com.talkar.app.data.services.MLKitRecognitionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors



@Composable
fun MLKitCameraView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    var cameraInitialized by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Initialize ML Kit service
    val mlKitService = remember { MLKitRecognitionService(context) }
    // Use a lifecycle-tied scope so work is cancelled when the composable leaves composition
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize camera when component is created
    LaunchedEffect(Unit) {
        try {
            Log.d("MLKitCameraView", "Initializing ML Kit camera...")
            cameraInitialized = true
            Log.d("MLKitCameraView", "Camera initialization completed")
        } catch (e: Exception) {
            Log.e("MLKitCameraView", "Failed to initialize camera", e)
            cameraError = "Failed to initialize camera: ${e.message}"
            onError("Camera error: ${e.message}")
        }
    }

    // Handle lifecycle events
    // Camera cleanup reference to be set by AndroidView factory
    val cameraCleanupRef = remember { mutableStateOf<(() -> Unit)?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("MLKitCameraView", "Cleaning up ML Kit camera resources")
            mlKitService.cleanup()
            cameraCleanupRef.value?.invoke()
        }
    }

    AndroidView(
        factory = { ctx ->
            createMLKitCameraView(ctx, mlKitService, onImageRecognized, onError, coroutineScope, cameraCleanupRef)
        },
        modifier = modifier.fillMaxSize()
    )

    // Handle errors
    cameraError?.let { errorMessage ->
        onError(errorMessage)
    }
}

private fun createMLKitCameraView(
    context: Context,
    mlKitService: MLKitRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    coroutineScope: CoroutineScope,
    cameraCleanupRef: MutableState<(() -> Unit)?>
) : android.view.View {
    // Create a layout to hold camera preview
    val layout = android.widget.FrameLayout(context)
    
    // Create TextureView for camera preview
    val textureView = TextureView(context)

    val cameraHolder = CameraHolder()

    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("MLKitCameraView", "Surface texture available: ${width}x${height}")
            initializeCamera(textureView, mlKitService, onImageRecognized, onError, cameraHolder)
        }
        
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("MLKitCameraView", "Surface texture size changed: ${width}x${height}")
        }
        
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            Log.d("MLKitCameraView", "Surface texture destroyed - cleaning up camera resources")
            cameraHolder.close()
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // This is called continuously when the camera is working
            // We can use this for real-time recognition
        }
    }
    
    layout.addView(textureView)
    
    // Add scanning indicator overlay
    val scanningOverlay = android.widget.TextView(context).apply {
        text = "🤖 ML Kit Recognition Active\nPoint camera at objects to detect"
        textSize = 16f
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
    
    // Add manual detection button for testing
    val detectButton = android.widget.Button(context).apply {
        text = "🔍 Detect Objects"
        setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        setTextColor(android.graphics.Color.WHITE)
        setPadding(20, 15, 20, 15)
        setOnClickListener {
            Log.d("MLKitCameraView", "Manual ML Kit detection triggered")
            // Capture current frame and run recognition
            captureAndRecognize(textureView, mlKitService, onImageRecognized, onError, coroutineScope)
        }
    }
    
    val buttonParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    )
    buttonParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER
    buttonParams.bottomMargin = 100
    layout.addView(detectButton, buttonParams)
    
    // expose cleanup to composable
    cameraCleanupRef.value = {
        try {
            Log.d("MLKitCameraView", "Composable requested camera cleanup")
            cameraHolder.close()
        } catch (e: Exception) {
            Log.e("MLKitCameraView", "Error during camera cleanup", e)
        }
    }

    return layout
}

private fun initializeCamera(
    textureView: TextureView,
    mlKitService: MLKitRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    cameraHolder: CameraHolder
) {
    try {
        val cameraManager = textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0] // Use first available camera
        
        Log.d("MLKitCameraView", "Opening camera: $cameraId")
        
        // Check for camera permission before opening
        val permission = android.content.pm.PackageManager.PERMISSION_GRANTED
        val context = textureView.context
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == permission) {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("MLKitCameraView", "Camera opened successfully")

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
                                Log.d("MLKitCameraView", "Camera session configured")
                                try {
                                    cameraHolder.captureSession = session
                                    session.setRepeatingRequest(captureRequest.build(), null, null)
                                    Log.d("MLKitCameraView", "ML Kit camera preview started")
                                } catch (e: Exception) {
                                    Log.e("MLKitCameraView", "Failed to start repeating request", e)
                                    onError("Failed to start camera preview: ${e.message}")
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.e("MLKitCameraView", "Camera session configuration failed")
                                onError("Camera session configuration failed")
                            }
                        }, android.os.Handler(android.os.Looper.getMainLooper()))
                    } catch (e: Exception) {
                        Log.e("MLKitCameraView", "Failed to create capture session", e)
                        onError("Failed to create capture session: ${e.message}")
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d("MLKitCameraView", "Camera disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("MLKitCameraView", "Camera error: $error")
                    onError("Camera error: $error")
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        } else {
            Log.e("MLKitCameraView", "Camera permission not granted")
            onError("Camera permission not granted")
        }
        
    } catch (e: Exception) {
        Log.e("MLKitCameraView", "Failed to initialize camera", e)
        onError("Failed to initialize camera: ${e.message}")
    }
}

private fun captureAndRecognize(
    textureView: TextureView,
    mlKitService: MLKitRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    coroutineScope: CoroutineScope
) {
    try {
        Log.d("MLKitCameraView", "Capturing frame for ML Kit recognition")
        
        // Capture current frame from TextureView
        val bitmap = textureView.getBitmap()
        if (bitmap != null) {
            Log.d("MLKitCameraView", "Frame captured: ${bitmap.width}x${bitmap.height}")
            
            // Run ML Kit recognition in background using lifecycle-tied scope
            coroutineScope.launch {
                try {
                    val recognitionResult = withContext(Dispatchers.Default) {
                        mlKitService.recognizeImage(bitmap)
                    }

                    Log.d("MLKitCameraView", "ML Kit recognition completed")
                    Log.d("MLKitCameraView", "Primary label: ${recognitionResult.primaryLabel}")
                    Log.d("MLKitCameraView", "Confidence: ${recognitionResult.confidence}")
                    Log.d("MLKitCameraView", "Processing time: ${recognitionResult.processingTimeMs}ms")

                    // Convert to ImageRecognition model on background dispatcher
                    val imageRecognition = withContext(Dispatchers.Default) {
                        mlKitService.convertToImageRecognition(recognitionResult)
                    }

                    // Post result back on main thread
                    withContext(Dispatchers.Main) {
                        onImageRecognized(imageRecognition)
                    }

                } catch (e: Exception) {
                    Log.e("MLKitCameraView", "ML Kit recognition failed", e)
                    withContext(Dispatchers.Main) {
                        onError("Recognition failed: ${e.message}")
                    }
                }
            }
        } else {
            Log.w("MLKitCameraView", "Failed to capture frame from TextureView")
            onError("Failed to capture camera frame")
        }
        
    } catch (e: Exception) {
        Log.e("MLKitCameraView", "Error during frame capture", e)
        onError("Frame capture failed: ${e.message}")
    }
}

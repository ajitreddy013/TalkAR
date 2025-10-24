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
import com.talkar.app.data.models.TalkingHeadVideo
import com.talkar.app.data.services.MLKitRecognitionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun AROverlayCameraView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    talkingHeadVideo: TalkingHeadVideo?,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    var cameraInitialized by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Initialize ML Kit service
    val mlKitService = remember { MLKitRecognitionService(context) }
    // Lifecycle-tied scope so background work is cancelled when composable leaves
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize camera when component is created
    LaunchedEffect(Unit) {
        try {
            Log.d("AROverlayCameraView", "Initializing AR overlay camera...")
            cameraInitialized = true
            Log.d("AROverlayCameraView", "Camera initialization completed")
        } catch (e: Exception) {
            Log.e("AROverlayCameraView", "Failed to initialize camera", e)
            cameraError = "Failed to initialize camera: ${e.message}"
            onError("Camera error: ${e.message}")
        }
    }

    // Handle lifecycle events
    // Camera cleanup reference to be set by AndroidView factory
    val cameraCleanupRef = remember { mutableStateOf<(() -> Unit)?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            Log.d("AROverlayCameraView", "Cleaning up AR overlay camera resources")
            mlKitService.cleanup()
            cameraCleanupRef.value?.invoke()
        }
    }

    AndroidView(
        factory = { ctx ->
            createAROverlayCameraView(ctx, mlKitService, onImageRecognized, onError, talkingHeadVideo, coroutineScope, cameraCleanupRef)
        },
        modifier = modifier.fillMaxSize()
    )

    // Handle errors
    cameraError?.let { errorMessage ->
        onError(errorMessage)
    }
}

private fun createAROverlayCameraView(
    context: Context,
    mlKitService: MLKitRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    talkingHeadVideo: TalkingHeadVideo?,
    coroutineScope: CoroutineScope,
    cameraCleanupRef: MutableState<(() -> Unit)?>
) : android.view.View {
    // Create a layout to hold camera preview and AR overlay
    val layout = android.widget.FrameLayout(context)
    
    // Create TextureView for camera preview
    val textureView = TextureView(context)

    val cameraHolder = CameraHolder()

    textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("AROverlayCameraView", "Surface texture available: ${width}x${height}")
            initializeCamera(textureView, mlKitService, onImageRecognized, onError, cameraHolder)
        }
        
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            Log.d("AROverlayCameraView", "Surface texture size changed: ${width}x${height}")
        }
        
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            Log.d("AROverlayCameraView", "Surface texture destroyed - cleaning up camera resources")
            cameraHolder.close()
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // This is called continuously when the camera is working
        }
    }
    
    layout.addView(textureView)
    
    // Add AR overlay for talking head video
    val arOverlay = createAROverlay(context, talkingHeadVideo)
    layout.addView(arOverlay)
    
    // Add scanning indicator overlay
    val scanningOverlay = android.widget.TextView(context).apply {
        text = "🤖 AR Recognition Active\nPoint camera at objects to detect"
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
            Log.d("AROverlayCameraView", "Manual AR detection triggered")
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
            Log.d("AROverlayCameraView", "Composable requested camera cleanup")
            cameraHolder.close()
        } catch (e: Exception) {
            Log.e("AROverlayCameraView", "Error during camera cleanup", e)
        }
    }

    return layout
}

private fun createAROverlay(context: Context, talkingHeadVideo: TalkingHeadVideo?): android.view.View {
    val overlayView = android.widget.FrameLayout(context)
    
    if (talkingHeadVideo != null) {
        Log.d("AROverlayCameraView", "Creating AR overlay for video: ${talkingHeadVideo.title}")
        
        // Create video player for talking head video
        val videoView = android.widget.VideoView(context)
        videoView.setVideoPath(talkingHeadVideo.videoUrl)
        videoView.setOnPreparedListener { mediaPlayer ->
            mediaPlayer.isLooping = true
            mediaPlayer.start()
            Log.d("AROverlayCameraView", "Talking head video started: ${talkingHeadVideo.title}")
        }
        
        // Position the video overlay in the center of the screen
        val videoParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        videoParams.gravity = android.view.Gravity.CENTER
        overlayView.addView(videoView, videoParams)
        
        // Add semi-transparent overlay to blend with camera
        val blendOverlay = android.view.View(context).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#40000000"))
        }
        overlayView.addView(blendOverlay, videoParams)
        
        // Add talking head info overlay
        val infoOverlay = android.widget.TextView(context).apply {
            text = "🎭 ${talkingHeadVideo.title}\n📝 ${talkingHeadVideo.description}"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER
            setPadding(20, 20, 20, 20)
            setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
        }
        
        val infoParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        infoParams.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER
        infoParams.bottomMargin = 200
        overlayView.addView(infoOverlay, infoParams)
        
    } else {
        Log.d("AROverlayCameraView", "No talking head video to overlay")
    }
    
    return overlayView
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
        
        Log.d("AROverlayCameraView", "Opening camera: $cameraId")
        
        // Check for camera permission before opening
        val context = textureView.context
        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("AROverlayCameraView", "Camera opened successfully")

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
                                Log.d("AROverlayCameraView", "Camera session configured")
                                try {
                                    cameraHolder.captureSession = session
                                    session.setRepeatingRequest(captureRequest.build(), null, null)
                                    Log.d("AROverlayCameraView", "AR overlay camera preview started")
                                } catch (e: Exception) {
                                    Log.e("AROverlayCameraView", "Failed to start repeating request", e)
                                    onError("Failed to start camera preview: ${e.message}")
                                }
                            }

                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                Log.e("AROverlayCameraView", "Camera session configuration failed")
                                onError("Camera session configuration failed")
                            }
                        }, android.os.Handler(android.os.Looper.getMainLooper()))
                    } catch (e: Exception) {
                        Log.e("AROverlayCameraView", "Failed to create capture session", e)
                        onError("Failed to create capture session: ${e.message}")
                    }
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d("AROverlayCameraView", "Camera disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("AROverlayCameraView", "Camera error: $error")
                    onError("Camera error: $error")
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
        } else {
            Log.e("AROverlayCameraView", "Camera permission not granted")
            onError("Camera permission not granted")
        }
        
    } catch (e: Exception) {
        Log.e("AROverlayCameraView", "Failed to initialize camera", e)
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
        Log.d("AROverlayCameraView", "Capturing frame for AR recognition")
        
        // Capture current frame from TextureView
        val bitmap = textureView.getBitmap()
        if (bitmap != null) {
            Log.d("AROverlayCameraView", "Frame captured: ${bitmap.width}x${bitmap.height}")
            
            // Run ML Kit recognition in background using lifecycle-tied scope
            coroutineScope.launch {
                try {
                    val recognitionResult = withContext(Dispatchers.Default) {
                        mlKitService.recognizeImage(bitmap)
                    }

                    Log.d("AROverlayCameraView", "AR recognition completed")
                    Log.d("AROverlayCameraView", "Primary label: ${recognitionResult.primaryLabel}")
                    Log.d("AROverlayCameraView", "Confidence: ${recognitionResult.confidence}")
                    Log.d("AROverlayCameraView", "Processing time: ${recognitionResult.processingTimeMs}ms")

                    val imageRecognition = withContext(Dispatchers.Default) {
                        mlKitService.convertToImageRecognition(recognitionResult)
                    }

                    withContext(Dispatchers.Main) {
                        onImageRecognized(imageRecognition)
                    }

                } catch (e: Exception) {
                    Log.e("AROverlayCameraView", "AR recognition failed", e)
                    withContext(Dispatchers.Main) {
                        onError("Recognition failed: ${e.message}")
                    }
                }
            }
        } else {
            Log.w("AROverlayCameraView", "Failed to capture frame from TextureView")
            onError("Failed to capture camera frame")
        }
        
    } catch (e: Exception) {
        Log.e("AROverlayCameraView", "Error during frame capture", e)
        onError("Frame capture failed: ${e.message}")
    }
}

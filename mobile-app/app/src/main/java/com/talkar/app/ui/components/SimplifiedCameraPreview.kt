package com.talkar.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.FaceLipDetectorService
import com.talkar.app.data.services.ImageMatcherService
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Simplified Camera Preview using CameraX
 * Shows camera feed and performs real-time image recognition
 */
@Composable
fun SimplifiedCameraPreview(
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
    imageMatcher: ImageMatcherService, // 🔥 Shared instance
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    captureController: CaptureController? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Initialize face detector service
    val faceDetector = remember { FaceLipDetectorService() }
    
    var isHybridMatched by remember { mutableStateOf(false) }
    var isFaceDetected by remember { mutableStateOf(false) }
    var isImageMatched by remember { mutableStateOf(false) }
    var isTemplatesLoading by remember { mutableStateOf(true) } // 🔥 New loading state
    val haptic = LocalHapticFeedback.current
    
    // Search timing logic
    var searchStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var currentSessionMatched by remember { mutableStateOf(false) }
    
    // 🔥 New Guidance Logic Variable
    var guidanceMessage by remember { mutableStateOf("") }
    
    // Load reference images from backend on startup
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            Log.d("SimplifiedCamera", "Loading reference images for hybrid matching...")
            isTemplatesLoading = true // Start loading
            
            // Note: Fallback templates are already pre-loaded in ImageMatcherService init
            // This call now tries to fetch fresh ones from backend with 5s timeout
            val result = imageMatcher.loadTemplatesFromBackend()
            
            result.fold(
                onSuccess = { count -> 
                    Log.d("SimplifiedCamera", "Loaded $count reference images") 
                },
                onFailure = { error -> 
                    Log.e("SimplifiedCamera", "Failed to refresh images (using fallbacks)", error)
                    // No error UI needed as fallbacks are active
                }
            )
            isTemplatesLoading = false // Done loading
        }
    }

    // 🔥 Guidance Logic Loop
    LaunchedEffect(isFaceDetected, isHybridMatched) {
        if (isHybridMatched) {
            guidanceMessage = ""
            return@LaunchedEffect
        }
        
        val startTime = System.currentTimeMillis()
        while(isActive && isFaceDetected && !isHybridMatched) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed > 7000) { // > 7 seconds
                 guidanceMessage = "Move closer to the product"
            } else if (elapsed > 12000) {
                 guidanceMessage = "Ensure product is centered"
            }
            kotlinx.coroutines.delay(1000)
        }
        if (!isFaceDetected) guidanceMessage = ""
    }

    // Launcher for camera permission
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onError("Camera permission is required for face detection")
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
            
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            // Preview use case
                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            // Image analysis use case for face/lip detection
                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            
                                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                                                if (isPaused || isImageMatched) {
                                                    imageProxy.close()
                                                    return@launch
                                                }
                                                
                                                try {
                                                    // Convert to Bitmap and ROTATE for ImageMatcher
                                                    val originalBitmap = imageProxy.toBitmap()
                                                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                                    val bitmap = if (rotationDegrees != 0) {
                                                        val matrix = android.graphics.Matrix()
                                                        matrix.postRotate(rotationDegrees.toFloat())
                                                        android.graphics.Bitmap.createBitmap(
                                                            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                                                        )
                                                    } else {
                                                        originalBitmap
                                                    }
                                                    
                                                    // Run both detections
                                                    val faceResult = faceDetector.detectFaceAndLips(image)
                                                    val matchResult = imageMatcher.matchFrame(bitmap)
                                                    
                                                    // Update UI states on main thread
                                                    launch(kotlinx.coroutines.Dispatchers.Main) {
                                                        isFaceDetected = faceResult.hasFace
                                                        isImageMatched = matchResult != null
                                                    }
                                                    
                                                    // Granular telemetry for diagnosis
                                                    val faceInfo = if (faceResult.hasFace) {
                                                        "Face[Bounds=${faceResult.faceBounds}, Mouth=${faceResult.mouthOutline.size} pts]"
                                                    } else "NoFace"
                                                    
                                                    val matchInfo = if (matchResult != null) {
                                                        val source = if (imageMatcher.isUsingFallback) "Local" else "Backend"
                                                        "Match[ID=${matchResult.imageId}, Name=${matchResult.imageName}, Source=$source, Score=${String.format("%.2f", matchResult.confidence)}]"
                                                    } else {
                                                        val templateCount = imageMatcher.getTemplateCount()
                                                        val source = if (imageMatcher.isUsingFallback) "Local" else "Backend"
                                                        "NoMatch[Templates=$templateCount, Source=$source]"
                                                    }
                                                    
                                                    Log.d("SimplifiedCamera", "Diagnostics: $faceInfo, $matchInfo")
                                                    
                                                    // Only "Matched" if image is found (Face is optional)
                                                    val isMatched = matchResult != null
                                                    isHybridMatched = isMatched
                                                    
                                                    if (isMatched && matchResult != null) {
                                                        launch(kotlinx.coroutines.Dispatchers.Main) {
                                                            if (!currentSessionMatched) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                currentSessionMatched = true
                                                            }
                                                        }
                                                        
                                                        Log.i("SimplifiedCamera", "MATCH SUCCESS: ${matchResult.imageName} (Confidence: ${String.format("%.2f", matchResult.confidence)})")
                                                        
                                                        launch(kotlinx.coroutines.Dispatchers.Main) {
                                                            val recognition = ImageRecognition(
                                                                id = matchResult.imageId,
                                                                imageUrl = "",
                                                                name = matchResult.imageName,
                                                                description = matchResult.description,
                                                                dialogues = (matchResult.dialogues as? List<com.talkar.app.data.models.Dialogue>) ?: emptyList(),
                                                                createdAt = System.currentTimeMillis().toString(),
                                                                updatedAt = System.currentTimeMillis().toString()
                                                            )
                                                            onImageRecognized(recognition)
                                                        }
                                                    } else if (faceResult.hasFace && matchResult == null) {
                                                        Log.v("SimplifiedCamera", "FACE TRACKED: Waiting for high-confidence image match...")
                                                    }
                                                    
                                                    if (bitmap != originalBitmap) bitmap.recycle()
                                                    originalBitmap.recycle()
                                                } catch (e: Exception) {
                                                    Log.e("SimplifiedCamera", "Hybrid detection error", e)
                                                } finally {
                                                    imageProxy.close()
                                                }
                                            }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            // Image Capture use case
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            // Link the controller to this capture instance
                            captureController?.captureImage = { onResult: (Bitmap) -> Unit ->
                                imageCapture.takePicture(
                                    ContextCompat.getMainExecutor(ctx),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            try {
                                                val bitmap = image.toBitmap() // Uses existing extension
                                                val rotation = image.imageInfo.rotationDegrees
                                                val finalBitmap = if (rotation != 0) {
                                                    val matrix = android.graphics.Matrix()
                                                    matrix.postRotate(rotation.toFloat())
                                                    android.graphics.Bitmap.createBitmap(
                                                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                                    )
                                                } else {
                                                    bitmap
                                                }
                                                onResult(finalBitmap)
                                            } catch (e: Exception) {
                                                Log.e("SimplifiedCamera", "Failed to process captured image", e)
                                            } finally {
                                                image.close()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("SimplifiedCamera", "Image capture failed", exception)
                                        }
                                    }
                                )
                            }

                            // Select back camera
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                // Unbind all use cases before rebinding
                                cameraProvider.unbindAll()

                                // Bind use cases to camera
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalyzer,
                                    imageCapture
                                )

                                Log.d("SimplifiedCamera", "Camera started successfully with image recognition & capture")
                            } catch (exc: Exception) {
                                Log.e("SimplifiedCamera", "Use case binding failed", exc)
                                onError("Failed to start camera: ${exc.message}")
                            }
                        } catch (exc: Exception) {
                            Log.e("SimplifiedCamera", "Camera provider initialization failed", exc)
                            onError("Camera initialization failed: ${exc.message}")
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Google Lens Style Corners
            ScanningOverlay(isMatched = isHybridMatched)
            
            // Backend Status Overlay
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (imageMatcher.isUsingFallback) Color(0xFFFF9800) else Color(0xFF4CAF50))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (imageMatcher.isUsingFallback) "OFFLINE MODE" else "BACKEND ONLINE",
                    color = Color.White,
                    fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                    fontWeight = FontWeight.Bold
                )
            }

            // Visual indicator for face detection
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHybridMatched) Color(0xFF4CAF50) else Color(0xFF2196F3))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isFaceDetected) Color.White else Color.White.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FACE DETECTOR",
                        color = Color.White,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                        fontWeight = if (isFaceDetected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // 🔥 Loading Indicator
            if (isTemplatesLoading) {
                 Box(
                    modifier = Modifier
                        .padding(top = 60.dp)
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Loading resources...",
                            color = Color.White,
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 🔥 Guidance Messages
            if (guidanceMessage.isNotEmpty() && !isTemplatesLoading) {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 100.dp)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = guidanceMessage,
                        color = Color.Yellow, // High visibility
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 18.sp // Larger text
                    )
                }
            }
            
            DisposableEffect(Unit) {
                onDispose {
                    faceDetector.stop()
                    imageMatcher.clearTemplates()
                    cameraExecutor.shutdown()
                }
            }
        } else {
            // Show message when permission is not granted
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera permission required")
            }
        }
    }
}

/**
 * Extension function to convert ImageProxy to Bitmap
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun ImageProxy.toBitmap(): Bitmap {
    val image = this.image ?: throw IllegalStateException("Image is null")
    val planes = image.planes
    val buffer = planes[0].buffer
    val pixelStride = planes[0].pixelStride
    val rowStride = planes[0].rowStride
    val rowPadding = rowStride - pixelStride * width

    val bitmap = Bitmap.createBitmap(
        width + rowPadding / pixelStride,
        height,
        Bitmap.Config.ARGB_8888
    )
    bitmap.copyPixelsFromBuffer(buffer)
    
    // Crop if there's row padding
    return if (rowPadding == 0) {
        bitmap
    } else {
        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
        bitmap.recycle()
        croppedBitmap
    }
}

/**
 * Google Lens style corners that turn green on match
 */
@Composable
fun ScanningOverlay(isMatched: Boolean) {
    val cornerColor = if (isMatched) Color(0xFF4CAF50) else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(280.dp)) {
            val strokeWidth = 8f
            val cornerLength = 60f
            
            // Top Left
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(cornerLength, 0f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(0f, cornerLength),
                strokeWidth = strokeWidth
            )
            
            // Top Right
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width - cornerLength, 0f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, cornerLength),
                strokeWidth = strokeWidth
            )
            
            // Bottom Left
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                end = androidx.compose.ui.geometry.Offset(cornerLength, size.height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(0f, size.height),
                end = androidx.compose.ui.geometry.Offset(0f, size.height - cornerLength),
                strokeWidth = strokeWidth
            )
            
            // Bottom Right
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(size.width, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width - cornerLength, size.height),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = cornerColor,
                start = androidx.compose.ui.geometry.Offset(size.width, size.height),
                end = androidx.compose.ui.geometry.Offset(size.width, size.height - cornerLength),
                strokeWidth = strokeWidth
            )
        }
    }
}

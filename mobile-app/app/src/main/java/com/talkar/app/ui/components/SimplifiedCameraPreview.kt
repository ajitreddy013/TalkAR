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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Simplified Camera Preview using CameraX
 * Shows camera feed and performs real-time image recognition
 */
@Composable
fun SimplifiedCameraPreview(
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
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

    // Initialize face and image matcher services
    val faceDetector = remember { FaceLipDetectorService() }
    val imageMatcher = remember { ImageMatcherService(context) }
    
    var isHybridMatched by remember { mutableStateOf(false) }
    var isFaceDetected by remember { mutableStateOf(false) }
    var isImageMatched by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    // Search timing logic
    var searchStartTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var currentSessionMatched by remember { mutableStateOf(false) }
    val elapsedSeconds = remember { derivedStateOf { (System.currentTimeMillis() - searchStartTime) / 1000 } }

    // Load reference images from backend on startup
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            Log.d("SimplifiedCamera", "Loading reference images for hybrid matching...")
            val result = imageMatcher.loadTemplatesFromBackend()
            result.fold(
                onSuccess = { count -> Log.d("SimplifiedCamera", "Loaded $count reference images") },
                onFailure = { error -> 
                    Log.e("SimplifiedCamera", "Failed to load images", error)
                    onError("Failed to load images: ${error.message}")
                }
            )
        }
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
                                            
                                            coroutineScope.launch {
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
                                                    
                                                    // Detailed state update
                                                    isFaceDetected = faceResult.hasFace
                                                    isImageMatched = matchResult != null
                                                    
                                                    // Logging for diagnosis
                                                    Log.d("SimplifiedCamera", "Diagnostics: Face=${faceResult.hasFace}, Match=${matchResult != null}")
                                                    
                                                    // Only "Matched" if both are found
                                                    val isMatched = faceResult.hasFace && matchResult != null
                                                    isHybridMatched = isMatched
                                                    
                                                    if (isMatched && matchResult != null) {
                                                        if (!currentSessionMatched) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            currentSessionMatched = true
                                                        }
                                                        
                                                        Log.i("SimplifiedCamera", "HYBRID SUCCESS: Face + ${matchResult.imageName}")
                                                        
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
                                                    } else if (matchResult != null && !faceResult.hasFace) {
                                                        Log.w("SimplifiedCamera", "MATCH DISCARDED: Found ${matchResult.imageName} BUT NO FACE DETECTED.")
                                                    } else if (faceResult.hasFace && matchResult == null) {
                                                        Log.v("SimplifiedCamera", "FACE DETECTED BUT NO MATCH FOUND in backend.")
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
                                    imageAnalyzer
                                )

                                Log.d("SimplifiedCamera", "Camera started successfully with image recognition")
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
            
            // Visual indicator for face detection
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isHybridMatched) Color(0xFF4CAF50) else Color(0xFFF44336))
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

            // Guidance messages for search timeout
            if (!isHybridMatched) {
                var guidanceText by remember { mutableStateOf("") }
                
                LaunchedEffect(Unit) {
                    while (true) {
                        val elapsed = (System.currentTimeMillis() - searchStartTime) / 1000
                        guidanceText = when {
                            elapsed > 12 -> "No match found yet. Try another character."
                            elapsed > 5 -> "Keep steady and ensure good lighting..."
                            else -> ""
                        }
                        kotlinx.coroutines.delay(1000)
                    }
                }

                if (guidanceText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 80.dp)
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 32.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = guidanceText,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                // Reset search timer on match so if it's lost it starts fresh
                SideEffect {
                    searchStartTime = System.currentTimeMillis()
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

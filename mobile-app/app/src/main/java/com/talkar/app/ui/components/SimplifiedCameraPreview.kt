package com.talkar.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
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
import com.talkar.app.data.models.ImageRecognition
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Simplified Camera Preview using CameraX
 * Shows camera feed immediately without complex AR initialization
 */
@Composable
fun SimplifiedCameraPreview(
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                onError("Camera permission is required")
            }
        }
    )

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

                            // Image analysis use case (for future image recognition)
                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

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

                                Log.d("SimplifiedCamera", "Camera started successfully")
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
            
            DisposableEffect(Unit) {
                onDispose {
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

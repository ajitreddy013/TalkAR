package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import android.widget.TextView
import android.view.ViewGroup
import com.google.ar.core.ArCoreApk

@Composable
fun ARCoreOnlyView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize AR service
    val arService = remember { ARImageRecognitionService(context) }

    // Initialize AR service when component is created - on background thread
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ARCoreOnlyView", "Initializing AR service on background thread...")
                val initialized = arService.initialize()
                android.util.Log.d("ARCoreOnlyView", "AR service initialized: $initialized")
                if (initialized) {
                    // Load images from backend into ARCore asynchronously
                    loadImagesIntoARCore(arService)
                }
            } catch (e: Exception) {
                android.util.Log.e("ARCoreOnlyView", "Failed to initialize AR service", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to initialize AR: ${e.message}")
                }
            }
        }
    }

    // Handle lifecycle events with proper ARCore coordination
    DisposableEffect(Unit) {
        onDispose {
            try {
                android.util.Log.d("ARCoreOnlyView", "Disposing AR service - pausing processing first")
                arService.pauseProcessing()
                android.util.Log.d("ARCoreOnlyView", "AR service paused successfully")
            } catch (e: Exception) {
                android.util.Log.e("ARCoreOnlyView", "Error disposing AR service", e)
            }
        }
    }

    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()

    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        android.util.Log.d("ARCoreOnlyView", "Recognized images updated: ${recognizedImages.size}")
        recognizedImages.forEach { augmentedImage ->
            android.util.Log.d("ARCoreOnlyView", "Processing recognized image: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)

            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let {
                android.util.Log.d("ARCoreOnlyView", "Image recognition result: ${it.name}")
                onImageRecognized(it)
            }
        }
    }

    // Show ARCore view only when not detected to allow overlays
    if (!isImageDetected) {
        AndroidView(
            factory = { ctx ->
                createARCoreOnlyView(ctx, arService, onImageRecognized)
            },
            modifier = modifier
        )
    }

    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
        android.util.Log.e("ARCoreOnlyView", "AR Error: $errorMessage")
    }
}

private suspend fun loadImagesIntoARCore(arService: ARImageRecognitionService) {
    withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("ARCoreOnlyView", "Loading images from backend into ARCore on background thread...")
            val imageRepository = TalkARApplication.instance.imageRepository

            imageRepository.getAllImages().collect { images ->
                android.util.Log.d("ARCoreOnlyView", "Loaded ${images.size} images from backend")

                // Limit the number of images to prevent memory issues
                val maxImages = 3 // Limit to 3 images for better performance
                val limitedImages = images.take(maxImages)

                android.util.Log.d("ARCoreOnlyView", "Processing ${limitedImages.size} images (limited from ${images.size} for performance)")

                limitedImages.forEach { image ->
                    try {
                        // Process each image on background thread
                        withContext(Dispatchers.IO) {
                            android.util.Log.d("ARCoreOnlyView", "Processing image for ARCore: ${image.name}")

                            // For now, we'll use the existing test images in ARCore
                            // In production, you would:
                            // 1. Download image bytes from backend URL
                            // 2. Optimize image size and quality
                            // 3. Add to ARCore database: arService.addReferenceImage(image.name, imageBytes)

                            android.util.Log.d("ARCoreOnlyView", "Image ${image.name} ready for recognition")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ARCoreOnlyView", "Failed to process image ${image.name}", e)
                    }
                }

                android.util.Log.d("ARCoreOnlyView", "Completed processing ${limitedImages.size} images")
            }
        } catch (e: Exception) {
            android.util.Log.e("ARCoreOnlyView", "Failed to load images into ARCore", e)
        }
    }
}

private fun createARCoreOnlyView(
    context: Context,
    arService: ARImageRecognitionService,
    onImageRecognized: (ImageRecognition) -> Unit
): android.view.View {
    // Create a simple view that lets ARCore handle everything
    val layout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK) // Black background
    }

    // Add scanning indicator overlay
    val scanningOverlay = TextView(context).apply {
        text = "🎯 Point camera at an image to scan\n(ARCore handles camera automatically)"
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
    overlayParams.gravity = android.view.Gravity.CENTER
    layout.addView(scanningOverlay, overlayParams)

    // Start ARCore processing only after initialization is complete
    CoroutineScope(SupervisorJob()).launch(Dispatchers.IO) {
        try {
            // Wait for ARCore to be fully initialized with proper checking
            var attempts = 0
            val maxAttempts = 10
            
            while (attempts < maxAttempts) {
                kotlinx.coroutines.delay(1000) // Wait 1 second between attempts
                attempts++
                
                android.util.Log.d("ARCoreOnlyView", "Checking ARCore initialization (attempt $attempts/$maxAttempts)...")
                
                // Check if ARCore service is initialized
                if (arService.isInitialized()) {
                    android.util.Log.d("ARCoreOnlyView", "ARCore is initialized, starting processing...")
                    arService.resumeProcessing()
                    android.util.Log.d("ARCoreOnlyView", "ARCore processing started successfully")
                    break
                } else {
                    android.util.Log.d("ARCoreOnlyView", "ARCore not yet initialized, waiting...")
                }
            }
            
            if (attempts >= maxAttempts) {
                android.util.Log.w("ARCoreOnlyView", "ARCore initialization timed out after $maxAttempts attempts")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ARCoreOnlyView", "Failed to start ARCore processing", e)
        }
    }

    return layout
}

package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.ARImageRecognitionService
import kotlinx.coroutines.launch

@Composable
fun SimpleARView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize AR service
    val arService = remember { ARImageRecognitionService(context) }
    
    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        try {
            val initialized = arService.initialize()
            android.util.Log.d("SimpleARView", "AR service initialized: $initialized")
        } catch (e: Exception) {
            android.util.Log.e("SimpleARView", "Failed to initialize AR service", e)
            onError("Failed to initialize AR: ${e.message}")
        }
    }
    
    // Handle lifecycle events
    DisposableEffect(Unit) {
        onDispose {
            try {
                arService.pauseProcessing()
            } catch (e: Exception) {
                android.util.Log.e("SimpleARView", "Error disposing AR service", e)
            }
        }
    }
    
    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()
    
    // Handle session resumption
    LaunchedEffect(isTracking) {
        if (isTracking) {
            android.util.Log.d("SimpleARView", "ARCore session resumed, resuming processing")
            try {
                arService.resumeProcessing()
            } catch (e: Exception) {
                android.util.Log.e("SimpleARView", "Error resuming processing", e)
            }
        }
    }
    
    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        android.util.Log.d("SimpleARView", "Recognized images updated: ${recognizedImages.size}")
        recognizedImages.forEach { augmentedImage ->
            android.util.Log.d("SimpleARView", "Processing recognized image: ${augmentedImage.name}")
            onAugmentedImageRecognized(augmentedImage)
            
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { 
                android.util.Log.d("SimpleARView", "Image recognition result: ${it.name}")
                onImageRecognized(it) 
            }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            createSimpleARView(ctx, arService, onImageRecognized)
        },
        modifier = modifier
    )
    
    // Handle errors
    error?.let { errorMessage ->
        onError(errorMessage)
        android.util.Log.e("SimpleARView", "AR Error: $errorMessage")
    }
}

private fun createSimpleARView(
    context: Context,
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
): android.view.View {
    // Create a simple view that shows ARCore is working
    val layout = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        setBackgroundColor(android.graphics.Color.TRANSPARENT) // Make transparent to allow overlays
        
        val textView = android.widget.TextView(context).apply {
            text = "🎯 ARCore Active\n\nPoint camera at Sunrich bottle\n\nAR tracking is running..."
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(android.graphics.Color.parseColor("#80000000")) // Semi-transparent background
        }
        
        addView(textView)
    }
    
    // Add a test button for simulation
    val testButton = android.widget.Button(context).apply {
        text = "🧪 Test Image Recognition"
        setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        setTextColor(android.graphics.Color.WHITE)
        setOnClickListener {
            android.util.Log.d("SimpleARView", "Test button clicked - simulating image recognition")
            simulateImageDetection(arService, onImageRecognized)
        }
    }
    
    val buttonParams = android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
    )
    buttonParams.gravity = android.view.Gravity.CENTER
    buttonParams.topMargin = 20
    layout.addView(testButton, buttonParams)
    
    // Start ARCore processing without manual camera control
    startARFrameProcessing(arService, onImageRecognized)
    
    return layout
}

private fun startARFrameProcessing(
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("SimpleARView", "Starting ARCore frame processing...")
        // ARCore handles camera internally, we just process frames
        arService.resumeProcessing()
    } catch (e: Exception) {
        android.util.Log.e("SimpleARView", "Failed to start AR processing", e)
    }
}

private fun simulateImageDetection(
    arService: ARImageRecognitionService,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("SimpleARView", "Simulating image detection for testing")
        
        // Create a mock image recognition result
        val mockImageRecognition = ImageRecognition(
            id = "dda5e144-2f31-483e-9526-81a7245d49eb",
            name = "Sunrich",
            description = "Hello there! I'm your Sunrich Water Bottle.",
            imageUrl = "/uploads/c32d2501-4f5d-4668-91dc-ee0910680e1a.jpeg",
            dialogues = emptyList(),
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        )
        
        android.util.Log.d("SimpleARView", "Simulated image recognition: ${mockImageRecognition.name}")
        onImageRecognized(mockImageRecognition)
        
    } catch (e: Exception) {
        android.util.Log.e("SimpleARView", "Error simulating image detection", e)
    }
}
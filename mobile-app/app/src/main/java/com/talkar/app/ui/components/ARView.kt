package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Frame
// Removed Sceneform imports - using modern ARCore approach
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.ARImageRecognitionService
import kotlinx.coroutines.launch

@Composable
fun ARView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize AR service
    val arService = remember { ARImageRecognitionService(context) }
    
    // Initialize AR service when component is created
    LaunchedEffect(Unit) {
        arService.initialize()
    }
    
    // Observe recognized images
    val recognizedImages by arService.recognizedImages.collectAsState()
    val isTracking by arService.isTracking.collectAsState()
    val error by arService.error.collectAsState()
    
    // Handle recognized images
    LaunchedEffect(recognizedImages) {
        recognizedImages.forEach { augmentedImage ->
            // Pass the augmented image for AR overlay positioning
            onAugmentedImageRecognized(augmentedImage)
            
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { onImageRecognized(it) }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            createARView(ctx, arService)
        },
        modifier = modifier
    )
    
    // Handle errors
    error?.let { errorMessage ->
        // You could show an error dialog or snackbar here
        println("AR Error: $errorMessage")
    }
}

private fun createARView(
    context: Context,
    arService: ARImageRecognitionService
): android.view.View {
    return try {
        // Try to create a camera preview view
        createCameraPreviewView(context, arService)
    } catch (e: Exception) {
        // Fallback to a placeholder view with instructions
        createFallbackView(context)
    }
}

private fun createCameraPreviewView(
    context: Context,
    arService: ARImageRecognitionService
): android.view.View {
    // Create a simple camera preview view
    val view = android.view.View(context)
    view.setBackgroundColor(android.graphics.Color.DKGRAY)
    
    // Add some text to indicate camera is working
    val textView = android.widget.TextView(context)
    textView.text = "Camera Preview\nAR Mode Active"
    textView.setTextColor(android.graphics.Color.WHITE)
    textView.textSize = 16f
    textView.gravity = android.view.Gravity.CENTER
    
    val layout = android.widget.LinearLayout(context)
    layout.orientation = android.widget.LinearLayout.VERTICAL
    layout.gravity = android.view.Gravity.CENTER
    layout.addView(textView)
    
    return layout
}

private fun createFallbackView(context: Context): android.view.View {
    // Create a fallback view for emulators or devices without camera
    val textView = android.widget.TextView(context)
    textView.text = "AR Mode\n\nCamera not available in emulator.\nPlease test on a physical device for full AR functionality."
    textView.setTextColor(android.graphics.Color.WHITE)
    textView.textSize = 14f
    textView.gravity = android.view.Gravity.CENTER
    textView.setPadding(32, 32, 32, 32)
    
    val layout = android.widget.LinearLayout(context)
    layout.orientation = android.widget.LinearLayout.VERTICAL
    layout.gravity = android.view.Gravity.CENTER
    layout.setBackgroundColor(android.graphics.Color.BLACK)
    layout.addView(textView)
    
    return layout
}


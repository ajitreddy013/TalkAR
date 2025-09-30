package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ar.core.Frame
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Sceneform
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.ARImageRecognitionService
import kotlinx.coroutines.launch

@Composable
fun ARView(
    onImageRecognized: (ImageRecognition) -> Unit,
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
            val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
            imageRecognition?.let { onImageRecognized(it) }
        }
    }
    
    AndroidView(
        factory = { ctx ->
            createARSceneView(ctx, arService)
        },
        modifier = modifier
    )
    
    // Handle errors
    error?.let { errorMessage ->
        // You could show an error dialog or snackbar here
        println("AR Error: $errorMessage")
    }
}

private fun createARSceneView(
    context: Context,
    arService: ARImageRecognitionService
): ArSceneView {
    val sceneView = ArSceneView(context)
    
    // Initialize Sceneform
    Sceneform.install(context)
    
    // Set up AR session
    sceneView.session = arService.session
    
    // Set up frame processing
    sceneView.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
        // Handle plane taps if needed
    }
    
    // Process frames for image recognition
    sceneView.setOnUpdateListener { frame ->
        frame?.let { arFrame ->
            arService.processFrame(arFrame)
        }
    }
    
    return sceneView
}


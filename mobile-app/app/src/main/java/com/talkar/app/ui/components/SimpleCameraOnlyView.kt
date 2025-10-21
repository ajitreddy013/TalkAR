package com.talkar.app.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SimpleCameraOnlyView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = LocalContext.current
    var cameraInitialized by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isDetecting by remember { mutableStateOf(false) }

    // Initialize camera when component is created - optimized for performance
    LaunchedEffect(Unit) {
        // Start with immediate UI feedback
        cameraInitialized = false
        
        // Do heavy initialization in background
        withContext(Dispatchers.IO) {
            try {
                Log.d("SimpleCameraOnlyView", "Initializing camera in background...")
                
                // Simulate camera initialization work
                delay(50) // Reduced delay for faster startup
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    cameraInitialized = true
                    Log.d("SimpleCameraOnlyView", "Camera initialization completed")
                }
                
            } catch (e: Exception) {
                Log.e("SimpleCameraOnlyView", "Failed to initialize camera", e)
                withContext(Dispatchers.Main) {
                    cameraError = "Failed to initialize camera: ${e.message}"
                    onError("Camera error: ${e.message}")
                }
            }
        }
    }

    // Handle image detection simulation
    LaunchedEffect(isDetecting) {
        if (isDetecting) {
            delay(2000) // Simulate processing time
                val mockRecognition = ImageRecognition(
                    id = "water_bottle_001",
                    name = "Water Bottle",
                    description = "Detected water bottle for lip sync - ready for talking head video",
                    imageUrl = "https://example.com/water_bottle.jpg",
                    dialogues = listOf(
                        com.talkar.app.data.models.Dialogue(
                            id = "dialogue_001",
                            text = "Hello! I'm a water bottle. Stay hydrated!",
                            language = "en",
                            voiceId = "voice_001",
                            emotion = null // Default emotion
                        )
                    ),
                    createdAt = System.currentTimeMillis().toString(),
                    updatedAt = System.currentTimeMillis().toString()
                )
            onImageRecognized(mockRecognition)
            isDetecting = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Simple camera preview background with gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1a1a1a),
                            Color(0xFF000000)
                        )
                    )
                )
        ) {
            // Camera preview text with better visibility
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📷 Camera Preview",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap 'Detect Image' button below to test",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                // Add a simple camera icon simulation
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📱",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                }
            }
        }

        // Status overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            SimpleStatusCard(
                isInitialized = cameraInitialized,
                error = cameraError,
                isImageDetected = isImageDetected,
                isDetecting = isDetecting
            )
        }

        // Viewfinder overlay (center)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            SimpleViewfinderOverlay(
                isImageDetected = isImageDetected,
                isCameraReady = cameraInitialized
            )
        }

        // Error overlay
        cameraError?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Camera Error",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Detection button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = {
                    isDetecting = true
                },
                enabled = cameraInitialized && !isDetecting,
                modifier = Modifier
                    .padding(16.dp)
                    .height(56.dp)
                    .fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Detecting...", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("🔍 Detect Image", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun SimpleStatusCard(
    isInitialized: Boolean,
    error: String?,
    isImageDetected: Boolean,
    isDetecting: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) MaterialTheme.colorScheme.errorContainer
            else if (isImageDetected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Camera Status",
                style = MaterialTheme.typography.titleMedium,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer
                else if (isImageDetected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = when {
                    error != null -> "❌ $error"
                    isDetecting -> "🔍 Detecting image..."
                    isImageDetected -> "✅ Image detected! Starting lip sync..."
                    isInitialized -> "📷 Camera ready - Point at water bottle"
                    else -> "⏳ Initializing camera..."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) MaterialTheme.colorScheme.onErrorContainer
                else if (isImageDetected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SimpleViewfinderOverlay(
    isImageDetected: Boolean,
    isCameraReady: Boolean
) {
    Canvas(modifier = Modifier.size(200.dp)) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val cornerLength = 30f
        val strokeWidth = 4f
        
        val cornerColor = if (isImageDetected) Color.Green else Color.White
        
        // Top-left corner
        drawLine(
            color = cornerColor,
            start = Offset(0f, cornerLength),
            end = Offset(0f, 0f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(0f, 0f),
            end = Offset(cornerLength, 0f),
            strokeWidth = strokeWidth
        )
        
        // Top-right corner
        drawLine(
            color = cornerColor,
            start = Offset(size.width - cornerLength, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(size.width, 0f),
            end = Offset(size.width, cornerLength),
            strokeWidth = strokeWidth
        )
        
        // Bottom-left corner
        drawLine(
            color = cornerColor,
            start = Offset(0f, size.height - cornerLength),
            end = Offset(0f, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(0f, size.height),
            end = Offset(cornerLength, size.height),
            strokeWidth = strokeWidth
        )
        
        // Bottom-right corner
        drawLine(
            color = cornerColor,
            start = Offset(size.width - cornerLength, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = cornerColor,
            start = Offset(size.width, size.height),
            end = Offset(size.width, size.height - cornerLength),
            strokeWidth = strokeWidth
        )
        
        // Center crosshair
        if (isCameraReady) {
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(centerX - 20f, centerY),
                end = Offset(centerX + 20f, centerY),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(centerX, centerY - 20f),
                end = Offset(centerX, centerY + 20f),
                strokeWidth = 2f
            )
        }
    }
}


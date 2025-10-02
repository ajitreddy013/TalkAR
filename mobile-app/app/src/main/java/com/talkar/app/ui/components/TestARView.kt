package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.talkar.app.data.models.ImageRecognition

@Composable
fun TestARView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            createTestARView(ctx, onImageRecognized)
        },
        modifier = modifier
    )
}

private fun createTestARView(
    context: Context,
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
): android.view.View {
    // Create a simple view for testing without ARCore
    val layout = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        val textView = android.widget.TextView(context).apply {
            text = "🎯 TalkAR Test Mode\n\nTap button to simulate\nimage detection"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
        }
        
        addView(textView)
    }
    
    // Add a test button for simulation
    val testButton = android.widget.Button(context).apply {
        text = "🧪 Test Image Recognition"
        setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
        setTextColor(android.graphics.Color.WHITE)
        setOnClickListener {
            android.util.Log.d("TestARView", "Test button clicked - simulating image recognition")
            simulateImageDetection(onImageRecognized)
        }
    }
    
    val buttonParams = android.widget.LinearLayout.LayoutParams(
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
    )
    buttonParams.gravity = android.view.Gravity.CENTER
    buttonParams.topMargin = 20
    layout.addView(testButton, buttonParams)
    
    return layout
}

private fun simulateImageDetection(
    onImageRecognized: (com.talkar.app.data.models.ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("TestARView", "Simulating image detection for testing")
        
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
        
        android.util.Log.d("TestARView", "Simulated image recognition: ${mockImageRecognition.name}")
        onImageRecognized(mockImageRecognition)
        
    } catch (e: Exception) {
        android.util.Log.e("TestARView", "Error simulating image detection", e)
    }
}

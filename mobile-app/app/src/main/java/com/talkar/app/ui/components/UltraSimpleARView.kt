package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.talkar.app.data.models.ImageRecognition

@Composable
fun UltraSimpleARView(
    onImageRecognized: (ImageRecognition) -> Unit,
    onAugmentedImageRecognized: (com.google.ar.core.AugmentedImage) -> Unit = {},
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
    isImageDetected: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Hide the view when image is detected to allow overlays to show
    if (!isImageDetected) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                createSimpleCameraView(ctx, onImageRecognized)
            },
            modifier = modifier
        )
    }
}

private fun createSimpleCameraView(
    context: Context,
    onImageRecognized: (ImageRecognition) -> Unit
): android.view.View {
    // Create a camera preview with actual camera feed
    val layout = android.widget.FrameLayout(context).apply {
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK) // Black background for camera
    }
    
    // Create TextureView for camera preview
    val textureView = android.view.TextureView(context)
    textureView.surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("UltraSimpleARView", "Surface texture available: ${width}x${height}")
            initializeCamera(textureView, onImageRecognized)
        }
        
        override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
            android.util.Log.d("UltraSimpleARView", "Surface texture size changed: ${width}x${height}")
        }
        
        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
            android.util.Log.d("UltraSimpleARView", "Surface texture destroyed")
            return true
        }
        
        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
            // This is called continuously when the camera is working
        }
    }
    
    layout.addView(textureView)
    
    // Add scanning indicator overlay
    val scanningOverlay = android.widget.TextView(context).apply {
        text = "🎯 Point camera at an image to scan"
        textSize = 18f
        setTextColor(android.graphics.Color.WHITE)
        gravity = android.view.Gravity.CENTER
        setPadding(20, 20, 20, 20)
        setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
    }
    
    val overlayParams = android.widget.FrameLayout.LayoutParams(
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
    )
    overlayParams.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER
    overlayParams.topMargin = 100
    layout.addView(scanningOverlay, overlayParams)
    
    return layout
}

private fun initializeCamera(
    textureView: android.view.TextureView, 
    onImageRecognized: (ImageRecognition) -> Unit
) {
    try {
        android.util.Log.d("UltraSimpleARView", "Initializing camera...")
        
        // Create camera manager
        val cameraManager = textureView.context.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val cameraIdList = cameraManager.cameraIdList
        
        if (cameraIdList.isEmpty()) {
            android.util.Log.e("UltraSimpleARView", "No cameras available")
            return
        }
        
        val cameraId = cameraIdList[0] // Use first available camera
        android.util.Log.d("UltraSimpleARView", "Using camera: $cameraId")
        
        // Check camera permissions
        val hasPermission = textureView.context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (!hasPermission) {
            android.util.Log.e("UltraSimpleARView", "Camera permission not granted")
            return
        }
        
        cameraManager.openCamera(cameraId, object : android.hardware.camera2.CameraDevice.StateCallback() {
            override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("UltraSimpleARView", "Camera opened successfully")
                
                try {
                    // Create capture session
                    val surface = android.view.Surface(textureView.surfaceTexture)
                    val captureRequest = camera.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW)
                    captureRequest.addTarget(surface)
                    
                    camera.createCaptureSession(listOf(surface), object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.d("UltraSimpleARView", "Camera session configured")
                            try {
                                session.setRepeatingRequest(captureRequest.build(), null, null)
                                android.util.Log.d("UltraSimpleARView", "Camera preview started")
                            } catch (e: Exception) {
                                android.util.Log.e("UltraSimpleARView", "Failed to start repeating request", e)
                            }
                        }
                        
                        override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {
                            android.util.Log.e("UltraSimpleARView", "Camera session configuration failed")
                        }
                    }, null)
                } catch (e: Exception) {
                    android.util.Log.e("UltraSimpleARView", "Failed to create capture session", e)
                }
            }
            
            override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
                android.util.Log.d("UltraSimpleARView", "Camera disconnected")
            }
            
            override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
                android.util.Log.e("UltraSimpleARView", "Camera error: $error")
            }
        }, null)
        
    } catch (e: Exception) {
        android.util.Log.e("UltraSimpleARView", "Failed to initialize camera", e)
    }
}

// Note: Real image detection will be implemented with ARCore integration
// For now, this component shows the camera preview without simulation


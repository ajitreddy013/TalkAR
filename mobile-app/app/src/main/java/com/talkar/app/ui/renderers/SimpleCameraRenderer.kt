package com.talkar.app.ui.renderers

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.services.SimpleARService
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL renderer that displays camera feed and handles AR tracking for SimpleARService
 */
class SimpleCameraRenderer(
    private val context: Context,
    private val arService: SimpleARService,
    private val onImageRecognized: (ImageRecognition) -> Unit,
    private val onError: (String) -> Unit
) : GLSurfaceView.Renderer {

    private var displayRotationHelper: DisplayRotationHelper? = null
    private var backgroundRenderer: BackgroundRenderer? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("SimpleCameraRenderer", "Surface created")
        
        // Set clear color to black
        android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        try {
            // Initialize background renderer for camera feed
            backgroundRenderer = BackgroundRenderer()
            backgroundRenderer?.createOnGlThread(context)
            
            // Initialize display rotation helper
            displayRotationHelper = DisplayRotationHelper(context)
            
            Log.d("SimpleCameraRenderer", "OpenGL setup complete")
            
        } catch (e: Exception) {
            Log.e("SimpleCameraRenderer", "Failed to initialize OpenGL", e)
            onError("Failed to initialize camera preview: ${e.message}")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("SimpleCameraRenderer", "Surface changed: ${width}x${height}")
        android.opengl.GLES20.glViewport(0, 0, width, height)
        displayRotationHelper?.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)

        try {
            // Check if ARCore is initialized
            if (!arService.isInitialized()) {
                return
            }

            // Get ARCore session from the service instead of creating a new one
            val session = arService.getSession()
            session?.let { session ->
                
                // Update display rotation
                displayRotationHelper?.updateSessionIfNeeded(session)
                
                try {
                    // Update ARCore frame
                    val frame = session.update()
                    val camera = frame.camera
                    
                    // Draw camera background
                    backgroundRenderer?.draw(frame)
                    
                    // Process frame for image recognition using the service
                    arService.processFrame(frame)
                    
                    // Check for recognized images and draw AR content
                    val augmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
                    for (augmentedImage in augmentedImages) {
                        when (augmentedImage.trackingState) {
                            TrackingState.TRACKING -> {
                                Log.d("SimpleCameraRenderer", "Image tracked: ${augmentedImage.name}")
                                // Here you would draw AR content on top of the detected image
                                // For now, we'll just trigger the callback
                                val imageRecognition = arService.getRecognizedImage(augmentedImage.name ?: "")
                                imageRecognition?.let { onImageRecognized(it) }
                            }
                            else -> {
                                // Image not being tracked
                            }
                        }
                    }
                    
                } catch (e: CameraNotAvailableException) {
                    Log.e("SimpleCameraRenderer", "Camera not available", e)
                } catch (e: Exception) {
                    Log.e("SimpleCameraRenderer", "Error in draw frame", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e("SimpleCameraRenderer", "Error in onDrawFrame", e)
        }
    }
}

/**
 * Helper class for display rotation
 */
private class DisplayRotationHelper(private val context: Context) {
    fun onSurfaceChanged(width: Int, height: Int) {
        Log.d("DisplayRotationHelper", "Surface changed: ${width}x${height}")
    }
    
    fun updateSessionIfNeeded(session: Session) {
        // Handle display rotation updates
    }
}

/**
 * Renderer for camera background
 */
private class BackgroundRenderer {
    fun createOnGlThread(context: Context) {
        Log.d("BackgroundRenderer", "Created on GL thread")
        // Initialize shaders and textures for camera background
    }
    
    fun draw(frame: Frame) {
        // Draw camera background texture
        // This would typically involve:
        // 1. Getting camera texture from ARCore frame
        // 2. Drawing it as background using OpenGL shaders
        Log.v("BackgroundRenderer", "Drawing camera background")
    }
}
package com.talkar.app.ar.video.rendering

import android.util.Size
import android.view.Choreographer
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.talkar.app.ar.video.models.FrameTime
import com.talkar.app.ar.video.models.Matrix4
import com.talkar.app.ar.video.models.TransformResult
import com.talkar.app.ar.video.models.Vector2
import kotlin.math.sqrt

/**
 * Implementation of RenderCoordinator using ARCore and Choreographer.
 *
 * Features:
 * - ARCore matrix math for 3D to 2D projection
 * - Frustum culling for off-screen detection
 * - 60fps frame callbacks via Choreographer
 * - Matrix caching for performance
 * - Distance-based scaling
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
class RenderCoordinatorImpl : RenderCoordinator {
    
    companion object {
        private const val TAG = "RenderCoordinator"
        private const val NANOS_PER_SECOND = 1_000_000_000L
    }
    
    // Cached matrices for performance
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)
    
    // Frame callback state
    private var frameCallback: ((FrameTime) -> Unit)? = null
    private var lastFrameTimeNanos: Long = 0
    private val choreographerCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val deltaTimeMs = if (lastFrameTimeNanos == 0L) {
                0f
            } else {
                (frameTimeNanos - lastFrameTimeNanos).toFloat() / 1_000_000f
            }
            
            lastFrameTimeNanos = frameTimeNanos
            
            frameCallback?.invoke(
                FrameTime(
                    timestampNs = frameTimeNanos,
                    deltaTimeMs = deltaTimeMs
                )
            )
            
            // Re-register for next frame
            if (frameCallback != null) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }
    
    override fun calculateTransform(
        anchor: Anchor,
        camera: Camera,
        viewportSize: Size
    ): TransformResult {
        // Get projection and view matrices from ARCore camera
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        camera.getViewMatrix(viewMatrix, 0)
        
        // Get model matrix from anchor pose
        anchor.pose.toMatrix(modelMatrix, 0)
        
        // Calculate model-view matrix
        android.opengl.Matrix.multiplyMM(
            modelViewMatrix, 0,
            viewMatrix, 0,
            modelMatrix, 0
        )
        
        // Calculate model-view-projection matrix
        android.opengl.Matrix.multiplyMM(
            modelViewProjectionMatrix, 0,
            projectionMatrix, 0,
            modelViewMatrix, 0
        )
        
        // Extract anchor position in world space
        val anchorPosition = anchor.pose.translation
        
        // Calculate distance from camera
        val cameraPosition = camera.pose.translation
        val dx = anchorPosition[0] - cameraPosition[0]
        val dy = anchorPosition[1] - cameraPosition[1]
        val dz = anchorPosition[2] - cameraPosition[2]
        val distanceFromCamera = sqrt(dx * dx + dy * dy + dz * dz)
        
        // Project anchor center to screen space
        val anchorCenter = floatArrayOf(0f, 0f, 0f, 1f)
        val screenSpacePosition = FloatArray(4)
        
        android.opengl.Matrix.multiplyMV(
            screenSpacePosition, 0,
            modelViewProjectionMatrix, 0,
            anchorCenter, 0
        )
        
        // Perform perspective divide
        if (screenSpacePosition[3] != 0f) {
            screenSpacePosition[0] /= screenSpacePosition[3]
            screenSpacePosition[1] /= screenSpacePosition[3]
            screenSpacePosition[2] /= screenSpacePosition[3]
        }
        
        // Convert from NDC (-1 to 1) to screen coordinates (0 to width/height)
        val screenX = (screenSpacePosition[0] + 1f) * 0.5f * viewportSize.width
        val screenY = (1f - screenSpacePosition[1]) * 0.5f * viewportSize.height
        
        // Check if anchor is visible (frustum culling)
        val isVisible = screenSpacePosition[0] >= -1f && screenSpacePosition[0] <= 1f &&
                       screenSpacePosition[1] >= -1f && screenSpacePosition[1] <= 1f &&
                       screenSpacePosition[2] >= -1f && screenSpacePosition[2] <= 1f
        
        // Calculate screen size based on distance (simple perspective scaling)
        // Assume a base size at 1 meter distance
        val baseSize = 200f // pixels at 1 meter
        val scale = baseSize / distanceFromCamera
        val screenSize = Vector2(scale, scale)
        
        // Create transformation matrix for rendering
        val transformMatrix = Matrix4(modelViewProjectionMatrix)
        
        return TransformResult(
            matrix = transformMatrix,
            screenPosition = Vector2(screenX, screenY),
            screenSize = screenSize,
            isVisible = isVisible,
            distanceFromCamera = distanceFromCamera
        )
    }
    
    override fun registerFrameCallback(callback: (FrameTime) -> Unit) {
        frameCallback = callback
        lastFrameTimeNanos = 0
        Choreographer.getInstance().postFrameCallback(choreographerCallback)
    }
    
    override fun unregisterFrameCallback() {
        frameCallback = null
        Choreographer.getInstance().removeFrameCallback(choreographerCallback)
    }
    
    override fun release() {
        unregisterFrameCallback()
    }
}

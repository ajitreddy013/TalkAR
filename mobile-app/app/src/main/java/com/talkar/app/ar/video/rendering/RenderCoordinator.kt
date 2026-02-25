package com.talkar.app.ar.video.rendering

import android.util.Size
import com.google.ar.core.Anchor
import com.google.ar.core.Camera
import com.talkar.app.ar.video.models.FrameTime
import com.talkar.app.ar.video.models.TransformResult

/**
 * Synchronizes AR tracking updates with video rendering at 60fps.
 *
 * Responsibilities:
 * - Calculate transformation matrices from AR anchor to screen space
 * - Convert 3D poses to 2D screen coordinates
 * - Implement frustum culling for off-screen overlays
 * - Provide frame callbacks synchronized with AR rendering
 * - Cache matrices for performance
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
interface RenderCoordinator {
    
    /**
     * Calculates transformation matrix from AR anchor to screen space.
     *
     * Uses ARCore's projection and view matrices to convert 3D anchor pose
     * to 2D screen coordinates with appropriate scale based on distance.
     *
     * @param anchor ARCore anchor with 3D position and orientation
     * @param camera AR camera with projection matrix
     * @param viewportSize Screen dimensions in pixels
     * @return TransformResult with matrix, screen position, size, and visibility
     */
    fun calculateTransform(
        anchor: Anchor,
        camera: Camera,
        viewportSize: Size
    ): TransformResult
    
    /**
     * Registers for frame callbacks synchronized with AR rendering.
     *
     * Callbacks are invoked at 60fps using Choreographer.
     *
     * @param callback Called every frame with timing information
     */
    fun registerFrameCallback(callback: (FrameTime) -> Unit)
    
    /**
     * Unregisters frame callback.
     */
    fun unregisterFrameCallback()
    
    /**
     * Releases rendering resources.
     */
    fun release()
}

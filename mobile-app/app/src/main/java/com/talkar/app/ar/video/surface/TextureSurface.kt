package com.talkar.app.ar.video.surface

import android.view.Surface
import com.talkar.app.ar.video.models.Matrix4

/**
 * Interface for a hardware-accelerated rendering surface for video frames.
 */
interface TextureSurface {
    /**
     * Gets the Surface for video rendering.
     * Returns null if surface is not yet available.
     */
    fun getSurface(): Surface?

    /**
     * Applies a transformation matrix for positioning and scaling.
     *
     * @param matrix 4x4 transformation matrix from AR tracking
     */
    fun setTransform(matrix: Matrix4)

    /**
     * Sets the size of the texture surface.
     *
     * @param width Width in pixels
     * @param height Height in pixels
     */
    fun setSize(width: Int, height: Int)

    /**
     * Sets the visibility of the surface.
     *
     * @param visible True to show, false to hide
     */
    fun setVisible(visible: Boolean)

    /**
     * Checks if the surface is currently available.
     */
    fun isAvailable(): Boolean

    /**
     * Releases surface resources.
     */
    fun release()

    /**
     * Registers a listener for surface lifecycle events.
     */
    fun setListener(listener: TextureSurfaceListener)
}

/**
 * Listener for texture surface lifecycle events.
 */
interface TextureSurfaceListener {
    /**
     * Called when the surface becomes available.
     */
    fun onSurfaceAvailable(surface: Surface, width: Int, height: Int)

    /**
     * Called when the surface size changes.
     */
    fun onSurfaceSizeChanged(surface: Surface, width: Int, height: Int)

    /**
     * Called when the surface is destroyed.
     */
    fun onSurfaceDestroyed(surface: Surface)
}

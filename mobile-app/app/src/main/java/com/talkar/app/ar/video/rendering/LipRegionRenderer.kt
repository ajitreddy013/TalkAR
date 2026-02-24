package com.talkar.app.ar.video.rendering

import android.view.Surface
import com.talkar.app.ar.video.models.LipCoordinates
import com.talkar.app.ar.video.models.Matrix4

/**
 * Renders lip video with alpha blending for seamless integration with static poster.
 *
 * Responsibilities:
 * - Render lip region video overlay
 * - Apply alpha blending with Gaussian blur for edge feathering
 * - Convert normalized coordinates to pixel coordinates
 * - Apply AR tracking transformations
 * - Maintain 60fps rendering performance
 *
 * Requirements: 9.1, 9.2, 9.3, 9.4, 10.1, 10.2, 10.4
 */
interface LipRegionRenderer {
    
    /**
     * Sets lip region coordinates from backend.
     *
     * Coordinates are in normalized 0-1 range where (0,0) is top-left.
     *
     * @param coordinates Normalized lip coordinates
     */
    fun setLipCoordinates(coordinates: LipCoordinates)
    
    /**
     * Sets the poster dimensions for coordinate conversion.
     *
     * Used to convert normalized coordinates to pixel coordinates.
     *
     * @param width Poster width in pixels
     * @param height Poster height in pixels
     */
    fun setPosterDimensions(width: Int, height: Int)
    
    /**
     * Applies transformation matrix from AR tracking.
     *
     * @param matrix 4x4 transformation matrix from RenderCoordinator
     */
    fun setTransform(matrix: Matrix4)
    
    /**
     * Configures alpha blending parameters.
     *
     * @param featherRadius Gaussian blur radius in pixels (5-10px recommended)
     */
    fun setBlendingParameters(featherRadius: Float)
    
    /**
     * Creates and returns a Surface for video rendering.
     *
     * This Surface should be passed to VideoDecoder for frame output.
     *
     * @return Surface that can be used by VideoDecoder
     */
    fun getSurface(): Surface?
    
    /**
     * Sets visibility of the lip overlay.
     *
     * @param visible True to show overlay, false to hide
     */
    fun setVisible(visible: Boolean)
    
    /**
     * Releases rendering resources.
     */
    fun release()
}

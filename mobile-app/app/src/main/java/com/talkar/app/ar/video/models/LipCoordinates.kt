package com.talkar.app.ar.video.models

import android.graphics.Rect

/**
 * Represents lip region coordinates in normalized format (0-1 range).
 * 
 * Coordinates are normalized where (0,0) is the top-left corner and (1,1) is the bottom-right
 * corner of the poster. This allows consistent positioning across different poster sizes.
 * 
 * @property lipX Normalized X coordinate of the lip region's top-left corner (0-1 range)
 * @property lipY Normalized Y coordinate of the lip region's top-left corner (0-1 range)
 * @property lipWidth Normalized width of the lip region (0-1 range)
 * @property lipHeight Normalized height of the lip region (0-1 range)
 * 
 * @throws IllegalArgumentException if any coordinate value is outside the 0-1 range
 */
data class LipCoordinates(
    val lipX: Float,
    val lipY: Float,
    val lipWidth: Float,
    val lipHeight: Float
) {
    init {
        require(lipX in 0f..1f) { "lipX must be in range 0-1, got $lipX" }
        require(lipY in 0f..1f) { "lipY must be in range 0-1, got $lipY" }
        require(lipWidth in 0f..1f) { "lipWidth must be in range 0-1, got $lipWidth" }
        require(lipHeight in 0f..1f) { "lipHeight must be in range 0-1, got $lipHeight" }
    }
    
    /**
     * Converts normalized coordinates to pixel coordinates based on poster dimensions.
     * 
     * @param posterWidth Width of the poster in pixels
     * @param posterHeight Height of the poster in pixels
     * @return Rect containing pixel coordinates of the lip region
     */
    fun toPixelCoordinates(posterWidth: Int, posterHeight: Int): Rect {
        val pixelX = (lipX * posterWidth).toInt()
        val pixelY = (lipY * posterHeight).toInt()
        val pixelWidth = (lipWidth * posterWidth).toInt()
        val pixelHeight = (lipHeight * posterHeight).toInt()
        
        return Rect(pixelX, pixelY, pixelX + pixelWidth, pixelY + pixelHeight)
    }
    
    companion object {
        /**
         * Creates LipCoordinates from pixel coordinates.
         * 
         * @param pixelRect Pixel coordinates of the lip region
         * @param posterWidth Width of the poster in pixels
         * @param posterHeight Height of the poster in pixels
         * @return LipCoordinates in normalized format
         */
        fun fromPixelCoordinates(
            pixelRect: Rect,
            posterWidth: Int,
            posterHeight: Int
        ): LipCoordinates {
            return LipCoordinates(
                lipX = pixelRect.left.toFloat() / posterWidth,
                lipY = pixelRect.top.toFloat() / posterHeight,
                lipWidth = pixelRect.width().toFloat() / posterWidth,
                lipHeight = pixelRect.height().toFloat() / posterHeight
            )
        }
    }
}

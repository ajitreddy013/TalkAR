package com.talkar.app.ar.video.models

/**
 * Metadata for a poster used in AR tracking.
 *
 * Contains information about whether the poster has a human face
 * and the normalized lip region coordinates.
 *
 * Requirements: 1.2, 3.3, 4.1
 */
data class PosterMetadata(
    val posterId: String,
    val hasHumanFace: Boolean,
    val lipRegionX: Float,
    val lipRegionY: Float,
    val lipRegionWidth: Float,
    val lipRegionHeight: Float
) {
    init {
        require(lipRegionX in 0f..1f) { "lipRegionX must be in range 0-1" }
        require(lipRegionY in 0f..1f) { "lipRegionY must be in range 0-1" }
        require(lipRegionWidth in 0f..1f) { "lipRegionWidth must be in range 0-1" }
        require(lipRegionHeight in 0f..1f) { "lipRegionHeight must be in range 0-1" }
    }
    
    /**
     * Converts to LipCoordinates for rendering.
     */
    fun toLipCoordinates(): LipCoordinates {
        return LipCoordinates(
            lipX = lipRegionX,
            lipY = lipRegionY,
            lipWidth = lipRegionWidth,
            lipHeight = lipRegionHeight
        )
    }
}

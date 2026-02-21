package com.talkar.app.ar.video.models

import com.google.ar.core.Anchor
import com.google.ar.core.TrackingState

/**
 * Information about a tracked AR image.
 *
 * @property id Unique identifier for the image
 * @property name Name of the reference image
 * @property anchor ARCore anchor for the detected image
 * @property trackingState Current tracking state from ARCore
 * @property extentX Physical width of the image in meters
 * @property extentZ Physical height of the image in meters
 */
data class TrackedImage(
    val id: String,
    val name: String,
    val anchor: Anchor,
    val trackingState: TrackingState,
    val extentX: Float,
    val extentZ: Float
)

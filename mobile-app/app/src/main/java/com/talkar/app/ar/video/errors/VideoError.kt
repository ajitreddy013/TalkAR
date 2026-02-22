package com.talkar.app.ar.video.errors

import android.net.Uri

/**
 * Sealed class hierarchy for video overlay errors.
 */
sealed class VideoError(
    message: String,
    val code: Int
) : Exception(message) {
    /**
     * Decoder initialization failed.
     */
    class DecoderInitializationFailed(message: String) : VideoError(message, ERROR_DECODER_INIT)

    /**
     * Surface creation failed.
     */
    class SurfaceCreationFailed(message: String) : VideoError(message, ERROR_SURFACE_CREATION)

    /**
     * Video file loading failed.
     */
    class VideoLoadFailed(message: String, val uri: Uri) : VideoError(message, ERROR_VIDEO_LOAD)

    /**
     * Unsupported video codec.
     */
    class UnsupportedCodec(val codec: String) : VideoError(
        "Codec not supported: $codec",
        ERROR_UNSUPPORTED_CODEC
    )

    /**
     * Frame rendering failed.
     */
    class RenderingFailed(message: String) : VideoError(message, ERROR_RENDERING)

    /**
     * Tracking was lost for the image anchor.
     */
    class TrackingLost(val imageId: String) : VideoError(
        "Tracking lost for image: $imageId",
        ERROR_TRACKING_LOST
    )

    companion object {
        const val ERROR_DECODER_INIT = 1001
        const val ERROR_SURFACE_CREATION = 1002
        const val ERROR_VIDEO_LOAD = 1003
        const val ERROR_UNSUPPORTED_CODEC = 1004
        const val ERROR_RENDERING = 1005
        const val ERROR_TRACKING_LOST = 1006
    }

    /**
     * Returns a formatted error string for logging.
     */
    fun toLogString(): String {
        return """
            ❌ Video Error
            Code: $code
            Message: $message
            Type: ${this::class.simpleName}
        """.trimIndent()
    }
}

package com.talkar.app.ar.video.models

/**
 * Result of transform calculation from 3D anchor to 2D screen space.
 *
 * @property matrix 4x4 transformation matrix for positioning the video overlay
 * @property screenPosition 2D position on screen (x, y in pixels)
 * @property screenSize 2D size on screen (width, height in pixels)
 * @property isVisible Whether the overlay is visible in the camera frustum
 * @property distanceFromCamera Distance from camera in meters
 */
data class TransformResult(
    val matrix: Matrix4,
    val screenPosition: Vector2,
    val screenSize: Vector2,
    val isVisible: Boolean,
    val distanceFromCamera: Float
)

/**
 * 4x4 transformation matrix.
 */
data class Matrix4(
    val values: FloatArray
) {
    init {
        require(values.size == 16) { "Matrix4 must have exactly 16 values" }
    }

    companion object {
        fun identity(): Matrix4 {
            return Matrix4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                )
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Matrix4
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }
}

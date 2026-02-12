package com.talkar.app.ui.components

import android.graphics.Bitmap

/**
 * Controller to trigger image capture from outside the composable
 */
class CaptureController {
    var captureImage: (( (Bitmap) -> Unit ) -> Unit)? = null

    fun capture(onResult: (Bitmap) -> Unit) {
        captureImage?.invoke(onResult)
    }
}

package com.talkar.app.ar.video.surface

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable wrapper for TextureSurface using AndroidView.
 *
 * @param modifier Modifier for the composable
 * @param onSurfaceAvailable Callback when surface becomes available
 * @param onSurfaceDestroyed Callback when surface is destroyed
 */
@Composable
fun VideoTextureSurface(
    modifier: Modifier = Modifier,
    onSurfaceAvailable: (Surface, Int, Int) -> Unit = { _, _, _ -> },
    onSurfaceDestroyed: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Use remember to prevent recreation on recomposition
    val textureView = remember {
        TextureView(context).apply {
            // Enable hardware acceleration
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            // Optimize for video
            isOpaque = true
        }
    }

    DisposableEffect(textureView) {
        // Setup surface texture listener
        val listener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                val surface = Surface(surfaceTexture)
                onSurfaceAvailable(surface, width, height)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                // Handle size changes if needed
            }

            override fun onSurfaceTextureDestroyed(
                surfaceTexture: SurfaceTexture
            ): Boolean {
                onSurfaceDestroyed()
                return true
            }

            override fun onSurfaceTextureUpdated(
                surfaceTexture: SurfaceTexture
            ) {
                // Frame rendered
            }
        }

        textureView.surfaceTextureListener = listener

        onDispose {
            // Cleanup
            textureView.surfaceTextureListener = null
        }
    }

    AndroidView(
        factory = { textureView },
        modifier = modifier,
        update = { view ->
            // Update view properties if needed
            // Avoid recreating the view here
        }
    )
}

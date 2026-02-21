package com.talkar.app.ar.video.surface

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import com.talkar.app.ar.video.models.Matrix4

/**
 * Implementation of TextureSurface using Android TextureView.
 */
class TextureSurfaceImpl(
    context: Context
) : TextureSurface {
    
    private val textureView: TextureView = TextureView(context)
    private var surface: Surface? = null
    private var listener: TextureSurfaceListener? = null
    private var isReleased = false

    init {
        // Enable hardware acceleration
        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // Optimize for video
        textureView.isOpaque = true
        
        // Set up surface texture listener
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "✅ Surface available: ${width}x${height}")
                surface = Surface(surfaceTexture)
                listener?.onSurfaceAvailable(surface!!, width, height)
            }

            override fun onSurfaceTextureSizeChanged(
                surfaceTexture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Log.d(TAG, "📐 Surface size changed: ${width}x${height}")
                surface?.let { listener?.onSurfaceSizeChanged(it, width, height) }
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                Log.d(TAG, "❌ Surface destroyed")
                surface?.let { listener?.onSurfaceDestroyed(it) }
                surface?.release()
                surface = null
                return true
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                // Frame rendered - can be used for debugging
                Log.v(TAG, "🎬 Frame rendered")
            }
        }
    }

    override fun getSurface(): Surface? = surface

    override fun setTransform(matrix: Matrix4) {
        if (isReleased) {
            Log.w(TAG, "Cannot set transform on released surface")
            return
        }

        // Convert Matrix4 to Android Matrix
        val androidMatrix = Matrix()
        androidMatrix.setValues(
            floatArrayOf(
                matrix.values[0], matrix.values[4], matrix.values[12],
                matrix.values[1], matrix.values[5], matrix.values[13],
                matrix.values[3], matrix.values[7], matrix.values[15]
            )
        )
        
        textureView.setTransform(androidMatrix)
    }

    override fun setSize(width: Int, height: Int) {
        if (isReleased) {
            Log.w(TAG, "Cannot set size on released surface")
            return
        }

        textureView.layoutParams = textureView.layoutParams?.apply {
            this.width = width
            this.height = height
        }
    }

    override fun setVisible(visible: Boolean) {
        if (isReleased) {
            Log.w(TAG, "Cannot set visibility on released surface")
            return
        }

        textureView.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun isAvailable(): Boolean {
        return !isReleased && surface != null && textureView.isAvailable
    }

    override fun release() {
        if (isReleased) return
        
        Log.d(TAG, "Releasing texture surface")
        isReleased = true
        surface?.release()
        surface = null
        listener = null
    }

    override fun setListener(listener: TextureSurfaceListener) {
        this.listener = listener
    }

    /**
     * Gets the underlying TextureView for integration with UI frameworks.
     */
    fun getTextureView(): TextureView = textureView

    companion object {
        private const val TAG = "TextureSurfaceImpl"
    }
}

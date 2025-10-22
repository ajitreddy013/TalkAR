package com.talkar.app.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import com.google.ar.core.AugmentedImage

/**
 * Shadow Plane View for rendering a subtle shadow below avatars
 * to enhance visual grounding and realism
 * Performance optimized with caching and throttling
 */
class ShadowPlane(context: Context) : View(context) {
    
    private val shadowPaint = Paint().apply {
        isAntiAlias = true
    }
    
    private var shadowRadius = 0f
    private var shadowAlpha = 0.3f
    private var shadowColor = 0xFF000000.toInt() // Black
    
    // Performance optimization: Cache gradient and avoid unnecessary redraws
    private var cachedGradient: RadialGradient? = null
    private var cachedWidth = 0
    private var cachedHeight = 0
    private var cachedRadius = 0f
    private var cachedAlpha = 0f
    
    // Performance optimization: Throttle updates
    private var lastUpdate = 0L
    private val UPDATE_INTERVAL = 100L // Update every 100ms
    
    /**
     * Update shadow properties based on image size and lighting conditions
     * Performance optimized with throttling
     */
    fun updateShadowProperties(image: AugmentedImage, lightEstimate: com.google.ar.core.LightEstimate?) {
        // Performance optimization: Throttle updates
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdate < UPDATE_INTERVAL) {
            return
        }
        lastUpdate = currentTime
        
        // Calculate shadow size based on image dimensions
        val imageExtentX = image.extentX
        val imageExtentZ = image.extentZ
        val imageSize = kotlin.math.max(imageExtentX, imageExtentZ)
        
        // Scale shadow radius based on image size
        val newRadius = imageSize * 500f // Convert to pixels and scale
        
        // Adjust shadow alpha based on lighting conditions
        val newAlpha = if (lightEstimate?.state == com.google.ar.core.LightEstimate.State.VALID) {
            val pixelIntensity = lightEstimate.pixelIntensity
            // In brighter conditions, make shadow more visible
            when {
                pixelIntensity > 0.7f -> 0.4f
                pixelIntensity > 0.4f -> 0.3f
                pixelIntensity > 0.2f -> 0.2f
                else -> 0.1f
            }
        } else {
            0.3f
        }
        
        // Only update if values have changed significantly
        if (kotlin.math.abs(newRadius - shadowRadius) > 1f || 
            kotlin.math.abs(newAlpha - shadowAlpha) > 0.01f) {
            shadowRadius = newRadius
            shadowAlpha = newAlpha
            
            // Update the shadow gradient
            updateShadowGradient()
            
            // Trigger redraw
            invalidate()
        }
    }
    
    /**
     * Update the shadow gradient for realistic appearance
     * Performance optimized with caching
     */
    private fun updateShadowGradient() {
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Performance optimization: Check if gradient needs to be recreated
        if (cachedGradient != null && 
            cachedWidth == width && 
            cachedHeight == height && 
            kotlin.math.abs(cachedRadius - shadowRadius) < 1f &&
            kotlin.math.abs(cachedAlpha - shadowAlpha) < 0.01f) {
            // Use cached gradient
            shadowPaint.shader = cachedGradient
            return
        }
        
        // Create radial gradient for soft shadow edges
        val colors = intArrayOf(
            shadowColor and (shadowAlpha * 255).toInt() shl 24,
            shadowColor and (shadowAlpha * 0.5f * 255).toInt() shl 24,
            0x00000000 // Transparent
        )
        
        val stops = floatArrayOf(0.0f, 0.7f, 1.0f)
        
        val gradient = RadialGradient(
            centerX, centerY, shadowRadius,
            colors, stops,
            Shader.TileMode.CLAMP
        )
        
        // Cache the gradient
        cachedGradient = gradient
        cachedWidth = width
        cachedHeight = height
        cachedRadius = shadowRadius
        cachedAlpha = shadowAlpha
        
        shadowPaint.shader = gradient
    }
    
    /**
     * Set shadow alpha directly
     */
    fun setShadowAlpha(alpha: Float) {
        val newAlpha = alpha.coerceIn(0.0f, 1.0f)
        if (kotlin.math.abs(newAlpha - shadowAlpha) > 0.01f) {
            shadowAlpha = newAlpha
            cachedGradient = null // Invalidate cache
            updateShadowGradient()
            invalidate()
        }
    }
    
    /**
     * Set shadow color
     */
    fun setShadowColor(color: Int) {
        if (color != shadowColor) {
            shadowColor = color
            cachedGradient = null // Invalidate cache
            updateShadowGradient()
            invalidate()
        }
    }
    
    /**
     * Set shadow radius
     */
    fun setShadowRadius(radius: Float) {
        if (kotlin.math.abs(radius - shadowRadius) > 1f) {
            shadowRadius = radius
            cachedGradient = null // Invalidate cache
            updateShadowGradient()
            invalidate()
        }
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Invalidate cache when size changes
        cachedGradient = null
        updateShadowGradient()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (shadowRadius > 0) {
            val centerX = width / 2f
            val centerY = height / 2f
            
            // Draw the shadow as a circle with radial gradient
            canvas.drawCircle(centerX, centerY, shadowRadius, shadowPaint)
        }
    }
    
    /**
     * Reset shadow properties
     */
    fun reset() {
        shadowRadius = 0f
        shadowAlpha = 0.3f
        cachedGradient = null
        invalidate()
    }
}
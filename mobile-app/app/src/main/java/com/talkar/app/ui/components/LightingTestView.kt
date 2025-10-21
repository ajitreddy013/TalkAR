package com.talkar.app.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.ar.core.LightEstimate

/**
 * Lighting Test View for visualizing light estimation data
 * This component helps test and debug lighting conditions
 */
class LightingTestView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 40f
    }
    
    private var lightEstimate: LightEstimate? = null
    private var lightingQuality: String = "UNKNOWN"
    
    /**
     * Update the view with new light estimation data
     */
    fun updateLightEstimate(estimate: LightEstimate?, quality: String) {
        lightEstimate = estimate
        lightingQuality = quality
        invalidate() // Trigger redraw
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw background
        canvas.drawColor(getBackgroundColorForQuality())
        
        // Draw lighting information
        drawLightingInfo(canvas)
    }
    
    /**
     * Get background color based on lighting quality
     */
    private fun getBackgroundColorForQuality(): Int {
        return when (lightingQuality) {
            "EXCELLENT" -> Color.parseColor("#E8F5E8") // Light green
            "GOOD" -> Color.parseColor("#FFF3E0") // Light orange
            "FAIR" -> Color.parseColor("#FFF8E1") // Light yellow
            "POOR" -> Color.parseColor("#FFEBEE") // Light red
            else -> Color.WHITE
        }
    }
    
    /**
     * Draw lighting information on canvas
     */
    private fun drawLightingInfo(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Draw lighting quality
        paint.color = Color.BLACK
        paint.textSize = 48f
        val qualityText = "Lighting Quality: $lightingQuality"
        val qualityTextWidth = paint.measureText(qualityText)
        canvas.drawText(qualityText, centerX - qualityTextWidth / 2, centerY - 100, paint)
        
        // Draw light estimate details
        paint.textSize = 36f
        val estimate = lightEstimate
        
        if (estimate != null) {
            // Draw state
            val stateText = "State: ${estimate.state}"
            val stateTextWidth = paint.measureText(stateText)
            canvas.drawText(stateText, centerX - stateTextWidth / 2, centerY - 20, paint)
            
            // Draw pixel intensity
            val intensityText = "Pixel Intensity: %.2f".format(estimate.pixelIntensity)
            val intensityTextWidth = paint.measureText(intensityText)
            canvas.drawText(intensityText, centerX - intensityTextWidth / 2, centerY + 40, paint)
            
            // Draw timestamp
            val timestampText = "Timestamp: ${estimate.timestamp}"
            val timestampTextWidth = paint.measureText(timestampText)
            canvas.drawText(timestampText, centerX - timestampTextWidth / 2, centerY + 100, paint)
        } else {
            val noDataText = "No light estimate data available"
            val noDataTextWidth = paint.measureText(noDataText)
            canvas.drawText(noDataText, centerX - noDataTextWidth / 2, centerY + 40, paint)
        }
    }
    
    /**
     * Reset the view
     */
    fun reset() {
        lightEstimate = null
        lightingQuality = "UNKNOWN"
        invalidate()
    }
}
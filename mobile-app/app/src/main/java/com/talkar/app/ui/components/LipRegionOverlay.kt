package com.talkar.app.ui.components

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Paint
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.talkar.app.ar.video.models.LipCoordinates

/**
 * Overlay for rendering the lip region video.
 * 
 * Renders the lip-sync video on top of the AR camera view
 * with proper positioning based on tracking data.
 * 
 * The actual video rendering is handled by LipRegionRenderer
 * which uses TextureView and OpenGL for alpha blending.
 * This composable provides the positioning layer.
 * 
 * Requirements: 10.1, 10.2, 10.4
 */
@Composable
fun LipRegionOverlay(
    modifier: Modifier = Modifier,
    lipCoordinates: LipCoordinates?,
    transform: Matrix?
) {
    if (lipCoordinates == null || transform == null) {
        return
    }
    
    Canvas(modifier = modifier) {
        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val w = size.width
            val h = size.height

            val left = lipCoordinates.lipX * w
            val top = lipCoordinates.lipY * h
            val right = left + (lipCoordinates.lipWidth * w)
            val bottom = top + (lipCoordinates.lipHeight * h)

            val pts = floatArrayOf(
                left, top,
                right, top,
                right, bottom,
                left, bottom
            )
            transform.mapPoints(pts)

            val minX = minOf(pts[0], pts[2], pts[4], pts[6]).coerceIn(0f, w)
            val minY = minOf(pts[1], pts[3], pts[5], pts[7]).coerceIn(0f, h)
            val maxX = maxOf(pts[0], pts[2], pts[4], pts[6]).coerceIn(0f, w)
            val maxY = maxOf(pts[1], pts[3], pts[5], pts[7]).coerceIn(0f, h)

            val mouthPath = Path().apply {
                moveTo(pts[0], pts[1])
                lineTo(pts[2], pts[3])
                lineTo(pts[4], pts[5])
                lineTo(pts[6], pts[7])
                close()
            }

            val fill = Paint().apply {
                color = android.graphics.Color.argb(85, 255, 80, 80)
                style = Paint.Style.FILL
                isAntiAlias = true
                // Feathered edge to approximate alpha blending softness.
                maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
            }
            val stroke = Paint().apply {
                color = android.graphics.Color.argb(180, 255, 80, 80)
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                isAntiAlias = true
            }

            nativeCanvas.save()
            nativeCanvas.drawPath(mouthPath, fill)
            nativeCanvas.restore()
            nativeCanvas.drawRoundRect(minX, minY, maxX, maxY, 10f, 10f, stroke)
        }
    }
}

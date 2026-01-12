package com.talkar.app.data.services

import android.graphics.PointF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceContour
import kotlinx.coroutines.tasks.await

/**
 * Service for detecting face and lip contours using ML Kit
 */
class FaceLipDetectorService {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.15f)
            .build()
    )

    data class FaceLipResult(
        val hasFace: Boolean,
        val faceBounds: android.graphics.Rect? = null,
        val upperLipContour: List<PointF> = emptyList(),
        val lowerLipContour: List<PointF> = emptyList(),
        val mouthOutline: List<PointF> = emptyList()
    )

    /**
     * Process an InputImage for face and lip detection
     */
    suspend fun detectFaceAndLips(image: InputImage): FaceLipResult {
        return try {
            val faces = detector.process(image).await()
            if (faces.isEmpty()) {
                FaceLipResult(false)
            } else {
                val face = faces[0] // Focus on the primary face
                
                val upperLipTop = face.getContour(FaceContour.UPPER_LIP_TOP)?.points ?: emptyList<android.graphics.PointF>()
                val upperLipBottom = face.getContour(FaceContour.UPPER_LIP_BOTTOM)?.points ?: emptyList<android.graphics.PointF>()
                val lowerLipTop = face.getContour(FaceContour.LOWER_LIP_TOP)?.points ?: emptyList<android.graphics.PointF>()
                val lowerLipBottom = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points ?: emptyList<android.graphics.PointF>()

                FaceLipResult(
                    hasFace = true,
                    faceBounds = face.boundingBox,
                    upperLipContour = upperLipTop.map { p: android.graphics.PointF -> android.graphics.PointF(p.x, p.y) },
                    lowerLipContour = lowerLipBottom.map { p: android.graphics.PointF -> android.graphics.PointF(p.x, p.y) },
                    mouthOutline = (upperLipTop + upperLipBottom + lowerLipTop + lowerLipBottom).map { p: android.graphics.PointF -> android.graphics.PointF(p.x, p.y) }
                )
            }
        } catch (e: Exception) {
            Log.e("FaceLipDetector", "Face detection failed", e)
            FaceLipResult(false)
        }
    }

    fun stop() {
        detector.close()
    }
}

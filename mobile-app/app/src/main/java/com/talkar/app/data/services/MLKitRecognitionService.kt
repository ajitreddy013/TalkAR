package com.talkar.app.data.services

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Recognition Service - Uses Google's ML Kit for real-time image recognition
 * 
 * Features:
 * - Object detection and classification
 * - Real-time processing
 * - High accuracy with Google's pre-trained models
 * - Works offline
 * - Optimized for mobile devices
 */
class MLKitRecognitionService(private val context: Context) {
    
    private val tag = "MLKitRecognitionService"
    
    // ML Kit components
    private val imageLabeler: ImageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.7f) // Only return labels with 70%+ confidence
            .build()
    )
    
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )
    
    // Recognition state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _lastRecognition = MutableStateFlow<RecognitionResult?>(null)
    val lastRecognition: StateFlow<RecognitionResult?> = _lastRecognition.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Recognition statistics
    private var recognitionCount = 0
    private var successfulRecognitions = 0
    
    data class RecognitionResult(
        val labels: List<ImageLabel>,
        val objects: List<DetectedObject>,
        val processingTimeMs: Long,
        val confidence: Float,
        val primaryLabel: String?
    )
    
    data class ImageLabel(
        val text: String,
        val confidence: Float,
        val index: Int
    )
    
    /**
     * Process a bitmap image and return recognition results
     */
    suspend fun recognizeImage(bitmap: Bitmap): RecognitionResult {
        return try {
            _isProcessing.value = true
            _error.value = null
            
            val startTime = System.currentTimeMillis()
            Log.d(tag, "Starting ML Kit recognition for image: ${bitmap.width}x${bitmap.height}")
            
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // Run both object detection and image labeling in parallel
            val labelsResult = recognizeLabels(inputImage)
            val objectsResult = detectObjects(inputImage)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Find the primary label (highest confidence)
            val primaryLabel = labelsResult.maxByOrNull { it.confidence }?.text
            
            val result = RecognitionResult(
                labels = labelsResult,
                objects = objectsResult,
                processingTimeMs = processingTime,
                confidence = labelsResult.maxOfOrNull { it.confidence } ?: 0f,
                primaryLabel = primaryLabel
            )
            
            _lastRecognition.value = result
            recognitionCount++
            successfulRecognitions++
            
            Log.d(tag, "Recognition completed in ${processingTime}ms")
            Log.d(tag, "Primary label: $primaryLabel (confidence: ${result.confidence})")
            Log.d(tag, "Found ${labelsResult.size} labels and ${objectsResult.size} objects")
            
            result
            
        } catch (e: Exception) {
            Log.e(tag, "Error during recognition", e)
            _error.value = "Recognition failed: ${e.message}"
            throw e
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * Recognize labels in the image
     */
    private suspend fun recognizeLabels(inputImage: InputImage): List<ImageLabel> {
        return suspendCancellableCoroutine { continuation ->
            imageLabeler.process(inputImage)
                .addOnSuccessListener { labels ->
                    val imageLabels = labels.mapIndexed { index, label ->
                        ImageLabel(
                            text = label.text,
                            confidence = label.confidence,
                            index = index
                        )
                    }
                    continuation.resume(imageLabels)
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "Image labeling failed", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }
    
    /**
     * Detect objects in the image
     */
    private suspend fun detectObjects(inputImage: InputImage): List<DetectedObject> {
        return suspendCancellableCoroutine { continuation ->
            objectDetector.process(inputImage)
                .addOnSuccessListener { objects ->
                    Log.d(tag, "Detected ${objects.size} objects")
                    continuation.resume(objects)
                }
                .addOnFailureListener { exception ->
                    Log.e(tag, "Object detection failed", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }
    
    /**
     * Check if a specific object is detected (e.g., "bottle", "cup", "book")
     */
    fun isObjectDetected(recognitionResult: RecognitionResult, targetObjects: List<String>): Boolean {
        val detectedLabels = recognitionResult.labels.map { it.text.lowercase() }
        val detectedObjects = recognitionResult.objects.flatMap { obj ->
            obj.labels.map { it.text.lowercase() }
        }
        
        val allDetected = detectedLabels + detectedObjects
        
        return targetObjects.any { target ->
            allDetected.any { detected ->
                detected.contains(target.lowercase()) || target.lowercase().contains(detected)
            }
        }
    }
    
    /**
     * Get recognition statistics
     */
    fun getRecognitionStats(): RecognitionStats {
        return RecognitionStats(
            totalRecognitions = recognitionCount,
            successfulRecognitions = successfulRecognitions,
            successRate = if (recognitionCount > 0) successfulRecognitions.toFloat() / recognitionCount else 0f
        )
    }
    
    /**
     * Convert recognition result to ImageRecognition model
     */
    fun convertToImageRecognition(recognitionResult: RecognitionResult): ImageRecognition {
        val primaryLabel = recognitionResult.primaryLabel ?: "Unknown Object"
        val description = "Detected: ${primaryLabel} (confidence: ${(recognitionResult.confidence * 100).toInt()}%)"
        
        return ImageRecognition(
            id = "mlkit-${System.currentTimeMillis()}",
            name = primaryLabel,
            description = description,
            imageUrl = "", // No URL for real-time detection
            dialogues = listOf(
                com.talkar.app.data.models.Dialogue(
                    id = "dialogue-${System.currentTimeMillis()}",
                    text = "Hello! I'm a $primaryLabel. I was detected using ML Kit!",
                    language = "en",
                    voiceId = "voice_001"
                )
            ),
            createdAt = System.currentTimeMillis().toString(),
            updatedAt = System.currentTimeMillis().toString()
        )
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            imageLabeler.close()
            objectDetector.close()
            Log.d(tag, "ML Kit resources cleaned up")
        } catch (e: Exception) {
            Log.e(tag, "Error cleaning up ML Kit resources", e)
        }
    }
    
    data class RecognitionStats(
        val totalRecognitions: Int,
        val successfulRecognitions: Int,
        val successRate: Float
    )
}

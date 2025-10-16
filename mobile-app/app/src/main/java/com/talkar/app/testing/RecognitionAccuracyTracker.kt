package com.talkar.app.testing

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Recognition Accuracy Tracker
 * Monitors AR image recognition accuracy and reliability
 */
class RecognitionAccuracyTracker {
    
    private val TAG = "RecognitionAccuracy"
    
    // Accuracy metrics
    private val _accuracyPercentage = MutableStateFlow(0f)
    val accuracyPercentage: StateFlow<Float> = _accuracyPercentage.asStateFlow()
    
    private val _totalAttempts = MutableStateFlow(0)
    val totalAttempts: StateFlow<Int> = _totalAttempts.asStateFlow()
    
    private val _successfulRecognitions = MutableStateFlow(0)
    val successfulRecognitions: StateFlow<Int> = _successfulRecognitions.asStateFlow()
    
    private val _falsePositives = MutableStateFlow(0)
    val falsePositives: StateFlow<Int> = _falsePositives.asStateFlow()
    
    private val _averageRecognitionTimeMs = MutableStateFlow(0L)
    val averageRecognitionTimeMs: StateFlow<Long> = _averageRecognitionTimeMs.asStateFlow()
    
    // Recognition history
    private val recognitionTimes = mutableListOf<Long>()
    private val recognitionResults = mutableListOf<RecognitionResult>()
    
    data class RecognitionResult(
        val imageName: String,
        val success: Boolean,
        val confidence: Float,
        val timeMs: Long,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    data class AccuracyReport(
        val totalAttempts: Int,
        val successfulRecognitions: Int,
        val falsePositives: Int,
        val accuracyPercentage: Float,
        val averageTimeMs: Long,
        val averageConfidence: Float,
        val lastResults: List<RecognitionResult>
    )
    
    /**
     * Record a recognition attempt
     */
    fun recordRecognition(
        imageName: String,
        success: Boolean,
        confidence: Float = 0f,
        recognitionTimeMs: Long = 0L
    ) {
        // Increment attempts
        _totalAttempts.value++
        
        // Record result
        val result = RecognitionResult(
            imageName = imageName,
            success = success,
            confidence = confidence,
            timeMs = recognitionTimeMs
        )
        recognitionResults.add(result)
        
        // Update success count
        if (success) {
            _successfulRecognitions.value++
            recognitionTimes.add(recognitionTimeMs)
        }
        
        // Calculate accuracy
        updateAccuracy()
        
        // Calculate average recognition time
        updateAverageTime()
        
        // Log result
        if (success) {
            Log.d(TAG, "✅ Recognition success: $imageName (${confidence * 100}% confidence, ${recognitionTimeMs}ms)")
        } else {
            Log.w(TAG, "❌ Recognition failed: $imageName")
        }
        
        // Keep only recent results (last 100)
        if (recognitionResults.size > 100) {
            recognitionResults.removeAt(0)
        }
    }
    
    /**
     * Record a false positive
     */
    fun recordFalsePositive(detectedName: String) {
        _falsePositives.value++
        Log.w(TAG, "⚠️ False positive detected: $detectedName")
    }
    
    /**
     * Update accuracy percentage
     */
    private fun updateAccuracy() {
        val total = _totalAttempts.value
        val successful = _successfulRecognitions.value
        
        val accuracy = if (total > 0) {
            (successful.toFloat() / total.toFloat()) * 100f
        } else {
            0f
        }
        
        _accuracyPercentage.value = accuracy
    }
    
    /**
     * Update average recognition time
     */
    private fun updateAverageTime() {
        val avgTime = if (recognitionTimes.isNotEmpty()) {
            recognitionTimes.average().toLong()
        } else {
            0L
        }
        
        _averageRecognitionTimeMs.value = avgTime
    }
    
    /**
     * Get accuracy report
     */
    fun getReport(): AccuracyReport {
        val avgConfidence = if (recognitionResults.isNotEmpty()) {
            recognitionResults.filter { it.success }.map { it.confidence }.average().toFloat()
        } else {
            0f
        }
        
        return AccuracyReport(
            totalAttempts = _totalAttempts.value,
            successfulRecognitions = _successfulRecognitions.value,
            falsePositives = _falsePositives.value,
            accuracyPercentage = _accuracyPercentage.value,
            averageTimeMs = _averageRecognitionTimeMs.value,
            averageConfidence = avgConfidence,
            lastResults = recognitionResults.takeLast(10)
        )
    }
    
    /**
     * Log accuracy report
     */
    fun logReport() {
        val report = getReport()
        
        Log.i(TAG, "📊 === Recognition Accuracy Report ===")
        Log.i(TAG, "Total Attempts: ${report.totalAttempts}")
        Log.i(TAG, "Successful: ${report.successfulRecognitions}")
        Log.i(TAG, "False Positives: ${report.falsePositives}")
        Log.i(TAG, "Accuracy: ${report.accuracyPercentage.toInt()}%")
        Log.i(TAG, "Avg Time: ${report.averageTimeMs}ms")
        Log.i(TAG, "Avg Confidence: ${(report.averageConfidence * 100).toInt()}%")
        Log.i(TAG, "======================================")
    }
    
    /**
     * Reset all metrics
     */
    fun reset() {
        _totalAttempts.value = 0
        _successfulRecognitions.value = 0
        _falsePositives.value = 0
        _accuracyPercentage.value = 0f
        _averageRecognitionTimeMs.value = 0L
        recognitionTimes.clear()
        recognitionResults.clear()
        Log.d(TAG, "Metrics reset")
    }
    
    /**
     * Check if accuracy meets requirements (>80%)
     */
    fun meetsAccuracyRequirement(): Boolean {
        return _accuracyPercentage.value >= 80f && _totalAttempts.value >= 10
    }
}

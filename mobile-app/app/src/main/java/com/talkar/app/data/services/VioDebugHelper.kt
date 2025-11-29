package com.talkar.app.data.services

import android.util.Log

/**
 * Helper class for VIO (Visual-Inertial Odometry) debugging and monitoring
 */
class VioDebugHelper {
    private val tag = "VioDebugHelper"
    
    /**
     * Log VIO initialization failure with detailed information
     */
    fun logVioInitializationFailure(error: String, requiredLandmarks: Int, actualLandmarks: Int) {
        Log.e(tag, "VIO Initialization Failed: $error")
        Log.e(tag, "Landmark Requirement: Required=$requiredLandmarks, Actual=$actualLandmarks")
        Log.e(tag, "Initialization Failure Analysis:")
        Log.e(tag, "  - Insufficient visual features in camera view")
        Log.e(tag, "  - Poor lighting conditions")
        Log.e(tag, "  - Camera not properly initialized")
        Log.e(tag, "  - Device movement during initialization")
        
        when {
            actualLandmarks == 0 -> {
                Log.e(tag, "Critical Issue: No landmarks detected. Camera view may be uniform or too dark.")
            }
            actualLandmarks < requiredLandmarks / 2 -> {
                Log.e(tag, "Severe Issue: Very few landmarks detected. Need significantly more visual features.")
            }
            actualLandmarks < requiredLandmarks -> {
                Log.w(tag, "Warning: Insufficient landmarks. Try moving camera to more textured areas.")
            }
        }
    }
    
    /**
     * Log VIO motion tracking issues
     */
    fun logVioMotionIssues(speed: Float, duration: String) {
        Log.w(tag, "VIO Motion Tracking Issue Detected:")
        Log.w(tag, "  - Device moving too fast: ${String.format("%.2f", speed)} m/s")
        Log.w(tag, "  - Duration of issue: $duration")
        Log.w(tag, "  - RANSAC failed to provide valid frame-to-frame translation")
        Log.w(tag, "Recommendations:")
        Log.w(tag, "  1. Hold device more steadily during tracking")
        Log.w(tag, "  2. Move camera more slowly")
        Log.w(tag, "  3. Reduce sudden movements")
        Log.w(tag, "  4. Ensure good lighting for better feature tracking")
    }
    
    /**
     * Log VIO fault detector warnings
     */
    fun logVioFaultWarnings(warning: String) {
        Log.w(tag, "VIO Fault Detector Warning: $warning")
    }
    
    /**
     * Log network connectivity issues affecting VIO
     */
    fun logNetworkIssues(exception: Exception) {
        Log.e(tag, "Network Connectivity Issue Affecting VIO:")
        Log.e(tag, "  - Failed to connect to backend server")
        Log.e(tag, "  - Error: ${exception.message}")
        Log.e(tag, "Impact on VIO:")
        Log.e(tag, "  - Unable to download reference images for tracking")
        Log.e(tag, "  - Falling back to local image database")
        Log.e(tag, "Recommendations:")
        Log.e(tag, "  1. Check network connectivity")
        Log.e(tag, "  2. Verify backend server is running")
        Log.e(tag, "  3. Check firewall settings")
        Log.e(tag, "  4. Use offline fallback mode")
    }
    
    /**
     * Provide actionable recommendations based on landmark count
     */
    fun getLandmarkRecommendations(actualLandmarks: Int, requiredLandmarks: Int): String {
        return when {
            actualLandmarks == 0 -> {
                """
                No landmarks detected. Recommendations:
                1. Point camera at textured surfaces (avoid plain walls)
                2. Improve lighting conditions
                3. Check camera permissions
                4. Clean camera lens
                5. Restart the application
                """.trimIndent()
            }
            actualLandmarks < requiredLandmarks / 3 -> {
                """
                Very few landmarks detected. Recommendations:
                1. Move to areas with more visual texture
                2. Avoid uniform surfaces (whiteboards, plain walls)
                3. Ensure adequate lighting
                4. Hold device steady during initialization
                """.trimIndent()
            }
            actualLandmarks < requiredLandmarks -> {
                """
                Insufficient landmarks for initialization. Recommendations:
                1. Scan environment for more visual features
                2. Slowly move camera to capture more landmarks
                3. Ensure good contrast in scene
                """.trimIndent()
            }
            else -> {
                "Sufficient landmarks detected. Tracking should initialize soon."
            }
        }
    }
    
    /**
     * Log VIO state transitions for debugging
     */
    fun logVioStateTransition(fromState: String, toState: String) {
        Log.d(tag, "VIO State Transition: $fromState -> $toState")
    }
    
    /**
     * Log VIO performance metrics
     */
    fun logVioPerformanceMetrics(fps: Float, latency: Long, landmarks: Int) {
        Log.d(tag, "VIO Performance Metrics - FPS: $fps, Latency: ${latency}ms, Landmarks: $landmarks")
    }
    
    /**
     * Get comprehensive VIO troubleshooting guide
     */
    fun getVioTroubleshootingGuide(): String {
        return """
            VIO Troubleshooting Guide:
            
            Network Issues:
            - Check internet connectivity
            - Verify backend server is accessible
            - Use offline fallback mode when network is unavailable
            
            Motion Tracking Issues:
            - Hold device steadily
            - Move camera slowly and smoothly
            - Avoid sudden jerky movements
            - Ensure adequate lighting
            
            Landmark Detection Issues:
            - Point camera at textured surfaces
            - Avoid plain walls or uniform patterns
            - Ensure good contrast in scene
            - Clean camera lens
            
            Initialization Issues:
            - Allow extra time for initialization
            - Restart app if problems persist
            - Check device compatibility
        """.trimIndent()
    }
}
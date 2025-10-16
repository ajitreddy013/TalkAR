package com.talkar.app.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.system.measureTimeMillis

/**
 * Performance Monitor for TalkAR
 * Tracks FPS, memory usage, load times, and performance metrics
 */
class PerformanceMonitor(private val context: Context) {
    
    private val TAG = "PerformanceMonitor"
    
    // Performance metrics
    private val _fps = MutableStateFlow(0f)
    val fps: StateFlow<Float> = _fps.asStateFlow()
    
    private val _memoryUsageMB = MutableStateFlow(0f)
    val memoryUsageMB: StateFlow<Float> = _memoryUsageMB.asStateFlow()
    
    private val _videoLoadTimeMs = MutableStateFlow(0L)
    val videoLoadTimeMs: StateFlow<Long> = _videoLoadTimeMs.asStateFlow()
    
    private val _performanceStatus = MutableStateFlow(PerformanceStatus.GOOD)
    val performanceStatus: StateFlow<PerformanceStatus> = _performanceStatus.asStateFlow()
    
    // FPS tracking
    private var frameCount = 0
    private var lastFpsUpdateTime = System.currentTimeMillis()
    private val fpsUpdateInterval = 1000L // Update every second
    
    // Frame time tracking
    private val frameTimesMs = mutableListOf<Long>()
    private val maxFrameHistory = 30 // Track last 30 frames
    
    // Memory tracking
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    enum class PerformanceStatus {
        EXCELLENT,  // >50 FPS, <300MB
        GOOD,       // >30 FPS, <400MB
        FAIR,       // >20 FPS, <500MB
        POOR        // <20 FPS or >500MB
    }
    
    data class PerformanceMetrics(
        val fps: Float,
        val memoryMB: Float,
        val averageFrameTimeMs: Float,
        val videoLoadTimeMs: Long,
        val status: PerformanceStatus,
        val deviceInfo: DeviceInfo
    )
    
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val totalMemoryMB: Int,
        val availableMemoryMB: Int
    )
    
    /**
     * Record a frame for FPS calculation
     */
    fun recordFrame() {
        frameCount++
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastUpdate = currentTime - lastFpsUpdateTime
        
        if (timeSinceLastUpdate >= fpsUpdateInterval) {
            // Calculate FPS
            val calculatedFps = (frameCount * 1000f) / timeSinceLastUpdate
            _fps.value = calculatedFps
            
            // Reset counters
            frameCount = 0
            lastFpsUpdateTime = currentTime
            
            // Update performance status
            updatePerformanceStatus()
            
            // Log if performance is degraded
            if (calculatedFps < 30f) {
                Log.w(TAG, "⚠️ Low FPS detected: ${calculatedFps.toInt()} FPS")
            }
        }
    }
    
    /**
     * Record frame time for average calculation
     */
    fun recordFrameTime(timeMs: Long) {
        frameTimesMs.add(timeMs)
        
        // Keep only recent frames
        if (frameTimesMs.size > maxFrameHistory) {
            frameTimesMs.removeAt(0)
        }
    }
    
    /**
     * Measure video load time
     */
    suspend fun measureVideoLoadTime(loadOperation: suspend () -> Unit): Long {
        val loadTime = measureTimeMillis {
            loadOperation()
        }
        
        _videoLoadTimeMs.value = loadTime
        
        if (loadTime > 2000L) {
            Log.w(TAG, "⚠️ Slow video load: ${loadTime}ms (target: <2000ms)")
        } else {
            Log.d(TAG, "✅ Video loaded in ${loadTime}ms")
        }
        
        return loadTime
    }
    
    /**
     * Update memory usage
     */
    fun updateMemoryUsage() {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Get app memory usage
        val runtime = Runtime.getRuntime()
        val usedMemoryBytes = runtime.totalMemory() - runtime.freeMemory()
        val usedMemoryMB = usedMemoryBytes / (1024f * 1024f)
        
        _memoryUsageMB.value = usedMemoryMB
        
        // Log if memory usage is high
        if (usedMemoryMB > 500f) {
            Log.w(TAG, "⚠️ High memory usage: ${usedMemoryMB.toInt()}MB (target: <500MB)")
        }
        
        // Update performance status
        updatePerformanceStatus()
    }
    
    /**
     * Update overall performance status
     */
    private fun updatePerformanceStatus() {
        val currentFps = _fps.value
        val currentMemoryMB = _memoryUsageMB.value
        
        val status = when {
            currentFps > 50f && currentMemoryMB < 300f -> PerformanceStatus.EXCELLENT
            currentFps > 30f && currentMemoryMB < 400f -> PerformanceStatus.GOOD
            currentFps > 20f && currentMemoryMB < 500f -> PerformanceStatus.FAIR
            else -> PerformanceStatus.POOR
        }
        
        _performanceStatus.value = status
        
        if (status == PerformanceStatus.POOR) {
            Log.e(TAG, "❌ Poor performance: FPS=${currentFps.toInt()}, Memory=${currentMemoryMB.toInt()}MB")
        }
    }
    
    /**
     * Get average frame time
     */
    fun getAverageFrameTimeMs(): Float {
        return if (frameTimesMs.isNotEmpty()) {
            frameTimesMs.average().toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Get complete performance metrics
     */
    fun getMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            fps = _fps.value,
            memoryMB = _memoryUsageMB.value,
            averageFrameTimeMs = getAverageFrameTimeMs(),
            videoLoadTimeMs = _videoLoadTimeMs.value,
            status = _performanceStatus.value,
            deviceInfo = getDeviceInfo()
        )
    }
    
    /**
     * Get device information
     */
    private fun getDeviceInfo(): DeviceInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemoryMB = (memoryInfo.totalMem / (1024 * 1024)).toInt()
        val availableMemoryMB = (memoryInfo.availMem / (1024 * 1024)).toInt()
        
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            totalMemoryMB = totalMemoryMB,
            availableMemoryMB = availableMemoryMB
        )
    }
    
    /**
     * Log performance report
     */
    fun logPerformanceReport() {
        val metrics = getMetrics()
        
        Log.i(TAG, "📊 === Performance Report ===")
        Log.i(TAG, "Device: ${metrics.deviceInfo.manufacturer} ${metrics.deviceInfo.model}")
        Log.i(TAG, "Android: ${metrics.deviceInfo.androidVersion}")
        Log.i(TAG, "FPS: ${metrics.fps.toInt()} (target: ≥30)")
        Log.i(TAG, "Memory: ${metrics.memoryMB.toInt()}MB / ${metrics.deviceInfo.totalMemoryMB}MB (target: <500MB)")
        Log.i(TAG, "Avg Frame Time: ${metrics.averageFrameTimeMs.toInt()}ms")
        Log.i(TAG, "Video Load Time: ${metrics.videoLoadTimeMs}ms (target: <2000ms)")
        Log.i(TAG, "Status: ${metrics.status}")
        Log.i(TAG, "===========================")
    }
    
    /**
     * Check if performance meets requirements
     */
    fun meetsRequirements(): Boolean {
        val metrics = getMetrics()
        
        val fpsOk = metrics.fps >= 30f
        val memoryOk = metrics.memoryMB < 500f
        val loadTimeOk = metrics.videoLoadTimeMs < 2000L
        
        return fpsOk && memoryOk && loadTimeOk
    }
    
    /**
     * Get performance warnings
     */
    fun getWarnings(): List<String> {
        val warnings = mutableListOf<String>()
        val metrics = getMetrics()
        
        if (metrics.fps < 30f) {
            warnings.add("⚠️ FPS below target: ${metrics.fps.toInt()} FPS (target: ≥30)")
        }
        
        if (metrics.memoryMB > 500f) {
            warnings.add("⚠️ Memory usage above target: ${metrics.memoryMB.toInt()}MB (target: <500MB)")
        }
        
        if (metrics.videoLoadTimeMs > 2000L) {
            warnings.add("⚠️ Video load time slow: ${metrics.videoLoadTimeMs}ms (target: <2000ms)")
        }
        
        return warnings
    }
}

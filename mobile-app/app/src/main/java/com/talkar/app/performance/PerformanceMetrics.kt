package com.talkar.app.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance Metrics Tracker for TalkAR
 * Monitors latency, FPS, memory usage, and other performance indicators
 */
object PerformanceMetrics {
    
    private const val TAG = "PerformanceMetrics"
    
    // Latency metrics
    private val imageDetectionLatencies = mutableListOf<Long>()
    private val videoLoadLatencies = mutableListOf<Long>()
    private val apiCallLatencies = mutableListOf<Long>()
    
    // FPS tracking
    private val _currentFps = MutableStateFlow(0f)
    val currentFps: StateFlow<Float> = _currentFps.asStateFlow()
    
    private var frameCount = 0L
    private var lastFpsTime = System.currentTimeMillis()
    
    // Memory tracking
    private val _memoryUsageMB = MutableStateFlow(0.0)
    val memoryUsageMB: StateFlow<Double> = _memoryUsageMB.asStateFlow()
    
    // Performance events
    private val events = mutableListOf<PerformanceEvent>()
    private val maxEvents = 100 // Keep last 100 events
    
    data class PerformanceEvent(
        val type: EventType,
        val timestamp: Long,
        val durationMs: Long? = null,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    enum class EventType {
        IMAGE_DETECTION,
        VIDEO_LOAD,
        API_CALL,
        FRAME_RENDER,
        CACHE_HIT,
        CACHE_MISS
    }
    
    data class PerformanceStats(
        val avgImageDetectionLatencyMs: Double,
        val avgVideoLoadLatencyMs: Double,
        val avgApiCallLatencyMs: Double,
        val currentFps: Float,
        val memoryUsageMB: Double,
        val totalEvents: Int,
        val cacheHitRate: Double
    )
    
    /**
     * Start timing an operation
     */
    fun startTiming(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * End timing and record latency for image detection
     */
    fun recordImageDetectionLatency(startTime: Long) {
        val latency = System.currentTimeMillis() - startTime
        imageDetectionLatencies.add(latency)
        
        // Keep only last 50 measurements
        if (imageDetectionLatencies.size > 50) {
            imageDetectionLatencies.removeAt(0)
        }
        
        logEvent(PerformanceEvent(
            type = EventType.IMAGE_DETECTION,
            timestamp = System.currentTimeMillis(),
            durationMs = latency
        ))
        
        Log.d(TAG, "Image detection latency: ${latency}ms")
    }
    
    /**
     * Record video load latency
     */
    fun recordVideoLoadLatency(startTime: Long, videoUrl: String) {
        val latency = System.currentTimeMillis() - startTime
        videoLoadLatencies.add(latency)
        
        if (videoLoadLatencies.size > 50) {
            videoLoadLatencies.removeAt(0)
        }
        
        logEvent(PerformanceEvent(
            type = EventType.VIDEO_LOAD,
            timestamp = System.currentTimeMillis(),
            durationMs = latency,
            metadata = mapOf("videoUrl" to videoUrl)
        ))
        
        Log.d(TAG, "Video load latency: ${latency}ms")
        
        // Check if video load is within 3-second target
        if (latency > 3000) {
            Log.w(TAG, "⚠️ Video load exceeded 3s target: ${latency}ms")
        } else {
            Log.d(TAG, "✅ Video load within 3s target: ${latency}ms")
        }
    }
    
    /**
     * Record API call latency
     */
    fun recordApiCallLatency(startTime: Long, endpoint: String) {
        val latency = System.currentTimeMillis() - startTime
        apiCallLatencies.add(latency)
        
        if (apiCallLatencies.size > 50) {
            apiCallLatencies.removeAt(0)
        }
        
        logEvent(PerformanceEvent(
            type = EventType.API_CALL,
            timestamp = System.currentTimeMillis(),
            durationMs = latency,
            metadata = mapOf("endpoint" to endpoint)
        ))
        
        Log.d(TAG, "API call latency ($endpoint): ${latency}ms")
    }
    
    /**
     * Update FPS (call this every frame)
     */
    fun recordFrame() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime
        
        // Update FPS every second
        if (elapsed >= 1000) {
            val fps = (frameCount.toFloat() / elapsed) * 1000f
            _currentFps.value = fps
            
            logEvent(PerformanceEvent(
                type = EventType.FRAME_RENDER,
                timestamp = currentTime,
                metadata = mapOf("fps" to fps)
            ))
            
            if (fps < 30) {
                Log.w(TAG, "⚠️ FPS below 30: $fps")
            }
            
            frameCount = 0
            lastFpsTime = currentTime
        }
    }
    
    /**
     * Update memory usage
     */
    fun updateMemoryUsage(context: Context) {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024.0)
        _memoryUsageMB.value = usedMemory
        
        // Check if memory usage exceeds 500MB threshold
        if (usedMemory > 500) {
            Log.w(TAG, "⚠️ Memory usage exceeds 500MB: ${String.format("%.2f", usedMemory)}MB")
        }
    }
    
    /**
     * Record cache hit
     */
    fun recordCacheHit(resourceType: String) {
        logEvent(PerformanceEvent(
            type = EventType.CACHE_HIT,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf("resourceType" to resourceType)
        ))
        Log.d(TAG, "Cache HIT: $resourceType")
    }
    
    /**
     * Record cache miss
     */
    fun recordCacheMiss(resourceType: String) {
        logEvent(PerformanceEvent(
            type = EventType.CACHE_MISS,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf("resourceType" to resourceType)
        ))
        Log.d(TAG, "Cache MISS: $resourceType")
    }
    
    /**
     * Get current performance statistics
     */
    fun getStats(): PerformanceStats {
        val cacheHits = events.count { it.type == EventType.CACHE_HIT }
        val cacheMisses = events.count { it.type == EventType.CACHE_MISS }
        val cacheHitRate = if (cacheHits + cacheMisses > 0) {
            cacheHits.toDouble() / (cacheHits + cacheMisses)
        } else {
            0.0
        }
        
        return PerformanceStats(
            avgImageDetectionLatencyMs = imageDetectionLatencies.average().takeIf { !it.isNaN() } ?: 0.0,
            avgVideoLoadLatencyMs = videoLoadLatencies.average().takeIf { !it.isNaN() } ?: 0.0,
            avgApiCallLatencyMs = apiCallLatencies.average().takeIf { !it.isNaN() } ?: 0.0,
            currentFps = _currentFps.value,
            memoryUsageMB = _memoryUsageMB.value,
            totalEvents = events.size,
            cacheHitRate = cacheHitRate
        )
    }
    
    /**
     * Log performance event
     */
    private fun logEvent(event: PerformanceEvent) {
        events.add(event)
        if (events.size > maxEvents) {
            events.removeAt(0)
        }
    }
    
    /**
     * Clear all metrics
     */
    fun reset() {
        imageDetectionLatencies.clear()
        videoLoadLatencies.clear()
        apiCallLatencies.clear()
        events.clear()
        frameCount = 0
        lastFpsTime = System.currentTimeMillis()
        Log.d(TAG, "Performance metrics reset")
    }
    
    /**
     * Print performance summary
     */
    fun printSummary() {
        val stats = getStats()
        Log.d(TAG, "=== Performance Summary ===")
        Log.d(TAG, "Avg Image Detection: ${String.format("%.2f", stats.avgImageDetectionLatencyMs)}ms")
        Log.d(TAG, "Avg Video Load: ${String.format("%.2f", stats.avgVideoLoadLatencyMs)}ms (Target: <3000ms)")
        Log.d(TAG, "Avg API Call: ${String.format("%.2f", stats.avgApiCallLatencyMs)}ms")
        Log.d(TAG, "Current FPS: ${String.format("%.1f", stats.currentFps)} (Target: ≥30)")
        Log.d(TAG, "Memory Usage: ${String.format("%.2f", stats.memoryUsageMB)}MB (Target: <500MB)")
        Log.d(TAG, "Cache Hit Rate: ${String.format("%.1f", stats.cacheHitRate * 100)}%")
        Log.d(TAG, "Total Events: ${stats.totalEvents}")
        Log.d(TAG, "==========================")
    }
}

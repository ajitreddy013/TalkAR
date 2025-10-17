package com.talkar.app.data.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

/**
 * Video Cache Manager for TalkAR
 * Caches last 2-3 lip-sync videos locally for offline/fast replay
 */
class VideoCacheManager(private val context: Context) {
    
    private val TAG = "VideoCacheManager"
    private val cacheDir: File
    private val maxCachedVideos = 3
    private val maxCacheSize = 50 * 1024 * 1024L // 50MB total cache limit
    
    // In-memory index of cached videos
    private val cacheIndex = mutableMapOf<String, CachedVideo>()
    
    data class CachedVideo(
        val videoUrl: String,
        val localPath: String,
        val fileSize: Long,
        val timestamp: Long,
        val imageId: String
    )
    
    init {
        // Create cache directory
        cacheDir = File(context.cacheDir, "talkar_videos")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            Log.d(TAG, "Created video cache directory: ${cacheDir.absolutePath}")
        }
        
        // Load existing cache index
        loadCacheIndex()
    }
    
    /**
     * Get cached video file path if available
     */
    fun getCachedVideoPath(videoUrl: String): String? {
        val cached = cacheIndex[videoUrl]
        if (cached != null) {
            val file = File(cached.localPath)
            if (file.exists()) {
                Log.d(TAG, "Cache HIT for: $videoUrl")
                // Update access time
                cacheIndex[videoUrl] = cached.copy(timestamp = System.currentTimeMillis())
                return cached.localPath
            } else {
                Log.w(TAG, "Cached file missing, removing from index: ${cached.localPath}")
                cacheIndex.remove(videoUrl)
            }
        }
        Log.d(TAG, "Cache MISS for: $videoUrl")
        return null
    }
    
    /**
     * Cache a video from URL
     */
    suspend fun cacheVideo(videoUrl: String, imageId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Caching video: $videoUrl")
            
            // Check if already cached
            getCachedVideoPath(videoUrl)?.let {
                Log.d(TAG, "Video already cached: $it")
                return@withContext it
            }
            
            // Generate unique filename from URL hash
            val fileName = generateFileName(videoUrl)
            val outputFile = File(cacheDir, fileName)
            
            // Download video
            Log.d(TAG, "Downloading video to: ${outputFile.absolutePath}")
            val url = URL(videoUrl)
            url.openStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            val fileSize = outputFile.length()
            Log.d(TAG, "Video cached successfully. Size: ${fileSize / 1024}KB")
            
            // Add to cache index
            val cached = CachedVideo(
                videoUrl = videoUrl,
                localPath = outputFile.absolutePath,
                fileSize = fileSize,
                timestamp = System.currentTimeMillis(),
                imageId = imageId
            )
            cacheIndex[videoUrl] = cached
            
            // Enforce cache limits
            enforceCache Limits()
            
            return@withContext outputFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache video: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Preload video in background
     */
    suspend fun preloadVideo(videoUrl: String, imageId: String) {
        if (getCachedVideoPath(videoUrl) == null) {
            Log.d(TAG, "Preloading video in background: $videoUrl")
            cacheVideo(videoUrl, imageId)
        } else {
            Log.d(TAG, "Video already cached, skip preload: $videoUrl")
        }
    }
    
    /**
     * Clear all cached videos
     */
    fun clearCache() {
        Log.d(TAG, "Clearing all cached videos")
        cacheDir.listFiles()?.forEach { it.delete() }
        cacheIndex.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val totalSize = cacheIndex.values.sumOf { it.fileSize }
        val count = cacheIndex.size
        return CacheStats(
            cachedVideosCount = count,
            totalSizeBytes = totalSize,
            totalSizeMB = totalSize / (1024.0 * 1024.0),
            cacheDirectory = cacheDir.absolutePath
        )
    }
    
    data class CacheStats(
        val cachedVideosCount: Int,
        val totalSizeBytes: Long,
        val totalSizeMB: Double,
        val cacheDirectory: String
    )
    
    /**
     * Enforce cache limits (LRU eviction)
     */
    private fun enforceCacheLimits() {
        // Remove oldest videos if exceeding count limit
        while (cacheIndex.size > maxCachedVideos) {
            val oldest = cacheIndex.values.minByOrNull { it.timestamp }
            if (oldest != null) {
                Log.d(TAG, "Evicting oldest cached video: ${oldest.videoUrl}")
                File(oldest.localPath).delete()
                cacheIndex.remove(oldest.videoUrl)
            }
        }
        
        // Remove videos if exceeding size limit
        var totalSize = cacheIndex.values.sumOf { it.fileSize }
        while (totalSize > maxCacheSize && cacheIndex.isNotEmpty()) {
            val oldest = cacheIndex.values.minByOrNull { it.timestamp }
            if (oldest != null) {
                Log.d(TAG, "Evicting video to reduce cache size: ${oldest.videoUrl}")
                File(oldest.localPath).delete()
                cacheIndex.remove(oldest.videoUrl)
                totalSize = cacheIndex.values.sumOf { it.fileSize }
            }
        }
    }
    
    /**
     * Generate unique filename from URL
     */
    private fun generateFileName(url: String): String {
        val hash = MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "video_$hash.mp4"
    }
    
    /**
     * Load cache index from disk
     */
    private fun loadCacheIndex() {
        cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && file.extension == "mp4") {
                Log.d(TAG, "Found cached video: ${file.name}, size: ${file.length() / 1024}KB")
                // Note: We can't restore the original URL without metadata
                // In production, use a metadata file or database
            }
        }
    }
}

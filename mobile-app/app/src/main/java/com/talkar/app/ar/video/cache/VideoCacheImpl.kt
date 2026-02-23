package com.talkar.app.ar.video.cache

import android.content.Context
import android.util.Log
import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.LipCoordinates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Implementation of VideoCache using Room database and file system.
 * 
 * Stores video files in app's cache directory and metadata in SQLite database.
 * Implements 24-hour expiration and 500MB cache limit with LRU eviction.
 */
class VideoCacheImpl(
    private val context: Context
) : VideoCache {
    
    private val cacheDir: File = File(context.cacheDir, "lip_videos").apply {
        if (!exists()) mkdirs()
    }
    
    private val database: VideoCacheDatabase = VideoCacheDatabase.getInstance(context)
    private val cacheDao: CacheDao = database.cacheDao()
    
    companion object {
        private const val TAG = "VideoCache"
    }
    
    override suspend fun store(
        posterId: String,
        videoPath: String,
        lipCoordinates: LipCoordinates,
        checksum: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Storing video for poster: $posterId")
            
            // Validate checksum
            val actualChecksum = calculateChecksum(videoPath)
            if (actualChecksum != checksum) {
                Log.e(TAG, "❌ Checksum mismatch: expected=$checksum, actual=$actualChecksum")
                return@withContext Result.failure(
                    TalkingPhotoError.CacheCorrupted(posterId)
                )
            }
            
            // Copy to cache directory
            val sourceFile = File(videoPath)
            val cacheFile = File(cacheDir, "$posterId.mp4")
            sourceFile.copyTo(cacheFile, overwrite = true)
            
            // Store metadata in database
            val entry = CacheEntry(
                posterId = posterId,
                videoPath = cacheFile.absolutePath,
                lipX = lipCoordinates.lipX,
                lipY = lipCoordinates.lipY,
                lipWidth = lipCoordinates.lipWidth,
                lipHeight = lipCoordinates.lipHeight,
                checksum = checksum,
                cachedAt = System.currentTimeMillis(),
                sizeBytes = cacheFile.length()
            )
            
            cacheDao.insert(entry)
            
            Log.d(TAG, "✅ Video stored: $posterId (${cacheFile.length()} bytes)")
            
            // Enforce cache limit
            enforceLimit()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error storing video", e)
            Result.failure(e)
        }
    }
    
    override suspend fun retrieve(posterId: String): CachedVideo? = withContext(Dispatchers.IO) {
        try {
            val entry = cacheDao.get(posterId) ?: return@withContext null
            
            // Check expiration
            val cachedVideo = entry.toCachedVideo()
            if (cachedVideo.isExpired()) {
                Log.d(TAG, "Video expired: $posterId")
                delete(posterId)
                return@withContext null
            }
            
            // Validate checksum
            val actualChecksum = calculateChecksum(entry.videoPath)
            if (actualChecksum != entry.checksum) {
                Log.e(TAG, "❌ Cached video corrupted: $posterId")
                delete(posterId)
                return@withContext null
            }
            
            // Verify file exists
            val file = File(entry.videoPath)
            if (!file.exists()) {
                Log.e(TAG, "❌ Cached video file not found: $posterId")
                cacheDao.delete(posterId)
                return@withContext null
            }
            
            Log.d(TAG, "✅ Retrieved cached video: $posterId")
            cachedVideo
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving video", e)
            null
        }
    }

    
    override suspend fun isCached(posterId: String): Boolean = withContext(Dispatchers.IO) {
        val entry = cacheDao.get(posterId) ?: return@withContext false
        val cachedVideo = entry.toCachedVideo()
        !cachedVideo.isExpired()
    }
    
    override suspend fun validateIntegrity(posterId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val entry = cacheDao.get(posterId) ?: return@withContext false
            val actualChecksum = calculateChecksum(entry.videoPath)
            actualChecksum == entry.checksum
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating integrity", e)
            false
        }
    }
    
    override suspend fun cleanupExpired(): Int = withContext(Dispatchers.IO) {
        try {
            val expirationTimestamp = System.currentTimeMillis() - CachedVideo.CACHE_RETENTION_MS
            
            // Get expired entries
            val allEntries = cacheDao.getAll()
            val expiredEntries = allEntries.filter { it.cachedAt < expirationTimestamp }
            
            // Delete files and database entries
            var deletedCount = 0
            expiredEntries.forEach { entry ->
                val file = File(entry.videoPath)
                if (file.exists()) {
                    file.delete()
                }
                cacheDao.delete(entry.posterId)
                deletedCount++
            }
            
            if (deletedCount > 0) {
                Log.d(TAG, "🗑️ Cleaned up $deletedCount expired videos")
            }
            
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cleaning up expired videos", e)
            0
        }
    }
    
    override suspend fun enforceLimit(): Int = withContext(Dispatchers.IO) {
        try {
            val totalSize = getTotalSize()
            
            if (totalSize <= CachedVideo.MAX_CACHE_SIZE_BYTES) {
                return@withContext 0
            }
            
            Log.d(TAG, "⚠️ Cache size ($totalSize bytes) exceeds limit, enforcing LRU eviction")
            
            // Get all entries sorted by age (oldest first)
            val entries = cacheDao.getAllSortedByAge()
            var currentSize = totalSize
            var deletedCount = 0
            
            // Delete oldest entries until under limit
            for (entry in entries) {
                if (currentSize <= CachedVideo.MAX_CACHE_SIZE_BYTES) {
                    break
                }
                
                val file = File(entry.videoPath)
                if (file.exists()) {
                    file.delete()
                }
                cacheDao.delete(entry.posterId)
                currentSize -= entry.sizeBytes
                deletedCount++
                
                Log.d(TAG, "🗑️ Evicted: ${entry.posterId} (${entry.sizeBytes} bytes)")
            }
            
            Log.d(TAG, "✅ Evicted $deletedCount videos, new size: $currentSize bytes")
            deletedCount
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enforcing cache limit", e)
            0
        }
    }
    
    override suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Clearing all cached videos")
            
            // Delete all files
            cacheDir.listFiles()?.forEach { it.delete() }
            
            // Clear database
            cacheDao.deleteAll()
            
            Log.d(TAG, "✅ Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing cache", e)
        }
    }
    
    override suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) {
        cacheDao.getTotalSize() ?: 0L
    }
    
    /**
     * Deletes a cached video and its metadata.
     */
    private suspend fun delete(posterId: String) {
        val entry = cacheDao.get(posterId)
        if (entry != null) {
            val file = File(entry.videoPath)
            if (file.exists()) {
                file.delete()
            }
            cacheDao.delete(posterId)
        }
    }
    
    /**
     * Calculates SHA-256 checksum of a file.
     * 
     * @param filePath Path to the file
     * @return SHA-256 checksum in format "sha256:hexstring"
     */
    private fun calculateChecksum(filePath: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val file = File(filePath)
        
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        
        val hashBytes = digest.digest()
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        return "sha256:$hexString"
    }
}

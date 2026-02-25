package com.talkar.app.ar.video.cache

import android.util.Log
import com.talkar.app.ar.video.errors.ErrorHandler
import com.talkar.app.ar.video.errors.TalkingPhotoError
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Error handling for video cache operations.
 * 
 * Handles:
 * - Cache corruption (checksum validation failures)
 * - Storage full errors
 * - File I/O errors
 * - Automatic recovery (delete and re-download)
 * 
 * Requirements: 16.1, 16.2, 16.3, 16.4, 16.5
 */
object CacheErrorHandler {
    
    private const val TAG = "CacheErrorHandler"
    
    /**
     * Validates video file integrity using checksum.
     * 
     * If validation fails, automatically deletes the corrupted file.
     * 
     * Requirements: 16.2, 16.3, 16.4
     */
    fun validateChecksum(
        videoPath: String,
        expectedChecksum: String,
        posterId: String
    ): Result<Unit> {
        return try {
            val file = File(videoPath)
            if (!file.exists()) {
                return Result.failure(
                    TalkingPhotoError.CacheCorrupted(posterId)
                )
            }
            
            val actualChecksum = calculateChecksum(file)
            
            if (actualChecksum == expectedChecksum) {
                Log.d(TAG, "Checksum validation passed for poster: $posterId")
                Result.success(Unit)
            } else {
                Log.w(TAG, "Checksum mismatch for poster: $posterId")
                Log.w(TAG, "Expected: $expectedChecksum, Actual: $actualChecksum")
                
                // Delete corrupted file
                if (file.delete()) {
                    Log.i(TAG, "Deleted corrupted file: $videoPath")
                }
                
                val error = TalkingPhotoError.CacheCorrupted(posterId)
                ErrorHandler.logError(
                    error = error,
                    component = "VideoCache",
                    context = mapOf(
                        "poster_id" to posterId,
                        "video_path" to videoPath,
                        "expected_checksum" to expectedChecksum,
                        "actual_checksum" to actualChecksum
                    )
                )
                
                Result.failure(error)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating checksum for poster: $posterId", e)
            Result.failure(
                TalkingPhotoError.CacheCorrupted(posterId)
            )
        }
    }
    
    /**
     * Calculates SHA-256 checksum for a file.
     * 
     * Requirement: 16.1
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Handles storage full errors.
     * 
     * Triggers LRU eviction to free up space.
     * 
     * Requirement: 15.1
     */
    fun handleStorageFull(
        requiredBytes: Long,
        onEvict: () -> Unit
    ): Result<Unit> {
        return try {
            Log.w(TAG, "Storage full, triggering LRU eviction")
            Log.d(TAG, "Required space: ${requiredBytes / 1024 / 1024}MB")
            
            onEvict()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to free up storage", e)
            Result.failure(
                TalkingPhotoError.GenerationFailed(
                    "Insufficient storage. Please clear some space."
                )
            )
        }
    }
    
    /**
     * Handles cache retrieval errors.
     * 
     * Automatically deletes corrupted files and returns null for cache miss.
     * 
     * Requirements: 16.4, 16.5
     */
    fun handleRetrievalError(
        posterId: String,
        videoPath: String,
        exception: Exception
    ): Result<CachedVideo?> {
        Log.w(TAG, "Cache retrieval error for poster: $posterId", exception)
        
        // Delete potentially corrupted file
        val file = File(videoPath)
        if (file.exists()) {
            if (file.delete()) {
                Log.i(TAG, "Deleted corrupted cache file: $videoPath")
            }
        }
        
        // Return null for cache miss (will trigger re-download)
        return Result.success(null)
    }
    
    /**
     * Handles cache storage errors.
     * 
     * Logs error and returns failure to trigger retry.
     */
    fun handleStorageError(
        posterId: String,
        exception: Exception
    ): Result<Unit> {
        val error = when (exception) {
            is IOException -> {
                if (exception.message?.contains("No space left") == true) {
                    TalkingPhotoError.GenerationFailed(
                        "Storage full. Please clear some space."
                    )
                } else {
                    TalkingPhotoError.GenerationFailed(
                        "Failed to save video: ${exception.message}"
                    )
                }
            }
            else -> {
                TalkingPhotoError.GenerationFailed(
                    "Cache storage error: ${exception.message}"
                )
            }
        }
        
        ErrorHandler.logError(
            error = error,
            component = "VideoCache",
            context = mapOf(
                "poster_id" to posterId,
                "operation" to "store"
            )
        )
        
        return Result.failure(error)
    }
    
    /**
     * Validates cache entry before retrieval.
     * 
     * Checks:
     * - File exists
     * - Not expired (24 hours)
     * - Checksum valid
     * 
     * Requirements: 5.2, 16.2
     */
    fun validateCacheEntry(
        videoPath: String,
        cachedAt: Long,
        checksum: String,
        posterId: String
    ): Result<Unit> {
        // Check file exists
        val file = File(videoPath)
        if (!file.exists()) {
            Log.w(TAG, "Cache file does not exist: $videoPath")
            return Result.failure(TalkingPhotoError.CacheCorrupted(posterId))
        }
        
        // Check expiration (24 hours)
        val age = System.currentTimeMillis() - cachedAt
        val maxAge = 24 * 60 * 60 * 1000L // 24 hours
        if (age > maxAge) {
            Log.d(TAG, "Cache entry expired for poster: $posterId")
            file.delete()
            return Result.failure(TalkingPhotoError.CacheCorrupted(posterId))
        }
        
        // Validate checksum
        return validateChecksum(videoPath, checksum, posterId)
    }
}

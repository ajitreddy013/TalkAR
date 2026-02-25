package com.talkar.app.ar.video.cache

import com.talkar.app.ar.video.models.LipCoordinates

/**
 * Interface for managing local storage of lip-sync videos with 24-hour retention.
 * 
 * Implements LRU eviction when cache exceeds 500MB limit and validates video integrity
 * using SHA-256 checksums.
 */
interface VideoCache {
    /**
     * Stores video with metadata.
     * 
     * @param posterId Unique poster identifier
     * @param videoPath Local file path to the video
     * @param lipCoordinates Lip region coordinates
     * @param checksum SHA-256 checksum for integrity validation
     * @return Result indicating success or failure
     */
    suspend fun store(
        posterId: String,
        videoPath: String,
        lipCoordinates: LipCoordinates,
        checksum: String
    ): Result<Unit>
    
    /**
     * Retrieves cached video if available and not expired.
     * 
     * @param posterId Unique poster identifier
     * @return CachedVideo if found and valid, null otherwise
     */
    suspend fun retrieve(posterId: String): CachedVideo?
    
    /**
     * Checks if video is cached and valid.
     * 
     * @param posterId Unique poster identifier
     * @return true if cached and not expired
     */
    suspend fun isCached(posterId: String): Boolean
    
    /**
     * Validates cached video integrity using checksum.
     * 
     * @param posterId Unique poster identifier
     * @return true if checksum matches, false if corrupted or not found
     */
    suspend fun validateIntegrity(posterId: String): Boolean
    
    /**
     * Deletes expired videos (>24 hours old).
     * 
     * @return Number of videos deleted
     */
    suspend fun cleanupExpired(): Int
    
    /**
     * Deletes oldest videos when cache exceeds 500MB.
     * 
     * @return Number of videos deleted
     */
    suspend fun enforceLimit(): Int
    
    /**
     * Clears all cached videos.
     */
    suspend fun clear()
    
    /**
     * Gets total cache size in bytes.
     * 
     * @return Total size of all cached videos
     */
    suspend fun getTotalSize(): Long
}

/**
 * Represents a cached video with metadata.
 * 
 * @property posterId Unique poster identifier
 * @property videoPath Local file path to the video
 * @property lipCoordinates Lip region coordinates
 * @property checksum SHA-256 checksum
 * @property cachedAt Timestamp when video was cached (milliseconds)
 * @property sizeBytes Video file size in bytes
 */
data class CachedVideo(
    val posterId: String,
    val videoPath: String,
    val lipCoordinates: LipCoordinates,
    val checksum: String,
    val cachedAt: Long,
    val sizeBytes: Long
) {
    companion object {
        /**
         * Cache retention period: 24 hours in milliseconds.
         */
        const val CACHE_RETENTION_MS = 24 * 60 * 60 * 1000L
        
        /**
         * Maximum cache size: 500MB in bytes.
         */
        const val MAX_CACHE_SIZE_BYTES = 500L * 1024 * 1024
    }
    
    /**
     * Checks if the cached video has expired (>24 hours old).
     * 
     * @return true if expired
     */
    fun isExpired(): Boolean {
        val ageMs = System.currentTimeMillis() - cachedAt
        return ageMs > CACHE_RETENTION_MS
    }
    
    /**
     * Gets the age of the cached video in milliseconds.
     * 
     * @return Age in milliseconds
     */
    fun getAgeMs(): Long {
        return System.currentTimeMillis() - cachedAt
    }
}

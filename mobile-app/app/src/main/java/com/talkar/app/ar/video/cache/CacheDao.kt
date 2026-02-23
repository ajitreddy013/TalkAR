package com.talkar.app.ar.video.cache

import androidx.room.*

/**
 * Room DAO for video cache operations.
 */
@Dao
interface CacheDao {
    /**
     * Inserts or replaces a cache entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CacheEntry)
    
    /**
     * Gets a cache entry by poster ID.
     */
    @Query("SELECT * FROM video_cache WHERE posterId = :posterId")
    suspend fun get(posterId: String): CacheEntry?
    
    /**
     * Gets all cache entries.
     */
    @Query("SELECT * FROM video_cache")
    suspend fun getAll(): List<CacheEntry>
    
    /**
     * Gets all cache entries sorted by cached time (oldest first).
     */
    @Query("SELECT * FROM video_cache ORDER BY cachedAt ASC")
    suspend fun getAllSortedByAge(): List<CacheEntry>
    
    /**
     * Deletes a cache entry by poster ID.
     */
    @Query("DELETE FROM video_cache WHERE posterId = :posterId")
    suspend fun delete(posterId: String)
    
    /**
     * Deletes all cache entries.
     */
    @Query("DELETE FROM video_cache")
    suspend fun deleteAll()
    
    /**
     * Deletes entries older than the specified timestamp.
     */
    @Query("DELETE FROM video_cache WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    /**
     * Gets total size of all cached videos.
     */
    @Query("SELECT SUM(sizeBytes) FROM video_cache")
    suspend fun getTotalSize(): Long?
    
    /**
     * Gets count of cached videos.
     */
    @Query("SELECT COUNT(*) FROM video_cache")
    suspend fun getCount(): Int
}

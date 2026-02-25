package com.talkar.app.ar.video.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.talkar.app.ar.video.models.LipCoordinates

/**
 * Room entity representing a cached video entry.
 */
@Entity(tableName = "video_cache")
data class CacheEntry(
    @PrimaryKey
    val posterId: String,
    val videoPath: String,
    val lipX: Float,
    val lipY: Float,
    val lipWidth: Float,
    val lipHeight: Float,
    val checksum: String,
    val cachedAt: Long,
    val sizeBytes: Long
) {
    /**
     * Converts database entity to domain model.
     */
    fun toCachedVideo(): CachedVideo {
        return CachedVideo(
            posterId = posterId,
            videoPath = videoPath,
            lipCoordinates = LipCoordinates(
                lipX = lipX,
                lipY = lipY,
                lipWidth = lipWidth,
                lipHeight = lipHeight
            ),
            checksum = checksum,
            cachedAt = cachedAt,
            sizeBytes = sizeBytes
        )
    }
    
    companion object {
        /**
         * Creates database entity from domain model.
         */
        fun fromCachedVideo(video: CachedVideo): CacheEntry {
            return CacheEntry(
                posterId = video.posterId,
                videoPath = video.videoPath,
                lipX = video.lipCoordinates.lipX,
                lipY = video.lipCoordinates.lipY,
                lipWidth = video.lipCoordinates.lipWidth,
                lipHeight = video.lipCoordinates.lipHeight,
                checksum = video.checksum,
                cachedAt = video.cachedAt,
                sizeBytes = video.sizeBytes
            )
        }
    }
}

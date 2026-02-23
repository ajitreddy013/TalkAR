package com.talkar.app.ar.video.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database for video cache.
 */
@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class VideoCacheDatabase : RoomDatabase() {
    
    abstract fun cacheDao(): CacheDao
    
    companion object {
        @Volatile
        private var INSTANCE: VideoCacheDatabase? = null
        
        /**
         * Gets the singleton database instance.
         */
        fun getInstance(context: Context): VideoCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VideoCacheDatabase::class.java,
                    "video_cache_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.talkar.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.talkar.app.data.models.ImageRecognition

@Database(
    entities = [ImageRecognition::class],
    version = 1,
    exportSchema = false
)
abstract class ImageDatabase : RoomDatabase() {
    
    abstract fun imageDao(): ImageDao
    
    companion object {
        @Volatile
        private var INSTANCE: ImageDatabase? = null
        
        fun getDatabase(context: Context): ImageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ImageDatabase::class.java,
                    "image_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


package com.talkar.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.ScannedProduct

@Database(
    entities = [ImageRecognition::class, ScannedProduct::class],
    version = 2,
    exportSchema = false
)
abstract class ImageDatabase : RoomDatabase() {
    
    abstract fun imageDao(): ImageDao
    abstract fun scannedProductDao(): ScannedProductDao
    
    companion object {
        @Volatile
        private var INSTANCE: ImageDatabase? = null
        
        fun getDatabase(context: Context): ImageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ImageDatabase::class.java,
                    "image_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
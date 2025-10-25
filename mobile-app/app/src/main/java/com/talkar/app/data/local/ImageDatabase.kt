package com.talkar.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.ScannedProduct
import com.talkar.app.data.models.Feedback

@Database(
    entities = [ImageRecognition::class, ScannedProduct::class, Feedback::class],
    version = 3,
    exportSchema = false
)
abstract class ImageDatabase : RoomDatabase() {
    
    abstract fun imageDao(): ImageDao
    abstract fun scannedProductDao(): ScannedProductDao
    abstract fun feedbackDao(): FeedbackDao
    
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
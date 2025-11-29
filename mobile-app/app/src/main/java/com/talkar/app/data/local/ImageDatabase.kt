package com.talkar.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.talkar.app.data.models.ImageRecognition
import com.talkar.app.data.models.ScannedProduct
import com.talkar.app.data.models.Feedback

@Database(
    entities = [ImageRecognition::class, ScannedProduct::class, Feedback::class],
    version = 4,
    exportSchema = false
)
abstract class ImageDatabase : RoomDatabase() {
    
    abstract fun imageDao(): ImageDao
    abstract fun scannedProductDao(): ScannedProductDao
    abstract fun feedbackDao(): FeedbackDao
    
    companion object {
        @Volatile
        private var INSTANCE: ImageDatabase? = null
        
        // Migration from version 3 to 4 - add indexes for better query performance
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add index on imagerecognition table for faster queries by id
                database.execSQL("CREATE INDEX IF NOT EXISTS index_imagerecognition_id ON imagerecognition(id)")
                
                // Add index on scanned_products table for faster queries by scannedAt
                database.execSQL("CREATE INDEX IF NOT EXISTS index_scanned_products_scannedAt ON scanned_products(scannedAt)")
                
                // Add index on feedback table for faster queries by synced status
                database.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_synced ON feedback(synced)")
                
                // Add index on feedback table for faster queries by timestamp
                database.execSQL("CREATE INDEX IF NOT EXISTS index_feedback_timestamp ON feedback(timestamp)")
            }
        }
        
        fun getDatabase(context: Context): ImageDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ImageDatabase::class.java,
                    "image_database"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
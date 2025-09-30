package com.talkar.app.data.local

import androidx.room.*
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    
    @Query("SELECT * FROM imagerecognition")
    fun getAllImages(): Flow<List<ImageRecognition>>
    
    @Query("SELECT * FROM imagerecognition WHERE id = :id")
    fun getImageById(id: String): Flow<ImageRecognition?>
    
    @Query("SELECT * FROM imagerecognition WHERE name LIKE :query OR description LIKE :query")
    fun searchImages(query: String): Flow<List<ImageRecognition>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageRecognition)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ImageRecognition>)
    
    @Update
    suspend fun update(image: ImageRecognition)
    
    @Delete
    suspend fun delete(image: ImageRecognition)
    
    @Query("DELETE FROM imagerecognition")
    suspend fun deleteAll()
}


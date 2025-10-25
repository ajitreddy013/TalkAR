package com.talkar.app.data.local

import androidx.room.*
import com.talkar.app.data.models.Feedback
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    
    @Query("SELECT * FROM feedback")
    fun getAllFeedback(): Flow<List<Feedback>>
    
    @Query("SELECT * FROM feedback WHERE id = :id")
    fun getFeedbackById(id: String): Flow<Feedback?>
    
    @Query("SELECT * FROM feedback WHERE synced = 0")
    fun getUnsyncedFeedback(): Flow<List<Feedback>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: Feedback)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(feedbacks: List<Feedback>)
    
    @Update
    suspend fun update(feedback: Feedback)
    
    @Delete
    suspend fun delete(feedback: Feedback)
    
    @Query("DELETE FROM feedback")
    suspend fun deleteAll()
    
    @Query("UPDATE feedback SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
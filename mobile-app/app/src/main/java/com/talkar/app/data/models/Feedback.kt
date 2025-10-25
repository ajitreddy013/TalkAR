package com.talkar.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.talkar.app.data.local.Converters

@Entity(tableName = "feedback")
@TypeConverters(Converters::class)
data class Feedback(
    @PrimaryKey
    val id: String,
    
    val adContentId: String,
    
    val productName: String,
    
    val isPositive: Boolean,
    
    val timestamp: Long,
    
    val synced: Boolean = false
)
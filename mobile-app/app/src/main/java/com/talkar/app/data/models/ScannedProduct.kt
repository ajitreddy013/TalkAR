package com.talkar.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.talkar.app.data.local.Converters

@Entity(tableName = "scanned_products")
@TypeConverters(Converters::class)
data class ScannedProduct(
    @PrimaryKey
    val id: String,
    
    val name: String,
    
    val scannedAt: Long
)
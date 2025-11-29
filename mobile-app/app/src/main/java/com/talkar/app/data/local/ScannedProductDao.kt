package com.talkar.app.data.local

import androidx.room.*
import com.talkar.app.data.models.ScannedProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedProductDao {
    
    @Query("SELECT * FROM scanned_products ORDER BY scannedAt DESC LIMIT 3")
    fun getRecentProducts(): Flow<List<ScannedProduct>>
    
    @Query("SELECT * FROM scanned_products ORDER BY scannedAt DESC LIMIT :limit")
    fun getRecentProducts(limit: Int): Flow<List<ScannedProduct>>
    
    @Query("SELECT * FROM scanned_products WHERE id = :id")
    fun getProductById(id: String): Flow<ScannedProduct?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ScannedProduct)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ScannedProduct>)
    
    @Update
    suspend fun update(product: ScannedProduct)
    
    @Delete
    suspend fun delete(product: ScannedProduct)
    
    @Query("DELETE FROM scanned_products")
    suspend fun deleteAll()
    
    @Query("DELETE FROM scanned_products WHERE scannedAt < :threshold")
    suspend fun deleteOldProducts(threshold: Long): Int
}
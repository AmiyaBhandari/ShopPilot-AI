package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PurchaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY createdAt DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getPurchasesForSupplier(supplierId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE createdAt >= :startTime AND createdAt <= :endTime ORDER BY createdAt DESC")
    fun getPurchasesBetween(startTime: Long, endTime: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: String): PurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<PurchaseEntity>)

    @Query("DELETE FROM purchases WHERE id = :id")
    suspend fun deletePurchaseById(id: String)

    @Query("DELETE FROM purchases")
    suspend fun deleteAll()
}

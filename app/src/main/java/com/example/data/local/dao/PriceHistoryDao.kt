package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history ORDER BY createdAt DESC")
    fun getAllPriceHistory(): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY createdAt DESC")
    fun getPriceHistoryForProduct(productId: String): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE newCostPrice > oldCostPrice AND oldCostPrice > 0 ORDER BY createdAt DESC")
    fun getCostIncreases(): Flow<List<PriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(priceHistory: PriceHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistories(priceHistories: List<PriceHistoryEntity>)

    @Query("DELETE FROM price_history")
    suspend fun deleteAll()
}

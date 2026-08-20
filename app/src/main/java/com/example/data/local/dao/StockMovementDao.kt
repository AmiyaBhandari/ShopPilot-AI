package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentMovements(limit: Int): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestMovement(): StockMovementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements WHERE id = :id")
    suspend fun deleteMovementById(id: String)

    @Query("DELETE FROM stock_movements")
    suspend fun deleteAll()
}

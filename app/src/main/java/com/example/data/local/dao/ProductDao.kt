package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun getProductByIdFlow(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT DISTINCT category FROM products WHERE active = 1")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE active = 1 AND currentStock <= minimumStock ORDER BY currentStock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE active = 1 AND currentStock <= 0")
    fun getOutOfStockProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET currentStock = :newStock, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStock(id: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET reservedStock = :newReserved, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateReservedStock(id: String, newReserved: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET costPrice = :cost, sellingPrice = :selling, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updatePrices(id: String, cost: Double, selling: Double, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAll()
}

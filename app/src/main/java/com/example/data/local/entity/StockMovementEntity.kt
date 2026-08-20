package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class StockMovementType {
    PURCHASE,
    SALE,
    ADJUSTMENT,
    RETURN_FROM_CUSTOMER,
    RETURN_TO_SUPPLIER,
    DAMAGE_LOSS,
    RESERVED,
    UNRESERVED
}

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val type: StockMovementType,
    val quantity: Double,
    val previousStock: Double,
    val newStock: Double,
    val reason: String = "",
    val referenceId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

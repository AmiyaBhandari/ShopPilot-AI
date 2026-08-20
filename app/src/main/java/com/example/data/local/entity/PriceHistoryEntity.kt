package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "price_history")
data class PriceHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val oldCostPrice: Double,
    val newCostPrice: Double,
    val oldSellingPrice: Double,
    val newSellingPrice: Double,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

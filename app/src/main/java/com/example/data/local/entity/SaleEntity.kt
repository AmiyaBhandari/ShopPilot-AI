package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

data class SaleItem(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val costPrice: Double,
    val total: Double
)

enum class PaymentMethod {
    CASH,
    UPI,
    BANK,
    CREDIT,
    OTHER
}

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemsJson: String, // JSON serialized List<SaleItem>
    val itemCount: Int = 1,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val estimatedCost: Double = 0.0,
    val estimatedGrossProfit: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val customerId: String? = null,
    val customerName: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

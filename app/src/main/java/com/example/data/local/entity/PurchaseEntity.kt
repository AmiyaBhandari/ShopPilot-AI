package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

data class PurchaseItem(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitCost: Double,
    val total: Double
)

enum class PaymentStatus {
    PAID,
    UNPAID,
    PARTIAL
}

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val supplierId: String? = null,
    val supplierName: String = "",
    val invoiceNumber: String = "",
    val itemsJson: String, // JSON serialized List<PurchaseItem>
    val itemCount: Int = 1,
    val subtotal: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val total: Double,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val notes: String = "",
    val invoiceImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

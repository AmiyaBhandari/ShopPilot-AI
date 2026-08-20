package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String = "General",
    val brand: String = "",
    val sku: String = "",
    val barcode: String = "",
    val unit: String = "pcs", // pcs, kg, g, pkt, bottle, box, litre
    val variant: String = "",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val reservedStock: Double = 0.0,
    val minimumStock: Double = 5.0,
    val reorderLevel: Double = 10.0,
    val supplierId: String? = null,
    val supplierName: String = "",
    val image: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val active: Boolean = true
) {
    val availableStock: Double
        get() = (currentStock - reservedStock).coerceAtLeast(0.0)

    val profitMarginPercent: Double
        get() = if (sellingPrice > 0) ((sellingPrice - costPrice) / sellingPrice) * 100 else 0.0

    val unitProfit: Double
        get() = sellingPrice - costPrice

    val inventoryCostValue: Double
        get() = currentStock * costPrice

    val inventorySellingValue: Double
        get() = currentStock * sellingPrice

    val potentialGrossProfit: Double
        get() = currentStock * unitProfit
}

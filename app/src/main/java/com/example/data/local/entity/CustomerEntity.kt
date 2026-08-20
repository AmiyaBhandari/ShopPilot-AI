package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String = "",
    val creditBalance: Double = 0.0, // positive means customer owes shop
    val totalPurchases: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_ledger_entries")
data class CustomerLedgerEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val type: String, // "SALE_CREDIT", "PAYMENT_RECEIVED", "ADJUSTMENT"
    val amount: Double,
    val balanceAfter: Double,
    val referenceId: String? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

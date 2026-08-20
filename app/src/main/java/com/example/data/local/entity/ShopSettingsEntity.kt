package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_settings")
data class ShopSettingsEntity(
    @PrimaryKey val id: String = "default_shop",
    val shopName: String = "My Shop",
    val shopType: String = "General Store / Kirana",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val currency: String = "₹",
    val language: String = "Hinglish", // English, Hindi, Hinglish
    val defaultLowStockThreshold: Double = 5.0,
    val deadStockDaysThreshold: Int = 30,
    val setupCompleted: Boolean = false,
    val lastBackupTimestamp: Long = 0L,
    val autoGenerateInsights: Boolean = true
)

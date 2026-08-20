package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class InsightType {
    LOW_STOCK,
    DEAD_STOCK,
    MARGIN_DROP,
    PRICE_INCREASE,
    REORDER_ALERT,
    SALES_TREND,
    GENERAL_BUSINESS
}

enum class InsightSeverity {
    INFO,
    WARNING,
    CRITICAL
}

@Entity(tableName = "ai_insights")
data class AIInsightEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: InsightType,
    val severity: InsightSeverity,
    val title: String,
    val description: String,
    val reasoning: String = "",
    val actionText: String? = null,
    val relatedProductIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val dismissed: Boolean = false
)

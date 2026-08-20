package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AIInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIInsightDao {
    @Query("SELECT * FROM ai_insights WHERE dismissed = 0 ORDER BY createdAt DESC")
    fun getActiveInsights(): Flow<List<AIInsightEntity>>

    @Query("SELECT * FROM ai_insights ORDER BY createdAt DESC")
    fun getAllInsights(): Flow<List<AIInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AIInsightEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<AIInsightEntity>)

    @Query("UPDATE ai_insights SET dismissed = 1 WHERE id = :id")
    suspend fun dismissInsight(id: String)

    @Query("DELETE FROM ai_insights WHERE dismissed = 1")
    suspend fun clearDismissed()

    @Query("DELETE FROM ai_insights")
    suspend fun deleteAll()
}

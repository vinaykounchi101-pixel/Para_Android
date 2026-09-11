package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paradox.app.data.local.entity.AiInsightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiInsightLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AiInsightLogEntity)

    @Query("SELECT * FROM ai_insight_logs WHERE profileId = :profileId ORDER BY generatedAt DESC")
    fun getLogsByProfile(profileId: String): Flow<List<AiInsightLogEntity>>

    @Query("SELECT * FROM ai_insight_logs WHERE profileId = :profileId AND type = :type ORDER BY generatedAt DESC LIMIT :limit")
    fun getLogsByType(profileId: String, type: String, limit: Int = 10): Flow<List<AiInsightLogEntity>>

    @Query("DELETE FROM ai_insight_logs WHERE profileId = :profileId")
    suspend fun clearLogsByProfile(profileId: String)
}

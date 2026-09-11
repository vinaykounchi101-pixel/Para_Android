package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "ai_insight_logs",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "type"]),
        Index(value = ["profileId", "generatedAt"])
    ]
)
data class AiInsightLogEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val type: String, // "HEALTH_SCORE", "LEAK_HUNTER", "SAFE_TO_SPEND", "FORECAST", "ASK_PARADOX"
    val title: String,
    val summary: String,
    val payloadJson: String,
    val isEstimate: Boolean = true,
    val generatedAt: Instant = Instant.now()
)

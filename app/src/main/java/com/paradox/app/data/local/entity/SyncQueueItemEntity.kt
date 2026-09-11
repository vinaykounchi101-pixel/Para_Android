package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "sync_queue",
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
        Index(value = ["profileId", "status"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncQueueItemEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val entityType: String, // "EXPENSE", "INCOME", "BUDGET", "SAVINGS_GOAL", "ACCOUNT"
    val entityId: String,
    val operation: String,  // "INSERT", "UPDATE", "DELETE"
    val payloadJson: String,
    val status: String = "PENDING", // "PENDING", "IN_PROGRESS", "FAILED", "SYNCED"
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

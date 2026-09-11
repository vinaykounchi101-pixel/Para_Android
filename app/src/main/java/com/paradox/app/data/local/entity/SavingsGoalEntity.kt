package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "savings_goals",
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
        Index(value = ["profileId", "targetDate"])
    ]
)
data class SavingsGoalEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val targetAmount: BigDecimal,
    val currentAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "INR",
    val targetDate: LocalDate,
    val colorHex: String = "#10B981",
    val iconName: String = "savings",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

@Entity(
    tableName = "savings_contributions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["goalId"]),
        Index(value = ["goalId", "date"])
    ]
)
data class SavingsContributionEntity(
    @PrimaryKey
    val id: String,
    val goalId: String,
    val profileId: String,
    val amount: BigDecimal,
    val currency: String = "INR",
    val date: LocalDate,
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)

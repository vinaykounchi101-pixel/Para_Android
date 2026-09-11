package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "type", "categoryId"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val type: String, // "MONTHLY", "DAILY", "WEEKLY", "CATEGORY"
    val amount: BigDecimal,
    val categoryId: String? = null,
    val thresholdPct: Int = 80
)

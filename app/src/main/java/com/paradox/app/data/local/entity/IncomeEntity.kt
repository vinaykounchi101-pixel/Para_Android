package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "incomes",
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
        Index(value = ["profileId", "date"]),
        Index(value = ["profileId", "source"])
    ]
)
data class IncomeEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val source: String,
    val amount: BigDecimal,
    val currency: String,
    val date: LocalDate,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

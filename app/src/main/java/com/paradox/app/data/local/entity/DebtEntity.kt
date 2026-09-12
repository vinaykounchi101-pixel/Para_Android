package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "debts",
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
        Index(value = ["profileId", "debtType"]),
        Index(value = ["profileId", "dueDate"])
    ]
)
data class DebtEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val personName: String,
    val personContactNumber: String? = null,
    val debtType: String, // "LENT" (Receivable) or "BORROWED" (Payable)
    val initialAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val currency: String = "INR",
    val dueDate: LocalDate? = null,
    val notes: String? = null,
    val status: String = "ACTIVE", // "ACTIVE" or "SETTLED"
    val reminderEnabled: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

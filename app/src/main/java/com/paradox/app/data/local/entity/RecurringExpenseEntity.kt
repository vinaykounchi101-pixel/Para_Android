package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "recurring_expenses",
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
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "nextDueDate"]),
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "paymentMethodId"])
    ]
)
data class RecurringExpenseEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val title: String,
    val amount: BigDecimal,
    val currency: String = "INR",
    val categoryId: String,
    val paymentMethodId: String,
    val frequency: String,
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val isActive: Boolean = true,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

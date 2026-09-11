package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "expenses",
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
        Index(value = ["profileId", "date"]),
        Index(value = ["profileId", "categoryId"]),
        Index(value = ["profileId", "paymentMethodId"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val title: String,
    val amount: BigDecimal,
    val currency: String,
    val categoryId: String,
    val paymentMethodId: String,
    val date: LocalDate,
    val notes: String? = null,
    val recurringFlag: Boolean = false,
    val source: String = "MANUAL",
    val attachmentRef: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

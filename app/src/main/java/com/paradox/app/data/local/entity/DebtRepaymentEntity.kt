package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "debt_repayments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
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
        Index(value = ["debtId"]),
        Index(value = ["debtId", "repaymentDate"])
    ]
)
data class DebtRepaymentEntity(
    @PrimaryKey
    val id: String,
    val debtId: String,
    val profileId: String,
    val amount: BigDecimal,
    val currency: String = "INR",
    val repaymentDate: LocalDate = LocalDate.now(),
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)

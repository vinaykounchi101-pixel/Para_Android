package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_methods",
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
        Index(value = ["profileId", "label"], unique = true)
    ]
)
data class PaymentMethodEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val type: String, // "CASH", "UPI", "DEBIT_CARD", "CREDIT_CARD", "BANK_ACCOUNT", "WALLET", "CUSTOM"
    val label: String,
    val isCustom: Boolean = false
)

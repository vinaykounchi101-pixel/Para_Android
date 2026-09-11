package com.paradox.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant

@Entity(
    tableName = "accounts",
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
        Index(value = ["profileId", "name"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val profileId: String,
    val name: String,
    val type: String,
    val currency: String = "INR",
    val initialBalance: BigDecimal = BigDecimal.ZERO,
    val colorHex: String = "#3B82F6",
    val iconName: String = "account_balance_wallet",
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

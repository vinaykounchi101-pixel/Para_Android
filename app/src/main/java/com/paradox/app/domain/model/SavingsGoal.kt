package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

data class SavingsGoal(
    val id: String,
    val profileId: String,
    val name: String,
    val targetAmount: Money,
    val currentAmount: Money = Money.zero(targetAmount.currencyCode),
    val currency: String = targetAmount.currencyCode,
    val targetDate: LocalDate,
    val colorHex: String = "#10B981",
    val iconName: String = "savings",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun progressPercentage(): Double {
        if (!targetAmount.isPositive()) return 0.0
        val ratio = currentAmount.amount.divide(targetAmount.amount, 4, RoundingMode.HALF_EVEN)
        val pct = (ratio * BigDecimal(100)).toDouble()
        return pct.coerceIn(0.0, 100.0)
    }

    fun remainingAmount(): Money {
        val rem = targetAmount.amount.subtract(currentAmount.amount).max(BigDecimal.ZERO)
        return Money(rem, currency)
    }

    fun isAchieved(): Boolean = currentAmount.amount >= targetAmount.amount
}

data class SavingsContribution(
    val id: String,
    val goalId: String,
    val profileId: String,
    val amount: Money,
    val currency: String = amount.currencyCode,
    val date: LocalDate,
    val notes: String? = null,
    val createdAt: Instant = Instant.now()
)

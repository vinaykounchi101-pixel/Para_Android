package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

enum class RecurringFrequency(val displayName: String, val multiplierToMonthly: BigDecimal) {
    DAILY("Daily", BigDecimal("30.4375")),
    WEEKLY("Weekly", BigDecimal("4.3333")),
    BIWEEKLY("Bi-weekly", BigDecimal("2.1667")),
    MONTHLY("Monthly", BigDecimal("1.0")),
    QUARTERLY("Quarterly", BigDecimal("0.3333")),
    YEARLY("Yearly", BigDecimal("0.0833"))
}

data class RecurringExpense(
    val id: String,
    val profileId: String,
    val title: String,
    val amount: Money,
    val currency: String = "INR",
    val categoryId: String,
    val paymentMethodId: String,
    val frequency: RecurringFrequency,
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val isActive: Boolean = true,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun monthlyNormalizedAmount(): Money {
        val normalized = amount.amount.multiply(frequency.multiplierToMonthly).setScale(2, RoundingMode.HALF_EVEN)
        return Money(normalized, currency)
    }
}

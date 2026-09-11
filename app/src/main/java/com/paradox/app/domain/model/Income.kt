package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.time.Instant
import java.time.LocalDate

enum class IncomeSource(val displayName: String) {
    SALARY("Salary"),
    FREELANCE("Freelance"),
    BUSINESS("Business"),
    INVESTMENT("Investment"),
    RENTAL("Rental"),
    GIFT("Gift"),
    OTHER("Other")
}

data class Income(
    val id: String,
    val profileId: String,
    val source: IncomeSource,
    val amount: Money,
    val currency: String,
    val date: LocalDate,
    val notes: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

data class CashFlowSummary(
    val totalIncome: Money,
    val totalExpense: Money,
    val netCashFlow: Money,
    val savingsAmount: Money,
    val savingsRatePct: Double
)

package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

enum class BudgetType {
    MONTHLY,
    DAILY,
    WEEKLY,
    CATEGORY
}

data class Budget(
    val id: String,
    val profileId: String,
    val type: BudgetType,
    val limit: Money,
    val categoryId: String? = null,
    val thresholdPct: Int = 80
)

enum class BudgetHealth {
    ON_TRACK,
    NEAR_LIMIT,
    OVER_BUDGET
}

data class BudgetStatus(
    val budget: Budget,
    val spent: Money,
    val remaining: Money,
    val percentageUsed: Double,
    val health: BudgetHealth
) {
    companion object {
        fun calculate(budget: Budget, spent: Money): BudgetStatus {
            val remainingAmount = budget.limit.amount.subtract(spent.amount)
            val remaining = Money(remainingAmount, budget.limit.currencyCode)

            val percentage = if (budget.limit.isZero()) {
                0.0
            } else {
                spent.amount
                    .divide(budget.limit.amount, 4, RoundingMode.HALF_EVEN)
                    .multiply(BigDecimal(100))
                    .toDouble()
            }

            val health = when {
                percentage >= 100.0 -> BudgetHealth.OVER_BUDGET
                percentage >= budget.thresholdPct.toDouble() -> BudgetHealth.NEAR_LIMIT
                else -> BudgetHealth.ON_TRACK
            }

            return BudgetStatus(
                budget = budget,
                spent = spent,
                remaining = remaining,
                percentageUsed = percentage,
                health = health
            )
        }
    }
}

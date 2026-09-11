package com.paradox.app.domain.model

import com.paradox.app.core.money.Money
import java.math.BigDecimal
import java.time.LocalDate

enum class ExpenseSortOrder {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

data class LedgerFilter(
    val query: String = "",
    val categoryId: String? = null,
    val paymentMethodId: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val minAmount: BigDecimal? = null,
    val maxAmount: BigDecimal? = null,
    val sortBy: ExpenseSortOrder = ExpenseSortOrder.DATE_DESC
)

data class CategorySpendSummary(
    val category: Category,
    val totalSpent: Money,
    val percentageOfTotal: Double
)

data class DaySpendSummary(
    val date: LocalDate,
    val dayLabel: String,
    val totalSpent: Money
)

data class DashboardSummary(
    val totalSpentThisMonth: Money,
    val overallBudgetStatus: BudgetStatus?,
    val safeToSpendToday: Money?,
    val recentExpenses: List<Expense>,
    val categoryBreakdown: List<CategorySpendSummary>,
    val dailyVelocity: List<DaySpendSummary>,
    val totalExpenseCount: Int
)

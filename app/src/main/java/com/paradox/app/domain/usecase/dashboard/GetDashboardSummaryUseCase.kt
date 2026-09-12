package com.paradox.app.domain.usecase.dashboard

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.CategorySpendSummary
import com.paradox.app.domain.model.DashboardSummary
import com.paradox.app.domain.model.DaySpendSummary
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.usecase.budget.CalculateBudgetStatusUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class GetDashboardSummaryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val calculateBudgetStatusUseCase: CalculateBudgetStatusUseCase
) {
    operator fun invoke(
        profileId: String,
        yearMonth: YearMonth = YearMonth.now()
    ): Flow<DashboardSummary> {
        val today = LocalDate.now()
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val daysInMonth = yearMonth.lengthOfMonth()
        val isCurrentMonth = yearMonth == YearMonth.now()
        val remainingDays = if (isCurrentMonth) {
            (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
        } else if (yearMonth.isBefore(YearMonth.now())) {
            1
        } else {
            daysInMonth
        }

        val expensesThisMonthFlow = expenseRepository.getExpensesInRange(profileId, firstDayOfMonth, lastDayOfMonth)
        val categoriesFlow = categoryRepository.getCategories(profileId)
        val budgetStatusFlow = calculateBudgetStatusUseCase(profileId, BudgetType.MONTHLY, firstDayOfMonth, lastDayOfMonth)
        val recentExpensesFlow = expenseRepository.getRecentExpenses(profileId, 5)

        return combine(
            expensesThisMonthFlow,
            categoriesFlow,
            budgetStatusFlow,
            recentExpensesFlow
        ) { expenses, categories, budgetStatus, recentExpenses ->

            var totalAmount = BigDecimal.ZERO
            val categoryAmounts = mutableMapOf<String, BigDecimal>()
            val dailyAmounts = mutableMapOf<LocalDate, BigDecimal>()

            // Velocity chart:
            // If current month, show past 7 days ending today.
            // If past month, show the last 7 days of that month.
            // If future month, show first 7 days.
            val velocityEndDay = if (isCurrentMonth) {
                today
            } else if (yearMonth.isBefore(YearMonth.now())) {
                lastDayOfMonth
            } else {
                firstDayOfMonth.plusDays(6.coerceAtMost((daysInMonth - 1).toLong()))
            }
            val velocityStartDay = velocityEndDay.minusDays(6)
            for (i in 0..6) {
                dailyAmounts[velocityStartDay.plusDays(i.toLong())] = BigDecimal.ZERO
            }

            expenses.forEach { expense ->
                totalAmount = totalAmount.add(expense.money.amount)
                categoryAmounts[expense.categoryId] = (categoryAmounts[expense.categoryId] ?: BigDecimal.ZERO).add(expense.money.amount)
                if (!expense.date.isBefore(velocityStartDay) && !expense.date.isAfter(velocityEndDay)) {
                    dailyAmounts[expense.date] = (dailyAmounts[expense.date] ?: BigDecimal.ZERO).add(expense.money.amount)
                }
            }

            val totalSpentMoney = Money(totalAmount, "INR")

            // Safe to spend calculation based on remaining budget or monthly average
            val safeToSpendToday = if (budgetStatus != null && budgetStatus.remaining.isPositive()) {
                val dailyAllowance = budgetStatus.remaining.amount.divide(
                    BigDecimal.valueOf(remainingDays.toLong()),
                    2,
                    RoundingMode.HALF_EVEN
                )
                Money(dailyAllowance, "INR")
            } else if (budgetStatus != null && !budgetStatus.remaining.isPositive()) {
                Money.ZERO_INR
            } else {
                null
            }

            // Category breakdown
            val categoriesMap = categories.associateBy { it.id }
            val categorySummaries = categoryAmounts.mapNotNull { (catId, catTotal) ->
                val category = categoriesMap[catId] ?: return@mapNotNull null
                val pct = if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    catTotal.divide(totalAmount, 4, RoundingMode.HALF_EVEN).multiply(BigDecimal(100)).toDouble()
                } else 0.0
                CategorySpendSummary(category, Money(catTotal, "INR"), pct)
            }.sortedByDescending { it.totalSpent.amount }

            // Daily velocity
            val dailyVelocity = dailyAmounts.entries.sortedBy { it.key }.map { (date, amount) ->
                val dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                DaySpendSummary(date, dayLabel, Money(amount, "INR"))
            }

            DashboardSummary(
                totalSpentThisMonth = totalSpentMoney,
                overallBudgetStatus = budgetStatus,
                safeToSpendToday = safeToSpendToday,
                recentExpenses = recentExpenses,
                categoryBreakdown = categorySummaries,
                dailyVelocity = dailyVelocity,
                totalExpenseCount = expenses.size
            )
        }
    }
}

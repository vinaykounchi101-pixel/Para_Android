package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.intelligence.FiftyThirtyTwentyRule
import com.paradox.app.domain.model.intelligence.SpendingForecast
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class ForecastSpendingUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(profileId: String, today: LocalDate = LocalDate.now()): SpendingForecast {
        val yearMonth = YearMonth.from(today)
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()
        val totalDays = yearMonth.lengthOfMonth()
        val daysElapsed = maxOf(1, today.dayOfMonth)
        val daysRemaining = maxOf(1, totalDays - today.dayOfMonth + 1)

        val totalSpent = expenseRepository.observeTotalSpentInRange(profileId, startOfMonth, today).first()
        val budget = budgetRepository.getOverallBudget(profileId, BudgetType.MONTHLY).firstOrNull()
        val totalIncome = incomeRepository.observeTotalIncomeInRange(profileId, startOfMonth, endOfMonth).first()
        val categoriesList = categoryRepository.getCategories(profileId).first()
        val categories: Map<String, Category> = categoriesList.associateBy { it.id }
        val expenses = expenseRepository.getExpensesInRange(profileId, startOfMonth, today).first()

        // Daily Burn Rate
        val burnRateAmount = totalSpent.amount.divide(BigDecimal(daysElapsed), 2, RoundingMode.HALF_EVEN)
        val dailyBurnRate = Money.of(burnRateAmount, totalSpent.currencyCode)

        // Projected Month End
        val projectedAmount = burnRateAmount.multiply(BigDecimal(totalDays)).setScale(2, RoundingMode.HALF_EVEN)
        val projectedMonthEnd = Money.of(projectedAmount, totalSpent.currencyCode)

        // Recommended Pace
        val budgetLimit = budget?.limit?.amount ?: totalIncome.amount
        val remainingBudget = if (budgetLimit > totalSpent.amount) budgetLimit.subtract(totalSpent.amount) else BigDecimal.ZERO
        val recommendedPaceAmount = remainingBudget.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_EVEN)
        val recommendedPace = Money.of(recommendedPaceAmount, totalSpent.currencyCode)

        // 50/30/20 Categorization
        val needsKeywords = setOf("housing", "rent", "bills", "groceries", "food", "transport", "healthcare", "utility")
        var needsTotal = BigDecimal.ZERO
        var wantsTotal = BigDecimal.ZERO

        for (exp in expenses) {
            val categoryName = categories[exp.categoryId]?.name?.lowercase() ?: ""
            val title = exp.title.lowercase()
            if (needsKeywords.any { categoryName.contains(it) || title.contains(it) }) {
                needsTotal = needsTotal.add(exp.money.amount)
            } else {
                wantsTotal = wantsTotal.add(exp.money.amount)
            }
        }

        val netSavings = if (totalIncome.isPositive() && totalIncome.amount > totalSpent.amount) {
            totalIncome.amount.subtract(totalSpent.amount)
        } else BigDecimal.ZERO

        val denominator = if (totalIncome.isPositive()) totalIncome.amount else totalSpent.amount
        val needsPct = if (denominator > BigDecimal.ZERO) needsTotal.divide(denominator, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal(100)) else BigDecimal("50.00")
        val wantsPct = if (denominator > BigDecimal.ZERO) wantsTotal.divide(denominator, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal(100)) else BigDecimal("30.00")
        val savingsPct = if (denominator > BigDecimal.ZERO) netSavings.divide(denominator, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal(100)) else BigDecimal("20.00")

        val fiftyThirtyTwenty = FiftyThirtyTwentyRule(
            needsAmount = Money.of(needsTotal, totalSpent.currencyCode),
            needsPct = needsPct,
            wantsAmount = Money.of(wantsTotal, totalSpent.currencyCode),
            wantsPct = wantsPct,
            savingsAmount = Money.of(netSavings, totalSpent.currencyCode),
            savingsPct = savingsPct
        )

        return SpendingForecast(
            currentMonthSpent = totalSpent,
            projectedMonthEnd = projectedMonthEnd,
            dailyBurnRate = dailyBurnRate,
            recommendedPace = recommendedPace,
            fiftyThirtyTwenty = fiftyThirtyTwenty
        )
    }
}

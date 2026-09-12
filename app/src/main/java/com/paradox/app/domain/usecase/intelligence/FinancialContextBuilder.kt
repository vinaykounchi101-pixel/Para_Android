package com.paradox.app.domain.usecase.intelligence

import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.DebtRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.usecase.insights.CalculateFinancialHealthScoreUseCase
import com.paradox.app.domain.usecase.insights.CalculateSafeToSpendUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class FinancialContextBuilder @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val accountRepository: AccountRepository,
    private val debtRepository: DebtRepository,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    private val calculateHealthScoreUseCase: CalculateFinancialHealthScoreUseCase
) {
    suspend fun buildContext(profileId: String): String {
        val today = LocalDate.now()
        val yearMonth = YearMonth.from(today)
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()

        val safeToSpend = calculateSafeToSpendUseCase(profileId, today)
        val healthScore = calculateHealthScoreUseCase(profileId, today)

        val expenses = expenseRepository.getExpensesInRange(profileId, startOfMonth, endOfMonth).first()
        val totalMonthSpend = expenses.fold(java.math.BigDecimal.ZERO) { acc, exp -> acc.add(exp.money.amount) }

        val monthlyBudget = budgetRepository.getOverallBudget(profileId, com.paradox.app.domain.model.BudgetType.MONTHLY).firstOrNull()
        val categories = categoryRepository.getCategories(profileId).firstOrNull() ?: emptyList()
        val categoryMap = categories.associateBy { it.id }

        val subscriptions = recurringRepository.getActiveRecurring(profileId).firstOrNull() ?: emptyList()
        val activeDebts = debtRepository.getActiveDebts(profileId).firstOrNull() ?: emptyList()

        val topCategorySpends = expenses.groupBy { it.categoryId }
            .map { (catId, exps) ->
                val name = categoryMap[catId]?.name ?: "Uncategorized"
                val sum = exps.fold(java.math.BigDecimal.ZERO) { acc, exp -> acc.add(exp.money.amount) }
                "$name: ₹$sum"
            }.take(5)

        val recentExpenses: List<String> = expenses.take(5).map {
            "• ${it.title}: ₹${it.money.amount} (${it.date})"
        }

        val lentTotal = activeDebts
            .filter { it.debtType == com.paradox.app.domain.model.DebtType.LENT }
            .fold(java.math.BigDecimal.ZERO) { acc, debt -> acc.add(debt.remainingAmount.amount) }
        val borrowedTotal = activeDebts
            .filter { it.debtType == com.paradox.app.domain.model.DebtType.BORROWED }
            .fold(java.math.BigDecimal.ZERO) { acc, debt -> acc.add(debt.remainingAmount.amount) }

        return """
            === USER LIVE FINANCIAL SUMMARY (STRICT GROUND TRUTH) ===
            • Today's Date: $today
            • Daily Safe-to-Spend Limit: ₹${safeToSpend.dailySafeAmount.amount} (${safeToSpend.status})
            • Current Month Spend: ₹$totalMonthSpend
            • Monthly Budget: ${if (monthlyBudget != null) "₹${monthlyBudget.limit.amount}" else "No overall budget set"}
            • Remaining Monthly Budget: ₹${safeToSpend.remainingBudget.amount} (${safeToSpend.daysRemaining} days left in month)
            • Financial Health Score: ${healthScore.overallScore}/100 (${healthScore.tier})
            • Top Spending Categories This Month: ${if (topCategorySpends.isNotEmpty()) topCategorySpends.joinToString(", ") else "None logged yet"}
            • Active Recurring Subscriptions: ${if (subscriptions.isNotEmpty()) subscriptions.joinToString { "${it.title} (₹${it.amount.amount}/${it.frequency})" } else "None"}
            • Unsettled Debts: Lent to others = ₹$lentTotal, Borrowed from others = ₹$borrowedTotal
            • Recent Transactions:
            ${if (recentExpenses.isNotEmpty()) recentExpenses.joinToString("\n") else "No recent transactions"}
            ==========================================================
        """.trimIndent()
    }
}

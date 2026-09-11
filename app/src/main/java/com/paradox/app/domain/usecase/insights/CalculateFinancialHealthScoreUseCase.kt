package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.intelligence.ActionableTip
import com.paradox.app.domain.model.intelligence.FinancialHealthPillar
import com.paradox.app.domain.model.intelligence.FinancialHealthScore
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

class CalculateFinancialHealthScoreUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(profileId: String, today: LocalDate = LocalDate.now()): FinancialHealthScore {
        val yearMonth = YearMonth.from(today)
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()

        val totalIncome = incomeRepository.observeTotalIncomeInRange(profileId, startOfMonth, endOfMonth).first()
        val totalSpent = expenseRepository.observeTotalSpentInRange(profileId, startOfMonth, endOfMonth).first()
        val budget = budgetRepository.getOverallBudget(profileId, BudgetType.MONTHLY).firstOrNull()
        val accounts = accountRepository.getAccounts(profileId).first()
        val expenses = expenseRepository.getExpensesInRange(profileId, startOfMonth, today).first()
        val recurring = recurringRepository.getActiveRecurring(profileId).first()

        val tips = mutableListOf<ActionableTip>()

        // 1. Savings Discipline (0 - 20)
        var savingsScore = 10
        var savingsSummary = "Moderate savings discipline."
        if (totalIncome.isPositive()) {
            val netSaved = totalIncome.amount.subtract(totalSpent.amount)
            val savingsRate = if (netSaved > BigDecimal.ZERO) {
                netSaved.divide(totalIncome.amount, 2, RoundingMode.HALF_EVEN)
            } else BigDecimal.ZERO

            when {
                savingsRate >= BigDecimal("0.20") -> {
                    savingsScore = 20
                    savingsSummary = "Excellent savings rate (${savingsRate.multiply(BigDecimal(100)).toInt()}%) exceeding the 20% benchmark."
                }
                savingsRate >= BigDecimal("0.10") -> {
                    savingsScore = 15
                    savingsSummary = "Healthy savings rate of ${savingsRate.multiply(BigDecimal(100)).toInt()}%."
                }
                savingsRate > BigDecimal.ZERO -> {
                    savingsScore = 10
                    savingsSummary = "Positive net cash flow, but savings rate is below the 10% threshold."
                    tips.add(
                        ActionableTip(
                            id = UUID.randomUUID().toString(),
                            priority = 2,
                            title = "Boost Monthly Savings",
                            description = "Aim to allocate at least 15% of monthly income to savings goals before discretionary spending."
                        )
                    )
                }
                else -> {
                    savingsScore = 4
                    savingsSummary = "Negative cash flow this month. Expenses exceed total recorded income."
                    tips.add(
                        ActionableTip(
                            id = UUID.randomUUID().toString(),
                            priority = 1,
                            title = "Curtail Non-Essential Spending",
                            description = "Expenses exceed income this month. Prioritize fixed necessities to rebalance cash flow."
                        )
                    )
                }
            }
        }
        val savingsPillar = FinancialHealthPillar("Savings Discipline", savingsScore, 20, savingsSummary, savingsScore >= 14)

        // 2. Budget Adherence (0 - 20)
        var budgetScore = 12
        var budgetSummary = "Budget in moderate range."
        if (budget != null && budget.limit.isPositive()) {
            val ratio = totalSpent.amount.divide(budget.limit.amount, 2, RoundingMode.HALF_EVEN)
            when {
                ratio <= BigDecimal("0.80") -> {
                    budgetScore = 20
                    budgetSummary = "Well within budget limit (${ratio.multiply(BigDecimal(100)).toInt()}% utilized)."
                }
                ratio <= BigDecimal("1.00") -> {
                    budgetScore = 16
                    budgetSummary = "Near budget ceiling (${ratio.multiply(BigDecimal(100)).toInt()}% utilized)."
                }
                ratio <= BigDecimal("1.15") -> {
                    budgetScore = 8
                    budgetSummary = "Over budget by ${(ratio.subtract(BigDecimal.ONE)).multiply(BigDecimal(100)).toInt()}%."
                    tips.add(
                        ActionableTip(
                            id = UUID.randomUUID().toString(),
                            priority = 1,
                            title = "Realign Active Budgets",
                            description = "Monthly spending has exceeded the configured budget threshold. Consider adjusting category limits."
                        )
                    )
                }
                else -> {
                    budgetScore = 2
                    budgetSummary = "Significantly exceeded monthly budget limit."
                }
            }
        }
        val budgetPillar = FinancialHealthPillar("Budget Adherence", budgetScore, 20, budgetSummary, budgetScore >= 14)

        // 3. Spending Stability (0 - 20)
        var stabilityScore = 15
        var stabilitySummary = "Consistent day-to-day spending pattern."
        if (expenses.size >= 5) {
            val averageExpense = totalSpent.amount.divide(BigDecimal(expenses.size), 2, RoundingMode.HALF_EVEN)
            val outlierCount = expenses.count { it.money.amount > averageExpense.multiply(BigDecimal("3.0")) }
            if (outlierCount >= 3) {
                stabilityScore = 8
                stabilitySummary = "Multiple high-value spending spikes detected this month."
                tips.add(
                    ActionableTip(
                        id = UUID.randomUUID().toString(),
                        priority = 3,
                        title = "Smooth Out Large Purchases",
                        description = "Distribute major discretionary buys across billing cycles to maintain spending stability."
                    )
                )
            } else if (outlierCount == 0) {
                stabilityScore = 19
                stabilitySummary = "High transaction predictability with zero irregular volatility."
            }
        }
        val stabilityPillar = FinancialHealthPillar("Spending Stability", stabilityScore, 20, stabilitySummary, stabilityScore >= 14)

        // 4. Cash Cushion (0 - 20)
        var totalBalance = BigDecimal.ZERO
        for (acc in accounts) {
            totalBalance = totalBalance.add(acc.initialBalance.amount)
        }
        var cushionScore = 12
        var cushionSummary = "Adequate liquid accounts buffer."
        if (totalSpent.isPositive()) {
            val monthsOfRunway = totalBalance.divide(totalSpent.amount, 1, RoundingMode.HALF_EVEN)
            when {
                monthsOfRunway >= BigDecimal("3.0") -> {
                    cushionScore = 20
                    cushionSummary = "Robust emergency cushion ($monthsOfRunway months of living expenses in reserve)."
                }
                monthsOfRunway >= BigDecimal("1.0") -> {
                    cushionScore = 15
                    cushionSummary = "Sufficient 1-month liquid liquidity buffer."
                }
                else -> {
                    cushionScore = 7
                    cushionSummary = "Low liquidity reserve relative to current monthly spending."
                    tips.add(
                        ActionableTip(
                            id = UUID.randomUUID().toString(),
                            priority = 2,
                            title = "Build 3-Month Emergency Fund",
                            description = "Aim to accumulate at least 3 months of baseline expenses in liquid savings accounts."
                        )
                    )
                }
            }
        }
        val cushionPillar = FinancialHealthPillar("Cash Cushion", cushionScore, 20, cushionSummary, cushionScore >= 14)

        // 5. Leak Control (0 - 20)
        var leakScore = 18
        var leakSummary = "Low recurring friction and controlled micro-spending."
        val microExpenses = expenses.count { it.money.amount <= BigDecimal("250.00") }
        if (microExpenses > 25) {
            leakScore = 10
            leakSummary = "$microExpenses micro-transactions detected. Small repeated purchases are accumulating."
            tips.add(
                ActionableTip(
                    id = UUID.randomUUID().toString(),
                    priority = 3,
                    title = "Audit Micro-Transactions",
                    description = "Frequent small expenses (< ₹250) are compounding. Check the Leak Hunter tab for itemized details."
                )
            )
        } else if (recurring.size >= 8) {
            leakScore = 12
            leakSummary = "${recurring.size} active recurring commitments found."
        }
        val leakPillar = FinancialHealthPillar("Leak Control", leakScore, 20, leakSummary, leakScore >= 14)

        val totalScore = (savingsScore + budgetScore + stabilityScore + cushionScore + leakScore).coerceIn(0, 100)
        val tier = when {
            totalScore >= 85 -> "Excellent"
            totalScore >= 70 -> "Strong"
            totalScore >= 50 -> "Fair"
            else -> "Attention Needed"
        }

        return FinancialHealthScore(
            overallScore = totalScore,
            tier = tier,
            savingsDiscipline = savingsPillar,
            budgetAdherence = budgetPillar,
            spendingStability = stabilityPillar,
            cashCushion = cushionPillar,
            leakControl = leakPillar,
            actionableTips = tips.take(3)
        )
    }
}

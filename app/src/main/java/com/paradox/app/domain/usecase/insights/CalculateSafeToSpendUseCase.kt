package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.intelligence.SafeToSpendResult
import com.paradox.app.domain.model.intelligence.SafeToSpendStatus
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
import javax.inject.Inject

class CalculateSafeToSpendUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(profileId: String, today: LocalDate = LocalDate.now()): SafeToSpendResult {
        val yearMonth = YearMonth.from(today)
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()
        val daysRemaining = maxOf(1, endOfMonth.dayOfMonth - today.dayOfMonth + 1)
        val daysElapsed = maxOf(1, today.dayOfMonth)

        // 1. Get Monthly Budget or fallback to recorded income
        val monthlyBudgetObj = budgetRepository.getOverallBudget(profileId, BudgetType.MONTHLY).firstOrNull()
        val totalBudgetMoney = if (monthlyBudgetObj != null && monthlyBudgetObj.limit.isPositive()) {
            monthlyBudgetObj.limit
        } else {
            val incomeMoney = incomeRepository.observeTotalIncomeInRange(profileId, startOfMonth, endOfMonth).first()
            if (incomeMoney.isPositive()) incomeMoney else Money.of(BigDecimal("50000.00"), "INR")
        }

        // 2. Total spent so far
        val totalSpent = expenseRepository.observeTotalSpentInRange(profileId, startOfMonth, today).first()
        val remainingBudget = if (totalBudgetMoney.amount > totalSpent.amount) {
            totalBudgetMoney - totalSpent
        } else {
            Money.zero(totalBudgetMoney.currencyCode)
        }

        // 3. Upcoming recurring expenses
        val activeRecurring = recurringRepository.getActiveRecurring(profileId).first()
        var upcomingRecurringTotal = BigDecimal.ZERO
        for (rec in activeRecurring) {
            val nextDate = rec.nextDueDate
            if (nextDate in today..endOfMonth) {
                upcomingRecurringTotal = upcomingRecurringTotal.add(rec.amount.amount)
            }
        }
        val upcomingCommitments = Money.of(upcomingRecurringTotal, totalBudgetMoney.currencyCode)

        // 4. Savings commitments (monthly goal targets)
        val goals = savingsGoalRepository.getAllGoals(profileId).first()
        var monthlySavingsTarget = BigDecimal.ZERO
        for (goal in goals) {
            val remainingToGoal = goal.targetAmount.amount.subtract(goal.currentAmount.amount)
            if (remainingToGoal > BigDecimal.ZERO) {
                val monthsUntilTarget = maxOf(1, java.time.Period.between(today, goal.targetDate).toTotalMonths().toInt())
                val monthlyShare = remainingToGoal.divide(BigDecimal(monthsUntilTarget), 2, RoundingMode.HALF_EVEN)
                monthlySavingsTarget = monthlySavingsTarget.add(monthlyShare)
            }
        }
        val savingsCommitments = Money.of(monthlySavingsTarget, totalBudgetMoney.currencyCode)

        // 5. Net available buffer
        val commitmentsDeduction = upcomingCommitments.amount.add(savingsCommitments.amount)
        val netAvailableAmount = remainingBudget.amount.subtract(commitmentsDeduction)
        val dailySafeAmountRaw = if (netAvailableAmount > BigDecimal.ZERO) {
            netAvailableAmount.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_EVEN)
        } else {
            BigDecimal.ZERO
        }
        val dailySafeAmount = Money.of(dailySafeAmountRaw, totalBudgetMoney.currencyCode)

        // 6. Burn rate per day so far
        val burnRateRaw = totalSpent.amount.divide(BigDecimal(daysElapsed), 2, RoundingMode.HALF_EVEN)
        val burnRatePerDay = Money.of(burnRateRaw, totalBudgetMoney.currencyCode)

        // 7. Determine status
        val status: SafeToSpendStatus
        val reason: String
        val budgetConsumptionRatio = if (totalBudgetMoney.isPositive()) {
            totalSpent.amount.divide(totalBudgetMoney.amount, 2, RoundingMode.HALF_EVEN)
        } else BigDecimal.ONE

        if (dailySafeAmount.isZero() || remainingBudget.isZero()) {
            status = SafeToSpendStatus.DANGER
            reason = "Monthly spending limit reached. Daily safe allowance is depleted."
        } else if (budgetConsumptionRatio >= BigDecimal("0.85")) {
            status = SafeToSpendStatus.CAUTION
            reason = "Over 85% of budget consumed with $daysRemaining days left."
        } else if (dailySafeAmountRaw < burnRateRaw.multiply(BigDecimal("0.65"))) {
            status = SafeToSpendStatus.MODERATE
            reason = "Current spending pace exceeds available safe daily allowance."
        } else {
            status = SafeToSpendStatus.HEALTHY
            reason = "On track. You can comfortably spend up to ${dailySafeAmount.amount} per day."
        }

        return SafeToSpendResult(
            dailySafeAmount = dailySafeAmount,
            remainingBudget = remainingBudget,
            totalMonthlyBudget = totalBudgetMoney,
            daysRemaining = daysRemaining,
            upcomingCommitments = upcomingCommitments,
            savingsCommitments = savingsCommitments,
            burnRatePerDay = burnRatePerDay,
            status = status,
            statusReason = reason
        )
    }
}

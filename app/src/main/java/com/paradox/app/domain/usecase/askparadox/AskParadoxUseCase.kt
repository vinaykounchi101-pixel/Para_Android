package com.paradox.app.domain.usecase.askparadox

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.intelligence.GroundedChatMessage
import com.paradox.app.domain.model.intelligence.GroundedSourceRef
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import com.paradox.app.domain.usecase.insights.CalculateFinancialHealthScoreUseCase
import com.paradox.app.domain.usecase.insights.CalculateSafeToSpendUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

class AskParadoxUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val accountRepository: AccountRepository,
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    private val calculateHealthScoreUseCase: CalculateFinancialHealthScoreUseCase
) {
    suspend operator fun invoke(profileId: String, query: String): GroundedChatMessage {
        val cleaned = query.trim().lowercase()
        val today = LocalDate.now()
        val yearMonth = YearMonth.from(today)
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()

        val sources = mutableListOf<GroundedSourceRef>()
        val answerBuilder = StringBuilder()

        when {
            // 1. Safe-to-Spend queries
            cleaned.contains("safe to spend") || cleaned.contains("daily allowance") || cleaned.contains("safely spend") -> {
                val safeResult = calculateSafeToSpendUseCase(profileId, today)
                answerBuilder.append("Your Safe-to-Spend limit for today is **₹${safeResult.dailySafeAmount.amount}**.\n\n")
                answerBuilder.append("• **Remaining Monthly Budget**: ₹${safeResult.remainingBudget.amount}\n")
                answerBuilder.append("• **Days Remaining**: ${safeResult.daysRemaining} days\n")
                answerBuilder.append("• **Upcoming Commitments**: ₹${safeResult.upcomingCommitments.amount}\n")
                answerBuilder.append("• **Status**: ${safeResult.status} (${safeResult.statusReason})")

                sources.add(
                    GroundedSourceRef(
                        type = "CALCULATION",
                        title = "Safe-to-Spend Engine",
                        amount = safeResult.dailySafeAmount,
                        date = today
                    )
                )
            }

            // 2. Health Score queries
            cleaned.contains("health") || cleaned.contains("score") || cleaned.contains("how am i doing") -> {
                val health = calculateHealthScoreUseCase(profileId, today)
                answerBuilder.append("Your Financial Health Score is **${health.overallScore}/100** (*${health.tier}*).\n\n")
                answerBuilder.append("• **Savings Discipline**: ${health.savingsDiscipline.score}/20 — ${health.savingsDiscipline.summary}\n")
                answerBuilder.append("• **Budget Adherence**: ${health.budgetAdherence.score}/20 — ${health.budgetAdherence.summary}\n")
                answerBuilder.append("• **Spending Stability**: ${health.spendingStability.score}/20 — ${health.spendingStability.summary}\n")
                answerBuilder.append("• **Cash Cushion**: ${health.cashCushion.score}/20 — ${health.cashCushion.summary}\n")
                answerBuilder.append("• **Leak Control**: ${health.leakControl.score}/20 — ${health.leakControl.summary}\n\n")
                if (health.actionableTips.isNotEmpty()) {
                    answerBuilder.append("**Top Recommendation**:\n")
                    answerBuilder.append("👉 ${health.actionableTips.first().title}: ${health.actionableTips.first().description}")
                }

                sources.add(
                    GroundedSourceRef(
                        type = "CALCULATION",
                        title = "5-Pillar Health Score",
                        amount = Money.of(BigDecimal.valueOf(health.overallScore.toLong()), "INR")
                    )
                )
            }

            // 3. Subscriptions / Recurring bills
            cleaned.contains("subscription") || cleaned.contains("recurring") || cleaned.contains("bills") -> {
                val recurringList = recurringRepository.getActiveRecurring(profileId).first()
                if (recurringList.isEmpty()) {
                    answerBuilder.append("You currently have no active recurring subscriptions or scheduled bills recorded.")
                } else {
                    var total = BigDecimal.ZERO
                    answerBuilder.append("You have **${recurringList.size} active recurring commitments**:\n\n")
                    for (rec in recurringList) {
                        total = total.add(rec.amount.amount)
                        answerBuilder.append("• **${rec.title}**: ₹${rec.amount.amount} (${rec.frequency.name.lowercase()}, due on ${rec.nextDueDate})\n")
                        sources.add(
                            GroundedSourceRef(
                                type = "RECURRING",
                                title = rec.title,
                                amount = rec.amount,
                                date = rec.nextDueDate
                            )
                        )
                    }
                    answerBuilder.append("\n**Total Monthly Commitment**: ₹$total")
                }
            }

            // 4. Savings Goals
            cleaned.contains("goal") || cleaned.contains("saving") -> {
                val goals = savingsGoalRepository.getAllGoals(profileId).first()
                if (goals.isEmpty()) {
                    answerBuilder.append("You don't have any savings goals set up yet. Create one from the Savings tab!")
                } else {
                    answerBuilder.append("Here is the status of your **${goals.size} savings goals**:\n\n")
                    for (goal in goals) {
                        val pct = if (goal.targetAmount.isPositive()) {
                            goal.currentAmount.amount.divide(goal.targetAmount.amount, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal(100)).toInt()
                        } else 0
                        answerBuilder.append("• **${goal.name}**: ₹${goal.currentAmount.amount} of ₹${goal.targetAmount.amount} ($pct% saved, target: ${goal.targetDate})\n")
                        sources.add(
                            GroundedSourceRef(
                                type = "SAVINGS_GOAL",
                                title = goal.name,
                                amount = goal.currentAmount,
                                date = goal.targetDate
                            )
                        )
                    }
                }
            }

            // 5. Biggest expense
            cleaned.contains("biggest") || cleaned.contains("highest") || cleaned.contains("largest") -> {
                val expenses = expenseRepository.getExpensesInRange(profileId, startOfMonth, today).first()
                val biggest = expenses.maxByOrNull { it.money.amount }
                if (biggest != null) {
                    answerBuilder.append("Your biggest recorded expense this month is **${biggest.title}** for **₹${biggest.money.amount}** on ${biggest.date}.")
                    sources.add(
                        GroundedSourceRef(
                            type = "EXPENSE",
                            title = biggest.title,
                            amount = biggest.money,
                            date = biggest.date
                        )
                    )
                } else {
                    answerBuilder.append("No expenses recorded yet for this month.")
                }
            }

            // 6. Specific Category Query (e.g. food, transport, shopping)
            else -> {
                val categories = categoryRepository.getCategories(profileId).first()
                val matchedCategory = categories.find { cleaned.contains(it.name.lowercase()) }

                if (matchedCategory != null) {
                    val categorySpend = expenseRepository.observeCategoryTotalSpentInRange(
                        profileId, matchedCategory.id, startOfMonth, today
                    ).first()
                    answerBuilder.append("You have spent **₹${categorySpend.amount}** on **${matchedCategory.name}** this month.")
                    sources.add(
                        GroundedSourceRef(
                            type = "EXPENSE",
                            title = matchedCategory.name,
                            amount = categorySpend
                        )
                    )
                } else {
                    // General monthly summary fallback
                    val totalSpent = expenseRepository.observeTotalSpentInRange(profileId, startOfMonth, today).first()
                    val totalIncome = incomeRepository.observeTotalIncomeInRange(profileId, startOfMonth, endOfMonth).first()
                    val budget = budgetRepository.getOverallBudget(profileId, BudgetType.MONTHLY).firstOrNull()

                    answerBuilder.append("### Monthly Financial Overview (${yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${yearMonth.year})\n\n")
                    answerBuilder.append("• **Total Spending**: ₹${totalSpent.amount}\n")
                    answerBuilder.append("• **Total Income**: ₹${totalIncome.amount}\n")
                    if (budget != null) {
                        answerBuilder.append("• **Monthly Budget**: ₹${budget.limit.amount}\n")
                        val remaining = if (budget.limit.amount > totalSpent.amount) budget.limit - totalSpent else Money.zero()
                        answerBuilder.append("• **Remaining Budget**: ₹${remaining.amount}\n")
                    }
                    val net = if (totalIncome.isPositive()) totalIncome.amount.subtract(totalSpent.amount) else BigDecimal.ZERO
                    answerBuilder.append("• **Net Cash Flow**: ₹$net\n\n")
                    answerBuilder.append("Ask me anything specific like *'How much did I spend on Food?'*, *'What is my safe to spend today?'*, or *'Show my subscriptions'*.")

                    sources.add(
                        GroundedSourceRef(
                            type = "EXPENSE",
                            title = "Monthly Spent Total",
                            amount = totalSpent
                        )
                    )
                }
            }
        }

        return GroundedChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            message = answerBuilder.toString(),
            timestamp = Instant.now(),
            groundedSources = sources
        )
    }
}

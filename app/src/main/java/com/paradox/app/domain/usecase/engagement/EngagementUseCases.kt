package com.paradox.app.domain.usecase.engagement

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.engagement.FinancialVibe
import com.paradox.app.domain.model.engagement.LoggingStreak
import com.paradox.app.domain.model.engagement.MilestoneBadge
import com.paradox.app.domain.model.engagement.MonthlyDigest
import com.paradox.app.domain.model.engagement.SplitExpenseResult
import com.paradox.app.domain.model.engagement.SplitPersonShare
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class GenerateMonthlyDigestUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    private val evaluateFinancialVibeUseCase: EvaluateFinancialVibeUseCase
) {
    suspend operator fun invoke(profileId: String, yearMonth: YearMonth = YearMonth.now()): MonthlyDigest {
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()

        val totalIncome = incomeRepository.observeTotalIncomeInRange(profileId, startOfMonth, endOfMonth).first()
        val totalSpent = expenseRepository.observeTotalSpentInRange(profileId, startOfMonth, endOfMonth).first()
        val expenses = expenseRepository.getExpensesInRange(profileId, startOfMonth, endOfMonth).first()
        val categoriesList = categoryRepository.getCategories(profileId).first()
        val categories: Map<String, Category> = categoriesList.associateBy { it.id }

        val netSaved = if (totalIncome.isPositive() && totalIncome.amount > totalSpent.amount) {
            totalIncome.amount.subtract(totalSpent.amount)
        } else BigDecimal.ZERO

        val savingsRate = if (totalIncome.isPositive() && netSaved > BigDecimal.ZERO) {
            netSaved.divide(totalIncome.amount, 2, RoundingMode.HALF_EVEN).multiply(BigDecimal(100))
        } else BigDecimal.ZERO

        // Top category
        val categorySpends = expenses.groupBy { it.categoryId }
            .mapValues { entry -> entry.value.fold(BigDecimal.ZERO) { acc, exp -> acc.add(exp.money.amount) } }
        val topCategoryEntry = categorySpends.maxByOrNull { it.value }
        val topCategoryName = topCategoryEntry?.let { categories[it.key]?.name } ?: "General"
        val topCategoryAmount = Money.of(topCategoryEntry?.value ?: BigDecimal.ZERO, totalSpent.currencyCode)

        // Biggest expense
        val biggestExp = expenses.maxByOrNull { it.money.amount }
        val biggestTitle = biggestExp?.title ?: "No expenses"
        val biggestAmount = biggestExp?.money ?: Money.zero(totalSpent.currencyCode)

        val monthLabel = "${yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${yearMonth.year}"
        val vibe = evaluateFinancialVibeUseCase(profileId, yearMonth)

        val highlightSentence = if (netSaved > BigDecimal.ZERO) {
            "You saved ₹$netSaved this month, achieving a $savingsRate% savings rate."
        } else {
            "You logged ${expenses.size} expenses totaling ₹${totalSpent.amount} this month."
        }

        val positiveEncouragement = if (netSaved > BigDecimal.ZERO) {
            "Every rupee saved strengthens your financial independence. Keep building momentum!"
        } else {
            "Tracking your money is the highest-leverage first step. You're in full control of your financial clarity."
        }

        return MonthlyDigest(
            monthYearLabel = monthLabel,
            totalEarned = totalIncome,
            totalSpent = totalSpent,
            netSaved = Money.of(netSaved, totalSpent.currencyCode),
            savingsRatePct = savingsRate,
            topCategory = topCategoryName,
            topCategoryAmount = topCategoryAmount,
            biggestExpenseTitle = biggestTitle,
            biggestExpenseAmount = biggestAmount,
            highlightSentence = highlightSentence,
            positiveEncouragement = positiveEncouragement,
            vibe = vibe
        )
    }
}

class TrackLoggingStreakUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(profileId: String, today: LocalDate = LocalDate.now()): LoggingStreak {
        val allExpenses = expenseRepository.getAllExpenses(profileId).first()
        if (allExpenses.isEmpty()) {
            return LoggingStreak(
                currentStreakDays = 0,
                longestStreakDays = 0,
                totalExpensesLogged = 0,
                badges = getInitialBadges(0, 0)
            )
        }

        val uniqueDates = allExpenses.map { it.date }.toSortedSet(Comparator.reverseOrder())

        var currentStreak = 0
        var checkDate = today
        if (!uniqueDates.contains(today) && uniqueDates.contains(today.minusDays(1))) {
            checkDate = today.minusDays(1)
        }

        while (uniqueDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        val badges = getInitialBadges(currentStreak, allExpenses.size)

        return LoggingStreak(
            currentStreakDays = currentStreak,
            longestStreakDays = maxOf(currentStreak, 7),
            totalExpensesLogged = allExpenses.size,
            badges = badges
        )
    }

    private fun getInitialBadges(currentStreak: Int, totalLogged: Int): List<MilestoneBadge> {
        return listOf(
            MilestoneBadge(
                id = "first_expense",
                title = "Clarity Pioneer",
                description = "Logged your first transaction in Paradox.",
                icon = "flag",
                isUnlocked = totalLogged >= 1
            ),
            MilestoneBadge(
                id = "streak_3",
                title = "3-Day Steady",
                description = "Logged money activity for 3 consecutive days.",
                icon = "bolt",
                isUnlocked = currentStreak >= 3
            ),
            MilestoneBadge(
                id = "streak_7",
                title = "Week Champion",
                description = "Maintained a 7-day continuous logging habit.",
                icon = "military_tech",
                isUnlocked = currentStreak >= 7
            ),
            MilestoneBadge(
                id = "century_club",
                title = "Century Club",
                description = "Recorded over 100 accurate financial transactions.",
                icon = "workspace_premium",
                isUnlocked = totalLogged >= 100
            )
        )
    }
}

class CalculateSplitExpenseUseCase @Inject constructor() {
    operator fun invoke(
        totalAmount: Money,
        peopleCount: Int,
        tipAmount: Money = Money.zero(totalAmount.currencyCode),
        customNames: List<String> = emptyList()
    ): SplitExpenseResult {
        require(peopleCount >= 1) { "People count must be at least 1" }

        val grandTotal = totalAmount + tipAmount
        val peopleCountBd = BigDecimal(peopleCount)
        val perPersonRaw = grandTotal.amount.divide(peopleCountBd, 2, RoundingMode.DOWN)
        val perPersonMoney = Money.of(perPersonRaw, totalAmount.currencyCode)

        val totalAllocated = perPersonRaw.multiply(peopleCountBd)
        val remainder = grandTotal.amount.subtract(totalAllocated)

        val shares = mutableListOf<SplitPersonShare>()
        for (i in 0 until peopleCount) {
            val name = customNames.getOrNull(i) ?: "Person ${i + 1}"
            // Add remaining cent/paisa to first person
            val shareAmount = if (i == 0 && remainder > BigDecimal.ZERO) {
                perPersonRaw.add(remainder)
            } else {
                perPersonRaw
            }
            shares.add(SplitPersonShare(personName = name, amount = Money.of(shareAmount, totalAmount.currencyCode)))
        }

        return SplitExpenseResult(
            totalAmount = totalAmount,
            peopleCount = peopleCount,
            tipAmount = tipAmount,
            grandTotal = grandTotal,
            perPersonAmount = perPersonMoney,
            remainderAmount = Money.of(remainder, totalAmount.currencyCode),
            shares = shares
        )
    }
}

class EvaluateFinancialVibeUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(profileId: String, yearMonth: YearMonth = YearMonth.now()): FinancialVibe {
        val startOfMonth = yearMonth.atDay(1)
        val endOfMonth = yearMonth.atEndOfMonth()

        val totalIncome = incomeRepository.observeTotalIncomeInRange(profileId, startOfMonth, endOfMonth).first()
        val totalSpent = expenseRepository.observeTotalSpentInRange(profileId, startOfMonth, endOfMonth).first()

        return when {
            totalIncome.isPositive() && totalIncome.amount > totalSpent.amount.multiply(BigDecimal("1.30")) -> {
                FinancialVibe(
                    vibeTitle = "Steady Wealth Builder",
                    vibeEmoji = "🌱",
                    tagline = "Consistent, disciplined, and flourishing.",
                    summary = "Your cash cushion is expanding comfortably. You are in total control of discretionary impulses."
                )
            }
            totalIncome.isPositive() && totalIncome.amount >= totalSpent.amount -> {
                FinancialVibe(
                    vibeTitle = "Mindful Navigator",
                    vibeEmoji = "🧭",
                    tagline = "Balanced cash flow with intentional choices.",
                    summary = "You are balancing life enjoyments with healthy budgeting boundaries."
                )
            }
            totalSpent.isPositive() -> {
                FinancialVibe(
                    vibeTitle = "Conscious Explorer",
                    vibeEmoji = "⚡",
                    tagline = "High-energy month with active investments in experiences.",
                    summary = "Spending was active this month. Checking your Safe-to-Spend will help ease the pace into next month."
                )
            }
            else -> {
                FinancialVibe(
                    vibeTitle = "Clean Slate",
                    vibeEmoji = "✨",
                    tagline = "Fresh start ready for mindful tracking.",
                    summary = "Log your daily income and expenses to unlock personalized financial intelligence."
                )
            }
        }
    }
}

package com.paradox.app.domain.model.engagement

import com.paradox.app.core.money.Money
import java.math.BigDecimal

data class MilestoneBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean
)

data class LoggingStreak(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalExpensesLogged: Int,
    val badges: List<MilestoneBadge>
)

data class SplitPersonShare(
    val personName: String,
    val amount: Money,
    val isPaid: Boolean = false
)

data class SplitExpenseResult(
    val totalAmount: Money,
    val peopleCount: Int,
    val tipAmount: Money,
    val grandTotal: Money,
    val perPersonAmount: Money,
    val remainderAmount: Money,
    val shares: List<SplitPersonShare>
)

data class FinancialVibe(
    val vibeTitle: String,
    val vibeEmoji: String,
    val tagline: String,
    val summary: String
)

data class MonthlyDigest(
    val monthYearLabel: String,
    val totalEarned: Money,
    val totalSpent: Money,
    val netSaved: Money,
    val savingsRatePct: BigDecimal,
    val topCategory: String,
    val topCategoryAmount: Money,
    val biggestExpenseTitle: String,
    val biggestExpenseAmount: Money,
    val highlightSentence: String,
    val positiveEncouragement: String,
    val vibe: FinancialVibe
)

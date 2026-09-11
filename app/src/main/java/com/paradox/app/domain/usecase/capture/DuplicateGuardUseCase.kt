package com.paradox.app.domain.usecase.capture

import com.paradox.app.core.money.CurrencyFormatter
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

data class DuplicateWarning(
    val isLikelyDuplicate: Boolean,
    val matchedExpense: Expense? = null,
    val reason: String? = null
)

@Singleton
class DuplicateGuardUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {

    suspend fun checkDuplicate(
        profileId: String,
        amount: Money,
        title: String,
        date: LocalDate
    ): DuplicateWarning {
        val startDate = date.minusDays(3)
        val endDate = date.plusDays(3)

        val recentExpenses = expenseRepository.getExpensesInRange(profileId, startDate, endDate)
            .firstOrNull() ?: emptyList()

        for (existing in recentExpenses) {
            val sameAmount = existing.money.amount.compareTo(amount.amount) == 0 &&
                    existing.money.currencyCode == amount.currencyCode

            if (sameAmount) {
                val titleSimilarity = calculateSimilarity(title.lowercase().trim(), existing.title.lowercase().trim())
                val dayDiff = abs(ChronoUnit.DAYS.between(date, existing.date))

                if (titleSimilarity > 0.6 || title.trim().equals(existing.title.trim(), ignoreCase = true)) {
                    val dateNote = if (dayDiff == 0L) "today" else "$dayDiff days apart"
                    val formattedAmount = CurrencyFormatter.format(existing.money)
                    return DuplicateWarning(
                        isLikelyDuplicate = true,
                        matchedExpense = existing,
                        reason = "Possible duplicate: \"${existing.title}\" ($formattedAmount) recorded $dateNote"
                    )
                }
            }
        }

        return DuplicateWarning(isLikelyDuplicate = false)
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        if (s1.contains(s2) || s2.contains(s1)) return 0.85

        val pairs1 = s1.windowed(2).toSet()
        val pairs2 = s2.windowed(2).toSet()
        val intersection = pairs1.intersect(pairs2).size
        val union = pairs1.union(pairs2).size

        return if (union == 0) 0.0 else (2.0 * intersection) / (pairs1.size + pairs2.size)
    }
}

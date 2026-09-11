package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.intelligence.LeakType
import com.paradox.app.domain.model.intelligence.SpendingLeak
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

class DetectSpendingLeaksUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val recurringRepository: RecurringExpenseRepository
) {
    suspend operator fun invoke(profileId: String, today: LocalDate = LocalDate.now()): List<SpendingLeak> {
        val yearMonth = YearMonth.from(today)
        val startOfMonth = yearMonth.atDay(1)
        val expenses = expenseRepository.getExpensesInRange(profileId, startOfMonth, today).first()
        val recurring = recurringRepository.getActiveRecurring(profileId).first()

        val leaks = mutableListOf<SpendingLeak>()

        // 1. Active Subscriptions & Recurring Commitments
        if (recurring.isNotEmpty()) {
            var recurringTotal = BigDecimal.ZERO
            for (rec in recurring) {
                recurringTotal = recurringTotal.add(rec.amount.amount)
            }
            leaks.add(
                SpendingLeak(
                    id = UUID.randomUUID().toString(),
                    title = "Active Subscriptions & Recurring Bills",
                    type = LeakType.SUBSCRIPTION,
                    totalImpact = Money.of(recurringTotal, "INR"),
                    occurrences = recurring.size,
                    description = "${recurring.size} recurring subscriptions currently active totaling ₹$recurringTotal / month.",
                    recommendation = "Review and cancel unused streaming services or software memberships."
                )
            )
        }

        // 2. Micro-Spending Accumulation (<= ₹250 / $5)
        val microTransactions = expenses.filter { it.money.amount <= BigDecimal("250.00") }
        if (microTransactions.size >= 5) {
            var microTotal = BigDecimal.ZERO
            for (m in microTransactions) {
                microTotal = microTotal.add(m.money.amount)
            }
            leaks.add(
                SpendingLeak(
                    id = UUID.randomUUID().toString(),
                    title = "Micro-Transactions Accumulation",
                    type = LeakType.MICRO_SPENDING,
                    totalImpact = Money.of(microTotal, "INR"),
                    occurrences = microTransactions.size,
                    description = "${microTransactions.size} purchases under ₹250 recorded this month, quietly adding up to ₹$microTotal.",
                    recommendation = "Bundle daily discretionary snacks and small convenience purchases into weekly budgets."
                )
            )
        }

        // 3. High Frequency Repeat Merchants
        val merchantGroups = expenses.groupBy { it.title.trim().lowercase() }
        for ((merchant, txList) in merchantGroups) {
            if (txList.size >= 4 && merchant.isNotBlank()) {
                var merchantTotal = BigDecimal.ZERO
                for (tx in txList) {
                    merchantTotal = merchantTotal.add(tx.money.amount)
                }
                val formattedName = merchant.replaceFirstChar { it.uppercase() }
                leaks.add(
                    SpendingLeak(
                        id = UUID.randomUUID().toString(),
                        title = "High Velocity Merchant: $formattedName",
                        type = LeakType.HIGH_FREQUENCY_MERCHANT,
                        totalImpact = Money.of(merchantTotal, "INR"),
                        occurrences = txList.size,
                        description = "You visited or ordered from $formattedName ${txList.size} times this month for ₹$merchantTotal.",
                        recommendation = "Set a dedicated weekly spending cap for orders with $formattedName."
                    )
                )
            }
        }

        return leaks.sortedByDescending { it.totalImpact.amount }
    }
}

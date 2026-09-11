package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.model.intelligence.AffordabilityRating
import com.paradox.app.domain.model.intelligence.LeakType
import com.paradox.app.domain.model.intelligence.SafeToSpendResult
import com.paradox.app.domain.model.intelligence.SafeToSpendStatus
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class LeakHunterAndSimulatorTest {

    @Test
    fun `detectSpendingLeaks detects micro transactions and subscriptions`() = runTest {
        val expenseRepo: ExpenseRepository = mockk(relaxed = true)
        val recurringRepo: RecurringExpenseRepository = mockk(relaxed = true)
        val testDate = LocalDate.of(2026, 9, 15)

        val expenses = (1..6).map {
            Expense(
                id = "e$it",
                profileId = "prof_1",
                title = "Tea stall",
                money = Money(BigDecimal("30.00"), "INR"),
                categoryId = "c1",
                paymentMethodId = "pm1",
                date = LocalDate.of(2026, 9, it)
            )
        }

        val recurring = listOf(
            RecurringExpense(
                id = "r1",
                profileId = "prof_1",
                title = "Netflix",
                amount = Money(BigDecimal("649.00"), "INR"),
                frequency = RecurringFrequency.MONTHLY,
                categoryId = "c1",
                paymentMethodId = "pm1",
                startDate = LocalDate.of(2026, 9, 1),
                nextDueDate = LocalDate.of(2026, 9, 28)
            )
        )

        every { expenseRepo.getExpensesInRange("prof_1", LocalDate.of(2026, 9, 1), testDate) } returns flowOf(expenses)
        every { recurringRepo.getActiveRecurring("prof_1") } returns flowOf(recurring)

        val useCase = DetectSpendingLeaksUseCase(expenseRepo, recurringRepo)
        val leaks = useCase("prof_1", testDate)

        assertEquals(3, leaks.size)
        assertTrue(leaks.any { it.type == LeakType.SUBSCRIPTION })
        assertTrue(leaks.any { it.type == LeakType.MICRO_SPENDING })
        assertTrue(leaks.any { it.type == LeakType.HIGH_FREQUENCY_MERCHANT })
    }

    @Test
    fun `simulatePurchase flags DELAY_PURCHASE when amount exceeds remaining budget`() = runTest {
        val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase = mockk()

        coEvery { calculateSafeToSpendUseCase("prof_1", any()) } returns SafeToSpendResult(
            dailySafeAmount = Money(BigDecimal("1000.00"), "INR"),
            remainingBudget = Money(BigDecimal("10000.00"), "INR"),
            totalMonthlyBudget = Money(BigDecimal("50000.00"), "INR"),
            daysRemaining = 10,
            upcomingCommitments = Money.zero(),
            savingsCommitments = Money.zero(),
            burnRatePerDay = Money(BigDecimal("1500.00"), "INR"),
            status = SafeToSpendStatus.HEALTHY,
            statusReason = "On track"
        )

        val simulator = SimulatePurchaseUseCase(calculateSafeToSpendUseCase)
        val result = simulator("prof_1", Money(BigDecimal("15000.00"), "INR"))

        assertEquals(AffordabilityRating.DELAY_PURCHASE, result.rating)
    }

    @Test
    fun `simulatePurchase approves SAFE_TO_BUY for minor purchases with ample buffer`() = runTest {
        val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase = mockk()

        coEvery { calculateSafeToSpendUseCase("prof_1", any()) } returns SafeToSpendResult(
            dailySafeAmount = Money(BigDecimal("2500.00"), "INR"),
            remainingBudget = Money(BigDecimal("40000.00"), "INR"),
            totalMonthlyBudget = Money(BigDecimal("50000.00"), "INR"),
            daysRemaining = 15,
            upcomingCommitments = Money.zero(),
            savingsCommitments = Money.zero(),
            burnRatePerDay = Money(BigDecimal("1000.00"), "INR"),
            status = SafeToSpendStatus.HEALTHY,
            statusReason = "On track"
        )

        val simulator = SimulatePurchaseUseCase(calculateSafeToSpendUseCase)
        val result = simulator("prof_1", Money(BigDecimal("2000.00"), "INR"))

        assertEquals(AffordabilityRating.SAFE_TO_BUY, result.rating)
    }
}

package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.model.intelligence.SafeToSpendStatus
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SafeToSpendUseCaseTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val recurringRepository: RecurringExpenseRepository = mockk(relaxed = true)
    private val savingsGoalRepository: SavingsGoalRepository = mockk(relaxed = true)

    private lateinit var useCase: CalculateSafeToSpendUseCase

    @Before
    fun setUp() {
        useCase = CalculateSafeToSpendUseCase(
            budgetRepository,
            expenseRepository,
            incomeRepository,
            recurringRepository,
            savingsGoalRepository
        )
    }

    @Test
    fun `calculateSafeToSpend accurately divides remaining buffer across remaining days`() = runTest {
        val testDate = LocalDate.of(2026, 9, 11) // 20 days remaining in September (11 to 30)

        every {
            budgetRepository.getOverallBudget("prof_1", BudgetType.MONTHLY)
        } returns flowOf(
            Budget(
                id = "b1",
                profileId = "prof_1",
                type = BudgetType.MONTHLY,
                limit = Money(BigDecimal("60000.00"), "INR")
            )
        )

        every {
            expenseRepository.observeTotalSpentInRange("prof_1", LocalDate.of(2026, 9, 1), testDate)
        } returns flowOf(Money(BigDecimal("20000.00"), "INR"))

        every {
            recurringRepository.getActiveRecurring("prof_1")
        } returns flowOf(
            listOf(
                RecurringExpense(
                    id = "r1",
                    profileId = "prof_1",
                    title = "Wifi Bill",
                    amount = Money(BigDecimal("1000.00"), "INR"),
                    frequency = RecurringFrequency.MONTHLY,
                    categoryId = "c1",
                    paymentMethodId = "pm1",
                    startDate = LocalDate.of(2026, 9, 1),
                    nextDueDate = LocalDate.of(2026, 9, 20)
                )
            )
        )

        every {
            savingsGoalRepository.getAllGoals("prof_1")
        } returns flowOf(emptyList())

        val result = useCase("prof_1", testDate)

        assertEquals(20, result.daysRemaining)
        assertEquals(BigDecimal("40000.00"), result.remainingBudget.amount)
        assertEquals(BigDecimal("1000.00"), result.upcomingCommitments.amount)
        // (40000 - 1000) / 20 = 39000 / 20 = 1950.00
        assertEquals(BigDecimal("1950.00"), result.dailySafeAmount.amount)
        assertEquals(SafeToSpendStatus.HEALTHY, result.status)
    }

    @Test
    fun `calculateSafeToSpend flags DANGER when remaining budget is exhausted`() = runTest {
        val testDate = LocalDate.of(2026, 9, 25)

        every {
            budgetRepository.getOverallBudget("prof_1", BudgetType.MONTHLY)
        } returns flowOf(
            Budget(
                id = "b1",
                profileId = "prof_1",
                type = BudgetType.MONTHLY,
                limit = Money(BigDecimal("30000.00"), "INR")
            )
        )

        every {
            expenseRepository.observeTotalSpentInRange("prof_1", LocalDate.of(2026, 9, 1), testDate)
        } returns flowOf(Money(BigDecimal("35000.00"), "INR"))

        every { recurringRepository.getActiveRecurring("prof_1") } returns flowOf(emptyList())
        every { savingsGoalRepository.getAllGoals("prof_1") } returns flowOf(emptyList())

        val result = useCase("prof_1", testDate)

        assertEquals(BigDecimal("0.00"), result.remainingBudget.amount)
        assertEquals(BigDecimal("0.00"), result.dailySafeAmount.amount)
        assertEquals(SafeToSpendStatus.DANGER, result.status)
    }
}

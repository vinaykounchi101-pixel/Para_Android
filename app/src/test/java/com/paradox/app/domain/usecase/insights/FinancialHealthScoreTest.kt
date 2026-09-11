package com.paradox.app.domain.usecase.insights

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.repository.AccountRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class FinancialHealthScoreTest {

    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val recurringRepository: RecurringExpenseRepository = mockk(relaxed = true)
    private val savingsGoalRepository: SavingsGoalRepository = mockk(relaxed = true)

    private lateinit var useCase: CalculateFinancialHealthScoreUseCase

    @Before
    fun setUp() {
        useCase = CalculateFinancialHealthScoreUseCase(
            expenseRepository,
            incomeRepository,
            budgetRepository,
            accountRepository,
            recurringRepository,
            savingsGoalRepository
        )
    }

    @Test
    fun `calculateHealthScore produces high score for disciplined savings and low budget usage`() = runTest {
        val testDate = LocalDate.of(2026, 9, 15)
        val start = LocalDate.of(2026, 9, 1)
        val end = LocalDate.of(2026, 9, 30)

        every {
            incomeRepository.observeTotalIncomeInRange("prof_1", start, end)
        } returns flowOf(Money(BigDecimal("100000.00"), "INR"))

        every {
            expenseRepository.observeTotalSpentInRange("prof_1", start, end)
        } returns flowOf(Money(BigDecimal("40000.00"), "INR"))

        every {
            budgetRepository.getOverallBudget("prof_1", BudgetType.MONTHLY)
        } returns flowOf(Budget("b1", "prof_1", BudgetType.MONTHLY, Money(BigDecimal("60000.00"), "INR")))

        every {
            accountRepository.getAccounts("prof_1")
        } returns flowOf(
            listOf(
                Account(
                    id = "acc1",
                    profileId = "prof_1",
                    name = "Savings",
                    type = AccountType.SAVINGS,
                    initialBalance = Money(BigDecimal("200000.00"), "INR")
                )
            )
        )

        every { expenseRepository.getExpensesInRange("prof_1", start, testDate) } returns flowOf(emptyList())
        every { recurringRepository.getActiveRecurring("prof_1") } returns flowOf(emptyList())

        val score = useCase("prof_1", testDate)

        assertTrue(score.overallScore >= 80)
        assertEquals("Excellent", score.tier)
        assertEquals(20, score.savingsDiscipline.score)
        assertEquals(20, score.budgetAdherence.score)
    }
}

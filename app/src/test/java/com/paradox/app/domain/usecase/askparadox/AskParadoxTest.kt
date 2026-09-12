package com.paradox.app.domain.usecase.askparadox

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.model.intelligence.SafeToSpendResult
import com.paradox.app.domain.model.intelligence.SafeToSpendStatus
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import com.paradox.app.domain.usecase.insights.CalculateFinancialHealthScoreUseCase
import com.paradox.app.domain.usecase.insights.CalculateSafeToSpendUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class AskParadoxTest {

    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val recurringRepository: RecurringExpenseRepository = mockk(relaxed = true)
    private val savingsGoalRepository: SavingsGoalRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase = mockk()
    private val calculateHealthScoreUseCase: CalculateFinancialHealthScoreUseCase = mockk()
    private val geminiApiClient: com.paradox.app.core.network.GeminiApiClient = mockk(relaxed = true)
    private val financialContextBuilder: com.paradox.app.domain.usecase.intelligence.FinancialContextBuilder = mockk(relaxed = true)
    private val aiSettingsRepository: com.paradox.app.domain.repository.AiSettingsRepository = mockk(relaxed = true)

    private lateinit var askParadox: AskParadoxUseCase

    @Before
    fun setUp() {
        every { aiSettingsRepository.isAiEnabled } returns flowOf(false)
        every { aiSettingsRepository.hasApiKey() } returns false

        askParadox = AskParadoxUseCase(
            expenseRepository,
            incomeRepository,
            budgetRepository,
            categoryRepository,
            recurringRepository,
            savingsGoalRepository,
            accountRepository,
            calculateSafeToSpendUseCase,
            calculateHealthScoreUseCase,
            geminiApiClient,
            financialContextBuilder,
            aiSettingsRepository
        )
    }

    @Test
    fun `askParadox answers Safe-to-Spend queries with grounded calculation citations`() = runTest {
        coEvery { calculateSafeToSpendUseCase("prof_1", any()) } returns SafeToSpendResult(
            dailySafeAmount = Money(BigDecimal("1450.00"), "INR"),
            remainingBudget = Money(BigDecimal("29000.00"), "INR"),
            totalMonthlyBudget = Money(BigDecimal("50000.00"), "INR"),
            daysRemaining = 20,
            upcomingCommitments = Money.zero(),
            savingsCommitments = Money.zero(),
            burnRatePerDay = Money(BigDecimal("1050.00"), "INR"),
            status = SafeToSpendStatus.HEALTHY,
            statusReason = "On track"
        )

        val response = askParadox("prof_1", "How much can I safely spend today?")

        assertFalse(response.isUser)
        assertTrue(response.message.contains("1450.00"))
        assertTrue(response.groundedSources.isNotEmpty())
        assertEquals("Safe-to-Spend Engine", response.groundedSources.first().title)
    }

    @Test
    fun `askParadox answers subscription queries directly from active recurring table`() = runTest {
        every {
            recurringRepository.getActiveRecurring("prof_1")
        } returns flowOf(
            listOf(
                RecurringExpense(
                    id = "r1",
                    profileId = "prof_1",
                    title = "Spotify Premium",
                    amount = Money(BigDecimal("119.00"), "INR"),
                    frequency = RecurringFrequency.MONTHLY,
                    categoryId = "c1",
                    paymentMethodId = "pm1",
                    startDate = LocalDate.of(2026, 9, 1),
                    nextDueDate = LocalDate.of(2026, 9, 25)
                )
            )
        )

        val response = askParadox("prof_1", "Show my active subscriptions")

        assertFalse(response.isUser)
        assertTrue(response.message.contains("Spotify Premium"))
        assertTrue(response.message.contains("119.00"))
    }
}

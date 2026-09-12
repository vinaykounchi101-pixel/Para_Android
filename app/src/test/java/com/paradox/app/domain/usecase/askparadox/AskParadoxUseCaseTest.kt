package com.paradox.app.domain.usecase.askparadox

import com.paradox.app.core.money.Money
import com.paradox.app.core.network.GeminiApiClient
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.intelligence.FinancialHealthPillar
import com.paradox.app.domain.model.intelligence.FinancialHealthScore
import com.paradox.app.domain.model.intelligence.SafeToSpendResult
import com.paradox.app.domain.model.intelligence.SafeToSpendStatus
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.AiSettingsRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import com.paradox.app.domain.usecase.insights.CalculateFinancialHealthScoreUseCase
import com.paradox.app.domain.usecase.insights.CalculateSafeToSpendUseCase
import com.paradox.app.domain.usecase.intelligence.FinancialContextBuilder
import io.mockk.coEvery
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

class AskParadoxUseCaseTest {

    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val recurringRepository: RecurringExpenseRepository = mockk(relaxed = true)
    private val savingsGoalRepository: SavingsGoalRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase = mockk(relaxed = true)
    private val calculateHealthScoreUseCase: CalculateFinancialHealthScoreUseCase = mockk(relaxed = true)
    private val geminiApiClient: GeminiApiClient = mockk(relaxed = true)
    private val financialContextBuilder: FinancialContextBuilder = mockk(relaxed = true)
    private val aiSettingsRepository: AiSettingsRepository = mockk(relaxed = true)

    private lateinit var useCase: AskParadoxUseCase

    @Before
    fun setUp() {
        useCase = AskParadoxUseCase(
            expenseRepository = expenseRepository,
            incomeRepository = incomeRepository,
            budgetRepository = budgetRepository,
            categoryRepository = categoryRepository,
            recurringRepository = recurringRepository,
            savingsGoalRepository = savingsGoalRepository,
            accountRepository = accountRepository,
            calculateSafeToSpendUseCase = calculateSafeToSpendUseCase,
            calculateHealthScoreUseCase = calculateHealthScoreUseCase,
            geminiApiClient = geminiApiClient,
            financialContextBuilder = financialContextBuilder,
            aiSettingsRepository = aiSettingsRepository
        )
    }

    @Test
    fun `when AI is enabled with valid API key, Gemini response is returned`() = runTest {
        every { aiSettingsRepository.isAiEnabled } returns flowOf(true)
        coEvery { aiSettingsRepository.getApiKey() } returns "AIzaSyFakeKeyForTest12345"
        coEvery { financialContextBuilder.buildContext("p1") } returns "Context: Safe to spend ₹500/day"
        coEvery {
            geminiApiClient.generateResponse(
                prompt = any(),
                systemInstruction = any(),
                apiKey = "AIzaSyFakeKeyForTest12345"
            )
        } returns Result.success("Yes, you can afford a ₹300 dinner tonight.")

        val result = useCase("p1", "Can I afford dinner tonight?")
        assertEquals("Yes, you can afford a ₹300 dinner tonight.", result.message)
        assertEquals("AI_MODEL", result.groundedSources.firstOrNull()?.type)
    }

    @Test
    fun `when AI is disabled, deterministic Safe-to-Spend calculation is used for safe spend query`() = runTest {
        every { aiSettingsRepository.isAiEnabled } returns flowOf(false)
        coEvery { calculateSafeToSpendUseCase("p1", any()) } returns SafeToSpendResult(
            dailySafeAmount = Money.of(BigDecimal("750.00"), "INR"),
            remainingBudget = Money.of(BigDecimal("15000.00"), "INR"),
            totalMonthlyBudget = Money.of(BigDecimal("30000.00"), "INR"),
            daysRemaining = 20,
            upcomingCommitments = Money.zero("INR"),
            savingsCommitments = Money.zero("INR"),
            burnRatePerDay = Money.of(BigDecimal("500.00"), "INR"),
            status = SafeToSpendStatus.HEALTHY,
            statusReason = "Spending pace is well within limits."
        )

        val result = useCase("p1", "What is my safe to spend today?")
        assertTrue(result.message.contains("₹750.00"))
        assertTrue(result.message.contains("HEALTHY"))
        assertEquals("CALCULATION", result.groundedSources.firstOrNull()?.type)
    }
}

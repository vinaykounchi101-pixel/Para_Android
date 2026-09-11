package com.paradox.app.domain.usecase.income

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class IncomeUseCasesTest {

    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)

    private lateinit var addIncomeUseCase: AddIncomeUseCase
    private lateinit var calculateCashFlowUseCase: CalculateCashFlowUseCase

    @Before
    fun setUp() {
        addIncomeUseCase = AddIncomeUseCase(incomeRepository)
        calculateCashFlowUseCase = CalculateCashFlowUseCase(incomeRepository, expenseRepository)
    }

    @Test
    fun `addIncome with valid parameters succeeds`() = runTest {
        val result = addIncomeUseCase(
            profileId = "prof_test",
            source = IncomeSource.SALARY,
            money = Money(BigDecimal("50000.00"), "INR"),
            currency = "INR",
            date = LocalDate.now(),
            notes = "Monthly salary"
        )

        assertTrue(result is Result.Success)
        val income = (result as Result.Success).data
        assertEquals("prof_test", income.profileId)
        assertEquals(IncomeSource.SALARY, income.source)
        assertEquals(BigDecimal("50000.00"), income.amount.amount)
        coVerify(exactly = 1) { incomeRepository.addIncome(any()) }
    }

    @Test
    fun `addIncome with zero or negative amount fails`() = runTest {
        val result = addIncomeUseCase(
            profileId = "prof_test",
            source = IncomeSource.FREELANCE,
            money = Money(BigDecimal("0.00"), "INR"),
            currency = "INR",
            date = LocalDate.now()
        )

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { incomeRepository.addIncome(any()) }
    }

    @Test
    fun `addIncome with future date fails`() = runTest {
        val result = addIncomeUseCase(
            profileId = "prof_test",
            source = IncomeSource.BUSINESS,
            money = Money(BigDecimal("15000.00"), "INR"),
            currency = "INR",
            date = LocalDate.now().plusDays(5)
        )

        assertTrue(result is Result.Error)
        coVerify(exactly = 0) { incomeRepository.addIncome(any()) }
    }

    @Test
    fun `calculateCashFlow accurately computes net savings and savings rate`() = runTest {
        val startDate = LocalDate.of(2026, 9, 1)
        val endDate = LocalDate.of(2026, 9, 30)

        every {
            incomeRepository.observeTotalIncomeInRange("prof_test", startDate, endDate, "INR")
        } returns flowOf(Money(BigDecimal("100000.00"), "INR"))

        every {
            expenseRepository.observeTotalSpentInRange("prof_test", startDate, endDate)
        } returns flowOf(Money(BigDecimal("60000.00"), "INR"))

        val summary = calculateCashFlowUseCase("prof_test", startDate, endDate, "INR").first()

        assertEquals(BigDecimal("100000.00"), summary.totalIncome.amount)
        assertEquals(BigDecimal("60000.00"), summary.totalExpense.amount)
        assertEquals(BigDecimal("40000.00"), summary.netCashFlow.amount)
        assertEquals(BigDecimal("40000.00"), summary.savingsAmount.amount)
        assertEquals(40.0, summary.savingsRatePct, 0.01)
    }
}

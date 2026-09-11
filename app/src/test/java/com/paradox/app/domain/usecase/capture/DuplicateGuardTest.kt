package com.paradox.app.domain.usecase.capture

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class DuplicateGuardTest {

    private val expenseRepository = mockk<ExpenseRepository>()
    private lateinit var duplicateGuard: DuplicateGuardUseCase

    @Before
    fun setUp() {
        duplicateGuard = DuplicateGuardUseCase(expenseRepository)
    }

    @Test
    fun `detects duplicate when same amount and title within date window`() = runTest {
        val today = LocalDate.of(2026, 9, 11)
        val existingExpense = Expense(
            id = "exp-1",
            profileId = "prof-1",
            title = "Starbucks Coffee",
            money = Money.of(BigDecimal("350.00"), "INR"),
            categoryId = "cat-1",
            paymentMethodId = "pm-1",
            date = today,
            notes = null,
            recurringFlag = false,
            source = "MANUAL",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { expenseRepository.getExpensesInRange("prof-1", any(), any()) } returns flowOf(listOf(existingExpense))

        val result = duplicateGuard.checkDuplicate(
            profileId = "prof-1",
            amount = Money.of(BigDecimal("350.00"), "INR"),
            title = "Starbucks Coffee",
            date = today
        )

        assertTrue(result.isLikelyDuplicate)
        assertTrue(result.reason?.contains("Possible duplicate") == true)
    }

    @Test
    fun `does not flag duplicate when amount is different`() = runTest {
        val today = LocalDate.of(2026, 9, 11)
        val existingExpense = Expense(
            id = "exp-1",
            profileId = "prof-1",
            title = "Starbucks Coffee",
            money = Money.of(BigDecimal("350.00"), "INR"),
            categoryId = "cat-1",
            paymentMethodId = "pm-1",
            date = today,
            notes = null,
            recurringFlag = false,
            source = "MANUAL",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        coEvery { expenseRepository.getExpensesInRange("prof-1", any(), any()) } returns flowOf(listOf(existingExpense))

        val result = duplicateGuard.checkDuplicate(
            profileId = "prof-1",
            amount = Money.of(BigDecimal("450.00"), "INR"),
            title = "Starbucks Coffee",
            date = today
        )

        assertFalse(result.isLikelyDuplicate)
    }
}

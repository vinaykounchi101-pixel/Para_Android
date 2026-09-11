package com.paradox.app.domain.usecase.engagement

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class EngagementUseCasesTest {

    @Test
    fun `calculateSplitExpense correctly divides bill and distributes cent remainder`() {
        val splitUseCase = CalculateSplitExpenseUseCase()
        val result = splitUseCase(
            totalAmount = Money(BigDecimal("100.00"), "INR"),
            peopleCount = 3,
            tipAmount = Money(BigDecimal("10.00"), "INR")
        )

        // Grand total: 110.00
        // 110.00 / 3 = 36.66 per person
        // 36.66 * 3 = 109.98 -> remainder = 0.02 added to Person 1 = 36.68
        assertEquals(BigDecimal("110.00"), result.grandTotal.amount)
        assertEquals(3, result.shares.size)
        assertEquals(BigDecimal("36.68"), result.shares[0].amount.amount)
        assertEquals(BigDecimal("36.66"), result.shares[1].amount.amount)
        assertEquals(BigDecimal("36.66"), result.shares[2].amount.amount)
    }

    @Test
    fun `trackLoggingStreak computes consecutive days accurately`() = runTest {
        val expenseRepo: ExpenseRepository = mockk(relaxed = true)
        val today = LocalDate.of(2026, 9, 11)

        val expenses = listOf(
            Expense("e1", "prof_1", "Lunch", Money(BigDecimal("150.00"), "INR"), "c1", "pm1", today),
            Expense("e2", "prof_1", "Dinner", Money(BigDecimal("250.00"), "INR"), "c1", "pm1", today.minusDays(1)),
            Expense("e3", "prof_1", "Groceries", Money(BigDecimal("450.00"), "INR"), "c1", "pm1", today.minusDays(2))
        )

        every { expenseRepo.getAllExpenses("prof_1") } returns flowOf(expenses)

        val useCase = TrackLoggingStreakUseCase(expenseRepo)
        val streak = useCase("prof_1", today)

        assertEquals(3, streak.currentStreakDays)
        assertEquals(3, streak.totalExpensesLogged)
        assertTrue(streak.badges.any { it.id == "streak_3" && it.isUnlocked })
    }
}

package com.paradox.app.domain.usecase.expense

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AddExpenseUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var addExpenseUseCase: AddExpenseUseCase

    @Before
    fun setup() {
        expenseRepository = mockk(relaxed = true)
        addExpenseUseCase = AddExpenseUseCase(expenseRepository)
    }

    @Test
    fun `adding valid expense succeeds and persists via repository`() = runTest {
        val result = addExpenseUseCase(
            profileId = "prof_123",
            title = "Groceries",
            money = Money.of("250.00", "INR"),
            categoryId = "cat_food",
            paymentMethodId = "pm_upi",
            date = LocalDate.now(),
            notes = "Weekly grocery run"
        )

        assertTrue(result.isSuccess)
        val expense = (result as Result.Success).data
        assertEquals("Groceries", expense.title)
        assertEquals(Money.of("250.00", "INR"), expense.money)

        coVerify(exactly = 1) { expenseRepository.addExpense(any()) }
    }

    @Test
    fun `adding expense with zero or negative amount returns error`() = runTest {
        val result = addExpenseUseCase(
            profileId = "prof_123",
            title = "Zero expense",
            money = Money.ZERO_INR,
            categoryId = "cat_food",
            paymentMethodId = "pm_upi",
            date = LocalDate.now()
        )

        assertTrue(result.isError)
        coVerify(exactly = 0) { expenseRepository.addExpense(any()) }
    }

    @Test
    fun `adding expense with future date returns error`() = runTest {
        val tomorrow = LocalDate.now().plusDays(1)
        val result = addExpenseUseCase(
            profileId = "prof_123",
            title = "Future expense",
            money = Money.of("100.00", "INR"),
            categoryId = "cat_food",
            paymentMethodId = "pm_upi",
            date = tomorrow
        )

        assertTrue(result.isError)
        coVerify(exactly = 0) { expenseRepository.addExpense(any()) }
    }

    @Test
    fun `adding expense with blank title returns error`() = runTest {
        val result = addExpenseUseCase(
            profileId = "prof_123",
            title = "   ",
            money = Money.of("100.00", "INR"),
            categoryId = "cat_food",
            paymentMethodId = "pm_upi",
            date = LocalDate.now()
        )

        assertTrue(result.isError)
        coVerify(exactly = 0) { expenseRepository.addExpense(any()) }
    }
}

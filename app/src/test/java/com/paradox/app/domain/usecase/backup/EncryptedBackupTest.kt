package com.paradox.app.domain.usecase.backup

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class EncryptedBackupTest {

    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val paymentMethodRepository: PaymentMethodRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val recurringRepository: RecurringExpenseRepository = mockk(relaxed = true)
    private val savingsGoalRepository: SavingsGoalRepository = mockk(relaxed = true)

    @Test
    fun `export and restore roundtrip decrypts data correctly with matching passphrase`() = runTest {
        val exportUseCase = EncryptedBackupUseCase(
            expenseRepository,
            incomeRepository,
            budgetRepository,
            categoryRepository,
            paymentMethodRepository,
            accountRepository,
            recurringRepository,
            savingsGoalRepository
        )

        val restoreUseCase = RestoreBackupUseCase(
            expenseRepository,
            incomeRepository,
            budgetRepository,
            categoryRepository,
            accountRepository,
            savingsGoalRepository
        )

        val categories = listOf(
            Category("cat_1", "prof_1", "Groceries", "shopping_cart", "#10B981", false, true)
        )
        val expenses = listOf(
            Expense("exp_1", "prof_1", "Milk & Eggs", Money(BigDecimal("180.00"), "INR"), "cat_1", "pm_1", LocalDate.of(2026, 9, 10))
        )

        every { categoryRepository.getCategories("prof_1") } returns flowOf(categories)
        every { expenseRepository.getAllExpenses("prof_1") } returns flowOf(expenses)
        every { incomeRepository.getAllIncomes("prof_1") } returns flowOf(emptyList())
        every { budgetRepository.getAllBudgets("prof_1") } returns flowOf(emptyList())
        every { accountRepository.getAccounts("prof_1") } returns flowOf(emptyList())
        every { recurringRepository.getAllRecurring("prof_1") } returns flowOf(emptyList())
        every { savingsGoalRepository.getAllGoals("prof_1") } returns flowOf(emptyList())

        val encryptedPayload = exportUseCase("prof_1", "secret12345")
        assertTrue(encryptedPayload.contains("paradoxBackup"))

        // Attempt restore with wrong passphrase
        val failedResult = restoreUseCase("prof_1", encryptedPayload, "wrongpassword")
        assertFalse(failedResult.success)

        // Restore with valid passphrase
        val successResult = restoreUseCase("prof_1", encryptedPayload, "secret12345")
        assertTrue(successResult.success)
        assertEquals(2, successResult.itemsRestoredCount)
        coVerify(exactly = 1) { categoryRepository.addCategory(any()) }
        coVerify(exactly = 1) { expenseRepository.addExpense(any()) }
    }
}

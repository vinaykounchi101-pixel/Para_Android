package com.paradox.app.domain.usecase.backup

import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.model.DebtStatus
import com.paradox.app.domain.model.DebtType
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.model.PaymentMethodType
import com.paradox.app.domain.model.Profile
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.model.SavingsGoal
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.DebtRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import com.paradox.app.domain.repository.ProfileRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import io.mockk.coEvery
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
import java.time.Instant
import java.time.LocalDate

class EncryptedBackupTest {

    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val expenseRepository: ExpenseRepository = mockk(relaxed = true)
    private val incomeRepository: IncomeRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val paymentMethodRepository: PaymentMethodRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val recurringRepository: RecurringExpenseRepository = mockk(relaxed = true)
    private val savingsGoalRepository: SavingsGoalRepository = mockk(relaxed = true)
    private val debtRepository: DebtRepository = mockk(relaxed = true)
    private val sessionDataStore: SessionDataStore = mockk(relaxed = true)

    @Test
    fun `export and restore roundtrip decrypts and restores all 9 entities including debts with matching passphrase`() = runTest {
        val exportUseCase = EncryptedBackupUseCase(
            expenseRepository,
            incomeRepository,
            budgetRepository,
            categoryRepository,
            paymentMethodRepository,
            accountRepository,
            recurringRepository,
            savingsGoalRepository,
            debtRepository
        )

        val restoreUseCase = RestoreBackupUseCase(
            profileRepository,
            expenseRepository,
            incomeRepository,
            budgetRepository,
            categoryRepository,
            paymentMethodRepository,
            accountRepository,
            recurringRepository,
            savingsGoalRepository,
            debtRepository,
            sessionDataStore
        )

        val testProfile = Profile("prof_1", "Vinay", AuthType.PIN, false)
        coEvery { profileRepository.getProfileById("prof_1") } returns testProfile
        every { profileRepository.getAllProfiles() } returns flowOf(listOf(testProfile))

        val categories = listOf(
            Category("cat_1", "prof_1", "Groceries", "shopping_cart", "#10B981", false, true)
        )
        val paymentMethods = listOf(
            PaymentMethod("pm_1", "prof_1", PaymentMethodType.UPI, "Google Pay", true)
        )
        val accounts = listOf(
            Account("acc_1", "prof_1", "HDFC Salary", AccountType.BANK, "INR", Money(BigDecimal("50000.00"), "INR"), "#3B82F6", "account_balance", true)
        )
        val budgets = listOf(
            Budget("bud_1", "prof_1", BudgetType.MONTHLY, Money(BigDecimal("25000.00"), "INR"), null, 80)
        )
        val recurring = listOf(
            RecurringExpense("rec_1", "prof_1", "Netflix 4K", Money(BigDecimal("649.00"), "INR"), "INR", "cat_1", "pm_1", RecurringFrequency.MONTHLY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 1))
        )
        val savingsGoals = listOf(
            SavingsGoal("goal_1", "prof_1", "Emergency Fund", Money(BigDecimal("100000.00"), "INR"), Money(BigDecimal("25000.00"), "INR"), "INR", LocalDate.of(2026, 12, 31), "#10B981", "savings")
        )
        val expenses = listOf(
            Expense("exp_1", "prof_1", "Milk & Eggs", Money(BigDecimal("180.00"), "INR"), "cat_1", "pm_1", LocalDate.of(2026, 9, 10))
        )
        val incomes = listOf(
            Income("inc_1", "prof_1", IncomeSource.SALARY, Money(BigDecimal("75000.00"), "INR"), "INR", LocalDate.of(2026, 9, 1))
        )
        val debts = listOf(
            Debt("debt_1", "prof_1", "Rahul Sharma", "+919876543210", DebtType.LENT, Money(BigDecimal("5000.00"), "INR"), Money(BigDecimal("3000.00"), "INR"), LocalDate.of(2026, 10, 15), "Laptop repair", DebtStatus.ACTIVE, true, Instant.now(), Instant.now())
        )
        val repayments = listOf(
            DebtRepayment("rep_1", "debt_1", "prof_1", Money(BigDecimal("2000.00"), "INR"), LocalDate.of(2026, 9, 11), "Cash return", Instant.now())
        )

        every { categoryRepository.getCategories("prof_1") } returns flowOf(categories)
        every { paymentMethodRepository.getPaymentMethods("prof_1") } returns flowOf(paymentMethods)
        every { accountRepository.getAccounts("prof_1") } returns flowOf(accounts)
        every { budgetRepository.getAllBudgets("prof_1") } returns flowOf(budgets)
        every { recurringRepository.getAllRecurring("prof_1") } returns flowOf(recurring)
        every { savingsGoalRepository.getAllGoals("prof_1") } returns flowOf(savingsGoals)
        every { expenseRepository.getAllExpenses("prof_1") } returns flowOf(expenses)
        every { incomeRepository.getAllIncomes("prof_1") } returns flowOf(incomes)
        every { debtRepository.getAllDebts("prof_1") } returns flowOf(debts)
        every { debtRepository.getRepaymentsForDebt("prof_1", "debt_1") } returns flowOf(repayments)

        val encryptedPayload = exportUseCase("prof_1", "strongPassphrase#2026")
        assertTrue(encryptedPayload.contains("paradoxBackup"))

        // Attempt restore with wrong passphrase
        val failedResult = restoreUseCase("prof_1", encryptedPayload, "wrongpassword")
        assertFalse(failedResult.success)
        assertEquals(0, failedResult.itemsRestoredCount)

        // Restore with valid passphrase
        val successResult = restoreUseCase("prof_1", encryptedPayload, "strongPassphrase#2026")
        assertTrue(successResult.success)
        assertEquals(9, successResult.itemsRestoredCount)

        coVerify(atLeast = 1) { categoryRepository.addCategory(any()) }
        coVerify(atLeast = 1) { paymentMethodRepository.addPaymentMethod(any()) }
        coVerify(atLeast = 1) { accountRepository.addAccount(any()) }
        coVerify(atLeast = 1) { budgetRepository.setBudget(any()) }
        coVerify(atLeast = 1) { recurringRepository.addRecurring(any()) }
        coVerify(atLeast = 1) { savingsGoalRepository.addGoal(any()) }
        coVerify(atLeast = 1) { expenseRepository.addExpense(any()) }
        coVerify(atLeast = 1) { incomeRepository.addIncome(any()) }
        coVerify(atLeast = 1) { debtRepository.insertDebt(any()) }
        coVerify(atLeast = 1) { debtRepository.recordRepayment(any()) }
    }
}

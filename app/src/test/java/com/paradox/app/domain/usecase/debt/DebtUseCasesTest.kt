package com.paradox.app.domain.usecase.debt

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.model.DebtStatus
import com.paradox.app.domain.model.DebtSummary
import com.paradox.app.domain.model.DebtType
import com.paradox.app.domain.repository.DebtRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class DebtUseCasesTest {

    private val debtRepository: DebtRepository = mockk(relaxed = true)

    private lateinit var addDebtUseCase: AddDebtUseCase
    private lateinit var getDebtsUseCase: GetDebtsUseCase
    private lateinit var getDebtSummaryUseCase: GetDebtSummaryUseCase
    private lateinit var recordDebtRepaymentUseCase: RecordDebtRepaymentUseCase
    private lateinit var deleteDebtUseCase: DeleteDebtUseCase
    private lateinit var updateDebtUseCase: UpdateDebtUseCase

    @Before
    fun setUp() {
        addDebtUseCase = AddDebtUseCase(debtRepository)
        getDebtsUseCase = GetDebtsUseCase(debtRepository)
        getDebtSummaryUseCase = GetDebtSummaryUseCase(debtRepository)
        recordDebtRepaymentUseCase = RecordDebtRepaymentUseCase(debtRepository)
        deleteDebtUseCase = DeleteDebtUseCase(debtRepository)
        updateDebtUseCase = UpdateDebtUseCase(debtRepository)
    }

    @Test
    fun `addDebt with valid lent parameters succeeds and sets remaining equal to initial amount`() = runTest {
        val result = addDebtUseCase(
            profileId = "prof_test",
            personName = "Rahul Sharma",
            personContactNumber = "+919876543210",
            debtType = DebtType.LENT,
            amount = BigDecimal("5000.00"),
            currency = "INR",
            dueDate = LocalDate.of(2026, 10, 15),
            notes = "Loan for laptop repair",
            reminderEnabled = true
        )

        assertTrue(result is Result.Success)
        val debt = (result as Result.Success).data
        assertEquals("prof_test", debt.profileId)
        assertEquals("Rahul Sharma", debt.personName)
        assertEquals("+919876543210", debt.personContactNumber)
        assertEquals(DebtType.LENT, debt.debtType)
        assertEquals(BigDecimal("5000.00"), debt.initialAmount.amount)
        assertEquals(BigDecimal("5000.00"), debt.remainingAmount.amount)
        assertEquals(DebtStatus.ACTIVE, debt.status)
        assertTrue(debt.reminderEnabled)

        val debtSlot = slot<Debt>()
        coVerify(exactly = 1) { debtRepository.insertDebt(capture(debtSlot)) }
        assertEquals("Rahul Sharma", debtSlot.captured.personName)
    }

    @Test
    fun `addDebt with blank person name fails with error`() = runTest {
        val result = addDebtUseCase(
            profileId = "prof_test",
            personName = "   ",
            personContactNumber = null,
            debtType = DebtType.BORROWED,
            amount = BigDecimal("2000.00"),
            currency = "INR"
        )

        assertTrue(result is Result.Error)
        assertEquals("Person name cannot be blank", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { debtRepository.insertDebt(any()) }
    }

    @Test
    fun `addDebt with zero or negative amount fails with error`() = runTest {
        val result = addDebtUseCase(
            profileId = "prof_test",
            personName = "Amit Verma",
            personContactNumber = null,
            debtType = DebtType.LENT,
            amount = BigDecimal("0.00"),
            currency = "INR"
        )

        assertTrue(result is Result.Error)
        assertEquals("Debt amount must be greater than zero", (result as Result.Error).exception.message)
        coVerify(exactly = 0) { debtRepository.insertDebt(any()) }
    }

    @Test
    fun `getDebtSummary accurately retrieves debt summary flow`() = runTest {
        val expectedSummary = DebtSummary(
            totalReceivable = Money(BigDecimal("15000.00"), "INR"),
            totalPayable = Money(BigDecimal("5000.00"), "INR"),
            netBalance = Money(BigDecimal("10000.00"), "INR")
        )
        every { debtRepository.observeDebtSummary("prof_test") } returns flowOf(expectedSummary)

        val summary = getDebtSummaryUseCase("prof_test").first()

        assertEquals(BigDecimal("15000.00"), summary.totalReceivable.amount)
        assertEquals(BigDecimal("5000.00"), summary.totalPayable.amount)
        assertEquals(BigDecimal("10000.00"), summary.netBalance.amount)
    }

    @Test
    fun `recordDebtRepayment with valid amount succeeds and delegates to repository`() = runTest {
        val existingDebt = Debt(
            id = "debt_101",
            profileId = "prof_test",
            personName = "Vikram",
            personContactNumber = null,
            debtType = DebtType.LENT,
            initialAmount = Money(BigDecimal("5000.00"), "INR"),
            remainingAmount = Money(BigDecimal("5000.00"), "INR"),
            dueDate = null,
            notes = null,
            status = DebtStatus.ACTIVE,
            reminderEnabled = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { debtRepository.getDebtById("prof_test", "debt_101") } returns existingDebt

        val result = recordDebtRepaymentUseCase(
            profileId = "prof_test",
            debtId = "debt_101",
            amount = BigDecimal("2000.00"),
            currency = "INR",
            repaymentDate = LocalDate.now(),
            notes = "Partial payment cash"
        )

        assertTrue(result is Result.Success)
        val repayment = (result as Result.Success).data
        assertEquals("debt_101", repayment.debtId)
        assertEquals("prof_test", repayment.profileId)
        assertEquals(BigDecimal("2000.00"), repayment.amount.amount)

        coVerify(exactly = 1) { debtRepository.recordRepayment(any()) }
    }

    @Test
    fun `recordDebtRepayment with amount exceeding remaining balance fails with error`() = runTest {
        val existingDebt = Debt(
            id = "debt_102",
            profileId = "prof_test",
            personName = "Vikram",
            personContactNumber = null,
            debtType = DebtType.LENT,
            initialAmount = Money(BigDecimal("5000.00"), "INR"),
            remainingAmount = Money(BigDecimal("1500.00"), "INR"),
            dueDate = null,
            notes = null,
            status = DebtStatus.ACTIVE,
            reminderEnabled = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        coEvery { debtRepository.getDebtById("prof_test", "debt_102") } returns existingDebt

        val result = recordDebtRepaymentUseCase(
            profileId = "prof_test",
            debtId = "debt_102",
            amount = BigDecimal("2000.00"),
            currency = "INR",
            repaymentDate = LocalDate.now(),
            notes = "Overpayment"
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception.message!!.contains("cannot exceed remaining debt"))
        coVerify(exactly = 0) { debtRepository.recordRepayment(any()) }
    }

    @Test
    fun `deleteDebt delegates to repository`() = runTest {
        val result = deleteDebtUseCase("prof_test", "debt_101")
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { debtRepository.deleteDebt("prof_test", "debt_101") }
    }
}

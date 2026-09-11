package com.paradox.app.domain.usecase.capture

import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.model.PaymentMethodType
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CsvImportUseCaseTest {

    private val expenseRepository = mockk<ExpenseRepository>(relaxed = true)
    private val categoryRepository = mockk<CategoryRepository>()
    private val paymentMethodRepository = mockk<PaymentMethodRepository>()
    private val duplicateGuard = mockk<DuplicateGuardUseCase>()
    private lateinit var csvImportUseCase: CsvImportUseCase

    @Before
    fun setUp() {
        val nlParser = NaturalLanguageParser()
        csvImportUseCase = CsvImportUseCase(
            expenseRepository,
            categoryRepository,
            paymentMethodRepository,
            nlParser,
            duplicateGuard
        )
    }

    @Test
    fun `parse and preview parses standard CSV with headers correctly`() = runTest {
        val csv = """
            Date,Description,Amount,Category,Payment Method
            2026-09-01,Uber Ride,250.00,Transportation,UPI
            2026-09-02,Starbucks,420.50,Food & Dining,Card
            2026-09-03,Blinkit Grocery,890.00,Groceries,UPI
        """.trimIndent()

        val sampleCat = Category("cat-1", "prof-1", "Transportation", "DirectionsCar", "#123456", false, false)
        val samplePm = PaymentMethod("pm-1", "prof-1", PaymentMethodType.UPI, "UPI", false)

        coEvery { categoryRepository.getCategories("prof-1") } returns flowOf(listOf(sampleCat))
        coEvery { paymentMethodRepository.getPaymentMethods("prof-1") } returns flowOf(listOf(samplePm))
        coEvery { duplicateGuard.checkDuplicate("prof-1", any(), any(), any()) } returns DuplicateWarning(false)

        val preview = csvImportUseCase.parseAndPreview(csv, "prof-1")

        assertEquals(3, preview.totalRows)
        assertEquals(3, preview.validCandidates.size)
        assertEquals(0, preview.invalidRowCount)
        assertEquals("Uber Ride", preview.validCandidates[0].title)
        assertEquals(0, BigDecimal("250.00").compareTo(preview.validCandidates[0].amount.amount))
    }
}

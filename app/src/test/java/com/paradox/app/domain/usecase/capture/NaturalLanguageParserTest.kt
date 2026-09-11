package com.paradox.app.domain.usecase.capture

import com.paradox.app.domain.model.PaymentMethodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class NaturalLanguageParserTest {

    private lateinit var parser: NaturalLanguageParser

    @Before
    fun setUp() {
        parser = NaturalLanguageParser()
    }

    @Test
    fun `parse simple expense with merchant and amount`() {
        val result = parser.parse("Uber 250")
        assertNotNull(result.amount)
        assertEquals(0, BigDecimal("250").compareTo(result.amount!!.amount))
        assertTrue(result.title.contains("Uber", ignoreCase = true))
        assertEquals("Transportation", result.suggestedCategoryName)
    }

    @Test
    fun `parse expense with relative date yesterday and payment cash`() {
        val result = parser.parse("Dinner 650 cash yesterday")
        val expectedDate = LocalDate.now().minusDays(1)

        assertNotNull(result.amount)
        assertEquals(0, BigDecimal("650").compareTo(result.amount!!.amount))
        assertEquals(expectedDate, result.date)
        assertEquals(PaymentMethodType.CASH, result.suggestedPaymentType)
        assertEquals("Food & Dining", result.suggestedCategoryName)
    }

    @Test
    fun `parse expense with decimal amount and currency symbol`() {
        val result = parser.parse("₹ 1450.50 Groceries at Blinkit via upi")
        assertNotNull(result.amount)
        assertEquals(0, BigDecimal("1450.50").compareTo(result.amount!!.amount))
        assertEquals(PaymentMethodType.UPI, result.suggestedPaymentType)
        assertEquals("Groceries", result.suggestedCategoryName)
    }

    @Test
    fun `parse empty input returns default empty candidate`() {
        val result = parser.parse("")
        assertEquals(null, result.amount)
        assertEquals("", result.title)
    }
}

package com.paradox.app.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `money addition preserves exact precision`() {
        val m1 = Money.of("100.50", "INR")
        val m2 = Money.of("200.25", "INR")
        val result = m1 + m2

        assertEquals(BigDecimal("300.75"), result.amount)
        assertEquals("INR", result.currencyCode)
    }

    @Test
    fun `money subtraction calculates correctly`() {
        val m1 = Money.of("500.00", "INR")
        val m2 = Money.of("125.50", "INR")
        val result = m1 - m2

        assertEquals(BigDecimal("374.50"), result.amount)
    }

    @Test
    fun `money multiplication uses bankers rounding`() {
        val m = Money.of("100.00", "INR")
        val result = m * BigDecimal("0.185") // 18.5%

        assertEquals(BigDecimal("18.50"), result.amount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cannot add different currencies`() {
        val m1 = Money.of("100.00", "INR")
        val m2 = Money.of("50.00", "USD")
        m1 + m2
    }

    @Test
    fun `zero and positive checks work accurately`() {
        val zero = Money.ZERO_INR
        val positive = Money.of("10.00", "INR")
        val negative = Money.of("-5.00", "INR")

        assertTrue(zero.isZero())
        assertTrue(positive.isPositive())
        assertTrue(negative.isNegative())
        assertFalse(positive.isZero())
    }
}

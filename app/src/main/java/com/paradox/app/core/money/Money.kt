package com.paradox.app.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency
import java.util.Locale

/**
 * Immutable value class representing monetary values with exact decimal arithmetic.
 * Never uses floating-point types (Float/Double) to eliminate precision loss.
 */
data class Money(
    val amount: BigDecimal,
    val currencyCode: String = "INR"
) : Comparable<Money> {

    init {
        require(amount.scale() <= 4) { "Monetary amount scale cannot exceed 4 decimal places" }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = this.amount.add(other.amount))
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return copy(amount = this.amount.subtract(other.amount))
    }

    operator fun times(multiplier: BigDecimal): Money {
        return copy(amount = this.amount.multiply(multiplier).setScale(2, RoundingMode.HALF_EVEN))
    }

    operator fun div(divisor: BigDecimal): Money {
        require(divisor.compareTo(BigDecimal.ZERO) != 0) { "Cannot divide money by zero" }
        return copy(amount = this.amount.divide(divisor, 2, RoundingMode.HALF_EVEN))
    }

    fun isZero(): Boolean = amount.compareTo(BigDecimal.ZERO) == 0
    fun isPositive(): Boolean = amount.compareTo(BigDecimal.ZERO) > 0
    fun isNegative(): Boolean = amount.compareTo(BigDecimal.ZERO) < 0

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return this.amount.compareTo(other.amount)
    }

    private fun requireSameCurrency(other: Money) {
        require(this.currencyCode == other.currencyCode) {
            "Cannot perform direct arithmetic on different currencies (${this.currencyCode} vs ${other.currencyCode})"
        }
    }

    companion object {
        val ZERO_INR = Money(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN), "INR")

        fun zero(currencyCode: String = "INR"): Money {
            return Money(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN), currencyCode)
        }

        fun of(amount: String, currencyCode: String = "INR"): Money {
            val sanitized = amount.trim().replace(",", "")
            val parsed = BigDecimal(sanitized).setScale(2, RoundingMode.HALF_EVEN)
            return Money(parsed, currencyCode)
        }

        fun of(amount: Long, currencyCode: String = "INR"): Money {
            return Money(BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_EVEN), currencyCode)
        }

        fun of(amount: BigDecimal, currencyCode: String = "INR"): Money {
            return Money(amount.setScale(2, RoundingMode.HALF_EVEN), currencyCode)
        }
    }
}

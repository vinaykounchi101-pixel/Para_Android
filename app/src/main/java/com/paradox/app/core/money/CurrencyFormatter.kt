package com.paradox.app.core.money

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyFormatter {

    fun format(
        money: Money,
        includeSymbol: Boolean = true,
        includeDecimals: Boolean = true
    ): String {
        return format(money.amount, money.currencyCode, includeSymbol, includeDecimals)
    }

    fun format(
        amount: BigDecimal,
        currencyCode: String = "INR",
        includeSymbol: Boolean = true,
        includeDecimals: Boolean = true
    ): String {
        val locale = if (currencyCode.equals("INR", ignoreCase = true)) {
            Locale("en", "IN")
        } else {
            Locale.getDefault()
        }

        val format = NumberFormat.getCurrencyInstance(locale)
        try {
            format.currency = Currency.getInstance(currencyCode)
        } catch (_: Exception) {
            // Fallback gracefully
        }

        if (!includeDecimals) {
            format.maximumFractionDigits = 0
            format.minimumFractionDigits = 0
        } else {
            format.maximumFractionDigits = 2
            format.minimumFractionDigits = 2
        }

        val formatted = format.format(amount)
        return if (!includeSymbol) {
            val symbol = format.currency?.symbol ?: ""
            formatted.replace(symbol, "").trim()
        } else {
            formatted
        }
    }

    fun formatCompact(
        money: Money,
        currencyCode: String = "INR"
    ): String {
        val amount = money.amount
        return when {
            amount >= BigDecimal("10000000") -> { // 1 Crore
                val cr = amount.divide(BigDecimal("10000000"), 2, java.math.RoundingMode.HALF_EVEN)
                "₹${cr.stripTrailingZeros().toPlainString()} Cr"
            }
            amount >= BigDecimal("100000") -> { // 1 Lakh
                val lk = amount.divide(BigDecimal("100000"), 2, java.math.RoundingMode.HALF_EVEN)
                "₹${lk.stripTrailingZeros().toPlainString()} L"
            }
            amount >= BigDecimal("1000") -> {
                val k = amount.divide(BigDecimal("1000"), 1, java.math.RoundingMode.HALF_EVEN)
                "₹${k.stripTrailingZeros().toPlainString()}k"
            }
            else -> format(money, includeSymbol = true, includeDecimals = false)
        }
    }
}

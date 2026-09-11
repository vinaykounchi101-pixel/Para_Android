package com.paradox.app.domain.usecase.capture

import com.paradox.app.domain.model.PaymentMethodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ReceiptOcrParserTest {

    private lateinit var ocrParser: ReceiptOcrParser

    @Before
    fun setUp() {
        val nlParser = NaturalLanguageParser()
        ocrParser = ReceiptOcrParser(nlParser)
    }

    @Test
    fun `parse receipt text extracts merchant, date, total amount and payment method`() {
        val receiptLines = listOf(
            "STARBUCKS COFFEE",
            "Store #1042 - Connaught Place",
            "Date: 2026-08-15",
            "1x Caramel Macchiato    350.00",
            "1x Butter Croissant     220.00",
            "Subtotal:               570.00",
            "Tax (CGST+SGST):         28.50",
            "GRAND TOTAL:            598.50",
            "PAID VIA UPI / GPAY"
        )

        val candidate = ocrParser.parseReceiptText(receiptLines)

        assertEquals("STARBUCKS COFFEE", candidate.title)
        assertNotNull(candidate.amount)
        assertEquals(0, BigDecimal("598.50").compareTo(candidate.amount!!.amount))
        assertEquals(LocalDate.of(2026, 8, 15), candidate.date)
        assertEquals(PaymentMethodType.UPI, candidate.suggestedPaymentType)
        assertEquals("Food & Dining", candidate.suggestedCategoryName)
    }

    @Test
    fun `parse supermarket receipt with dd-mm-yyyy date format`() {
        val receiptLines = listOf(
            "NATURES BASKET",
            "Invoice No: 88412",
            "Date: 10/09/2026",
            "Groceries & Vegetables",
            "TOTAL AMOUNT: 1240.00",
            "CASH"
        )

        val candidate = ocrParser.parseReceiptText(receiptLines)

        assertEquals("NATURES BASKET", candidate.title)
        assertNotNull(candidate.amount)
        assertEquals(0, BigDecimal("1240.00").compareTo(candidate.amount!!.amount))
        assertEquals(LocalDate.of(2026, 9, 10), candidate.date)
        assertEquals(PaymentMethodType.CASH, candidate.suggestedPaymentType)
    }
}

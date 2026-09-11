package com.paradox.app.domain.usecase.capture

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.PaymentMethodType
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptOcrParser @Inject constructor(
    private val nlParser: NaturalLanguageParser
) {

    private val totalKeywords = listOf(
        "grand total", "total amount", "total bill", "net total", "final total",
        "net amount", "amount due", "balance due", "total due", "total", "paid"
    )

    fun parseReceiptText(lines: List<String>, defaultCurrency: String = "INR"): ParsedExpenseCandidate {
        if (lines.isEmpty()) {
            return nlParser.parse("", defaultCurrency)
        }

        val fullText = lines.joinToString("\n")
        var extractedAmount: BigDecimal? = null
        var extractedDate: LocalDate? = null
        var extractedMerchant: String? = null
        var suggestedPayment: PaymentMethodType? = null

        // 1. Extract Merchant (Scan top lines, filtering out common header noise)
        for (line in lines.take(5)) {
            val cleaned = line.trim()
            if (cleaned.length in 3..40 &&
                !cleaned.contains(Regex("""\b(tax|invoice|bill|receipt|gst|vat|tel|phone|ph|date|time|welcome|cashier|order)\b""", RegexOption.IGNORE_CASE)) &&
                !cleaned.matches(Regex("""^[\d\W_]+$"""))
            ) {
                extractedMerchant = cleaned
                break
            }
        }

        // 2. Extract Total Amount (Search from bottom to top, prioritizing Grand Total and avoiding Subtotal)
        val totalPattern = Regex("""\b(grand total|total amount|total bill|net total|final total|net amount|amount due|balance due|total due|total|paid)\b""", RegexOption.IGNORE_CASE)

        for (i in lines.indices.reversed()) {
            val line = lines[i]
            // Skip lines that are purely subtotal if they don't contain grand total
            if (line.contains(Regex("""\bsubtotal\b""", RegexOption.IGNORE_CASE)) && !line.contains(Regex("""\bgrand\b""", RegexOption.IGNORE_CASE))) {
                continue
            }
            if (totalPattern.containsMatchIn(line)) {
                val lineAmount = extractFirstAmount(line)
                if (lineAmount != null) {
                    extractedAmount = lineAmount
                    break
                } else if (i + 1 < lines.size) {
                    val nextLineAmount = extractFirstAmount(lines[i + 1])
                    if (nextLineAmount != null) {
                        extractedAmount = nextLineAmount
                        break
                    }
                }
            }
        }

        // Fallback: Find the highest reasonable numeric value
        if (extractedAmount == null) {
            val allAmounts = lines.mapNotNull { extractFirstAmount(it) }
            if (allAmounts.isNotEmpty()) {
                extractedAmount = allAmounts.maxOrNull()
            }
        }

        // 3. Extract Date
        val dateRegexList = listOf(
            Regex("""\b(\d{4})[/-](\d{1,2})[/-](\d{1,2})\b"""), // yyyy-mm-dd
            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b"""), // dd-mm-yyyy
            Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2})\b""")    // dd-mm-yy
        )

        for (line in lines) {
            for (dateRegex in dateRegexList) {
                val match = dateRegex.find(line)
                if (match != null) {
                    try {
                        val g = match.groupValues
                        if (g[1].length == 4) {
                            extractedDate = LocalDate.of(g[1].toInt(), g[2].toInt(), g[3].toInt())
                        } else if (g[3].length == 4) {
                            extractedDate = LocalDate.of(g[3].toInt(), g[2].toInt(), g[1].toInt())
                        } else if (g[3].length == 2) {
                            val year = 2000 + g[3].toInt()
                            extractedDate = LocalDate.of(year, g[2].toInt(), g[1].toInt())
                        }
                        break
                    } catch (_: Exception) {}
                }
            }
            if (extractedDate != null) break
        }

        // 4. Extract Payment Method
        val upperText = fullText.uppercase()
        when {
            upperText.contains("UPI") || upperText.contains("GPAY") || upperText.contains("PHONEPE") || upperText.contains("PAYTM") ->
                suggestedPayment = PaymentMethodType.UPI
            upperText.contains("CREDIT") || upperText.contains("MASTER") || upperText.contains("VISA") || upperText.contains("AMEX") ->
                suggestedPayment = PaymentMethodType.CREDIT_CARD
            upperText.contains("DEBIT") ->
                suggestedPayment = PaymentMethodType.DEBIT_CARD
            upperText.contains("CASH") ->
                suggestedPayment = PaymentMethodType.CASH
            upperText.contains("NET BANKING") ->
                suggestedPayment = PaymentMethodType.BANK_ACCOUNT
        }

        // 5. Category Suggestion via NaturalLanguageParser heuristic on full text
        val nlParsed = nlParser.parse(fullText, defaultCurrency)

        val finalMerchant = extractedMerchant ?: nlParsed.title.ifBlank { "Scanned Receipt" }
        val finalDate = extractedDate ?: LocalDate.now()
        val finalAmount = extractedAmount?.let { Money.of(it, defaultCurrency) } ?: nlParsed.amount

        return ParsedExpenseCandidate(
            title = finalMerchant,
            amount = finalAmount,
            date = finalDate,
            suggestedCategoryName = nlParsed.suggestedCategoryName,
            suggestedPaymentType = suggestedPayment ?: nlParsed.suggestedPaymentType,
            notes = "Scanned receipt / OCR",
            rawInput = fullText
        )
    }

    private fun extractFirstAmount(text: String): BigDecimal? {
        val amountRegex = Regex("""(?:(?:rs\.?|inr|₹|\$|€|£)\s*)?(\b\d{1,6}(?:\.\d{1,2})?\b)""", RegexOption.IGNORE_CASE)
        val match = amountRegex.findAll(text).lastOrNull() ?: return null
        return try {
            val v = BigDecimal(match.groupValues[1])
            if (v > BigDecimal.ZERO && v < BigDecimal(10000000)) v else null
        } catch (_: Exception) {
            null
        }
    }
}

package com.paradox.app.domain.usecase.capture

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.PaymentMethodType
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedExpenseCandidate(
    val title: String,
    val amount: Money?,
    val date: LocalDate,
    val suggestedCategoryName: String?,
    val suggestedPaymentType: PaymentMethodType?,
    val notes: String?,
    val rawInput: String
)

@Singleton
class NaturalLanguageParser @Inject constructor() {

    private val paymentKeywords = mapOf(
        "cash" to PaymentMethodType.CASH,
        "upi" to PaymentMethodType.UPI,
        "gpay" to PaymentMethodType.UPI,
        "phonepe" to PaymentMethodType.UPI,
        "paytm" to PaymentMethodType.UPI,
        "card" to PaymentMethodType.DEBIT_CARD,
        "debit" to PaymentMethodType.DEBIT_CARD,
        "credit" to PaymentMethodType.CREDIT_CARD,
        "cc" to PaymentMethodType.CREDIT_CARD,
        "bank" to PaymentMethodType.BANK_ACCOUNT,
        "netbanking" to PaymentMethodType.BANK_ACCOUNT,
        "wallet" to PaymentMethodType.WALLET
    )

    private val categoryKeywords = mapOf(
        // Food & Dining
        "food" to "Food & Dining",
        "dinner" to "Food & Dining",
        "lunch" to "Food & Dining",
        "breakfast" to "Food & Dining",
        "snack" to "Food & Dining",
        "snacks" to "Food & Dining",
        "coffee" to "Food & Dining",
        "tea" to "Food & Dining",
        "cafe" to "Food & Dining",
        "starbucks" to "Food & Dining",
        "mcdonalds" to "Food & Dining",
        "kfc" to "Food & Dining",
        "swiggy" to "Food & Dining",
        "zomato" to "Food & Dining",
        "burger" to "Food & Dining",
        "pizza" to "Food & Dining",
        "restaurant" to "Food & Dining",

        // Transportation
        "uber" to "Transportation",
        "ola" to "Transportation",
        "rapido" to "Transportation",
        "cab" to "Transportation",
        "taxi" to "Transportation",
        "auto" to "Transportation",
        "metro" to "Transportation",
        "bus" to "Transportation",
        "train" to "Transportation",
        "flight" to "Transportation",
        "petrol" to "Transportation",
        "diesel" to "Transportation",
        "fuel" to "Transportation",
        "parking" to "Transportation",
        "toll" to "Transportation",

        // Groceries
        "grocery" to "Groceries",
        "groceries" to "Groceries",
        "blinkit" to "Groceries",
        "zepto" to "Groceries",
        "instamart" to "Groceries",
        "supermarket" to "Groceries",
        "vegetables" to "Groceries",
        "fruits" to "Groceries",
        "milk" to "Groceries",
        "walmart" to "Groceries",

        // Utilities
        "wifi" to "Utilities",
        "internet" to "Utilities",
        "electricity" to "Utilities",
        "water" to "Utilities",
        "gas" to "Utilities",
        "recharge" to "Utilities",
        "mobile" to "Utilities",
        "bill" to "Utilities",

        // Entertainment
        "movie" to "Entertainment",
        "cinema" to "Entertainment",
        "netflix" to "Entertainment",
        "spotify" to "Entertainment",
        "prime" to "Entertainment",
        "game" to "Entertainment",
        "gaming" to "Entertainment",

        // Shopping
        "amazon" to "Shopping",
        "flipkart" to "Shopping",
        "myntra" to "Shopping",
        "clothes" to "Shopping",
        "shoes" to "Shopping",
        "shopping" to "Shopping",

        // Healthcare
        "medicine" to "Healthcare",
        "pharmacy" to "Healthcare",
        "doctor" to "Healthcare",
        "hospital" to "Healthcare",
        "clinic" to "Healthcare",
        "apollo" to "Healthcare"
    )

    fun parse(input: String, defaultCurrency: String = "INR"): ParsedExpenseCandidate {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedExpenseCandidate(
                title = "",
                amount = null,
                date = LocalDate.now(),
                suggestedCategoryName = null,
                suggestedPaymentType = null,
                notes = null,
                rawInput = input
            )
        }

        var remainingText = trimmed
        var parsedAmount: Money? = null
        var parsedDate: LocalDate = LocalDate.now()
        var suggestedPayment: PaymentMethodType? = null
        var suggestedCategory: String? = null

        // 1. Amount Extraction (Currency prefix or raw numeric)
        val amountRegex = Regex("""(?:(?:rs\.?|inr|₹|\$|€|£)\s*)?(\b\d+(?:\.\d{1,2})?\b)""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.findAll(remainingText).firstOrNull()

        if (amountMatch != null) {
            val amountStr = amountMatch.groupValues[1]
            try {
                val bd = BigDecimal(amountStr)
                if (bd > BigDecimal.ZERO) {
                    parsedAmount = Money.of(bd, defaultCurrency)
                    remainingText = remainingText.replaceRange(amountMatch.range, " ")
                }
            } catch (_: Exception) {}
        }

        // 2. Date Extraction (Relative dates)
        val today = LocalDate.now()
        when {
            remainingText.contains(Regex("""\byesterday\b""", RegexOption.IGNORE_CASE)) -> {
                parsedDate = today.minusDays(1)
                remainingText = remainingText.replace(Regex("""\byesterday\b""", RegexOption.IGNORE_CASE), " ")
            }
            remainingText.contains(Regex("""\btoday\b""", RegexOption.IGNORE_CASE)) -> {
                parsedDate = today
                remainingText = remainingText.replace(Regex("""\btoday\b""", RegexOption.IGNORE_CASE), " ")
            }
            remainingText.contains(Regex("""\bday before yesterday\b""", RegexOption.IGNORE_CASE)) -> {
                parsedDate = today.minusDays(2)
                remainingText = remainingText.replace(Regex("""\bday before yesterday\b""", RegexOption.IGNORE_CASE), " ")
            }
        }

        // 3. Payment Method Extraction
        for ((keyword, type) in paymentKeywords) {
            val kwRegex = Regex("""\b$keyword\b""", RegexOption.IGNORE_CASE)
            if (kwRegex.containsMatchIn(remainingText)) {
                suggestedPayment = type
                remainingText = remainingText.replace(kwRegex, " ")
                break
            }
        }

        // 4. Category Suggestion from Keywords
        for ((keyword, catName) in categoryKeywords) {
            val kwRegex = Regex("""\b$keyword\b""", RegexOption.IGNORE_CASE)
            if (kwRegex.containsMatchIn(input)) {
                suggestedCategory = catName
                break
            }
        }

        // 5. Clean Title Extraction
        val fillerRegex = Regex("""\b(?:for|at|paid|spent|on|to|via|using|with|rs\.?|inr|in)\b""", RegexOption.IGNORE_CASE)
        val cleanedTitle = remainingText
            .replace(fillerRegex, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        val finalTitle = if (cleanedTitle.isNotBlank()) {
            cleanedTitle
        } else if (suggestedCategory != null) {
            suggestedCategory
        } else {
            "Quick Expense"
        }

        return ParsedExpenseCandidate(
            title = finalTitle,
            amount = parsedAmount,
            date = parsedDate,
            suggestedCategoryName = suggestedCategory,
            suggestedPaymentType = suggestedPayment,
            notes = null,
            rawInput = input
        )
    }
}

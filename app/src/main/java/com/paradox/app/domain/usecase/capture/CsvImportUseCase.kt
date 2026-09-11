package com.paradox.app.domain.usecase.capture

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CsvRowCandidate(
    val index: Int,
    val title: String,
    val amount: Money,
    val date: LocalDate,
    val categoryName: String?,
    val matchedCategoryId: String?,
    val paymentMethodName: String?,
    val matchedPaymentMethodId: String?,
    val notes: String?,
    val isDuplicateWarning: Boolean = false,
    val isValid: Boolean = true,
    val errorMessage: String? = null
)

data class CsvImportPreview(
    val totalRows: Int,
    val validCandidates: List<CsvRowCandidate>,
    val invalidRowCount: Int,
    val detectedHeaders: List<String>
)

@Singleton
class CsvImportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val nlParser: NaturalLanguageParser,
    private val duplicateGuard: DuplicateGuardUseCase
) {

    suspend fun parseAndPreview(csvContent: String, profileId: String, defaultCurrency: String = "INR"): CsvImportPreview {
        val rows = parseCsv(csvContent)
        if (rows.isEmpty()) {
            return CsvImportPreview(0, emptyList(), 0, emptyList())
        }

        val headers = rows.first().map { it.trim().lowercase() }
        val dataRows = rows.drop(1)

        // Identify column indices
        val dateIdx = headers.indexOfFirst { it.contains("date") || it.contains("time") }
        val titleIdx = headers.indexOfFirst { it.contains("title") || it.contains("desc") || it.contains("merchant") || it.contains("narration") || it.contains("particular") || it.contains("payee") || it.contains("name") }
        val amountIdx = headers.indexOfFirst { it.contains("amount") || it.contains("debit") || it.contains("spent") || it.contains("cost") || it.contains("total") || it.contains("value") }
        val catIdx = headers.indexOfFirst { it.contains("category") || it.contains("type") }
        val payIdx = headers.indexOfFirst { it.contains("payment") || it.contains("method") || it.contains("account") || it.contains("mode") }

        val categories = categoryRepository.getCategories(profileId).firstOrNull() ?: emptyList()
        val paymentMethods = paymentMethodRepository.getPaymentMethods(profileId).firstOrNull() ?: emptyList()
        val defaultCategory = categories.firstOrNull { it.isDefault } ?: categories.firstOrNull()
        val defaultPayment = paymentMethods.firstOrNull()

        val validCandidates = mutableListOf<CsvRowCandidate>()
        var invalidCount = 0

        for ((idx, row) in dataRows.withIndex()) {
            if (row.all { it.isBlank() }) continue

            try {
                // 1. Title
                val rawTitle = if (titleIdx >= 0 && titleIdx < row.size) row[titleIdx].trim() else "Imported Expense"
                val title = rawTitle.ifBlank { "Imported Expense" }

                // 2. Amount
                val rawAmountStr = if (amountIdx >= 0 && amountIdx < row.size) {
                    row[amountIdx].replace(Regex("""[^\d.]"""), "")
                } else ""
                val amountBd = BigDecimal(rawAmountStr)
                if (amountBd <= BigDecimal.ZERO) {
                    invalidCount++
                    continue
                }
                val money = Money.of(amountBd, defaultCurrency)

                // 3. Date
                val rawDateStr = if (dateIdx >= 0 && dateIdx < row.size) row[dateIdx].trim() else ""
                val date = parseFlexibleDate(rawDateStr) ?: LocalDate.now()

                // 4. Category Match
                val rawCat = if (catIdx >= 0 && catIdx < row.size) row[catIdx].trim() else null
                val matchedCat = categories.firstOrNull { it.name.equals(rawCat, ignoreCase = true) }
                    ?: nlParser.parse("$title $rawCat", defaultCurrency).suggestedCategoryName?.let { catName ->
                        categories.firstOrNull { it.name.equals(catName, ignoreCase = true) }
                    } ?: defaultCategory

                // 5. Payment Method Match
                val rawPay = if (payIdx >= 0 && payIdx < row.size) row[payIdx].trim() else null
                val matchedPay = paymentMethods.firstOrNull { it.label.equals(rawPay, ignoreCase = true) }
                    ?: defaultPayment

                // 6. Duplicate check
                val dupWarning = duplicateGuard.checkDuplicate(profileId, money, title, date)

                validCandidates.add(
                    CsvRowCandidate(
                        index = idx + 1,
                        title = title,
                        amount = money,
                        date = date,
                        categoryName = matchedCat?.name,
                        matchedCategoryId = matchedCat?.id,
                        paymentMethodName = matchedPay?.label,
                        matchedPaymentMethodId = matchedPay?.id,
                        notes = "Imported from CSV",
                        isDuplicateWarning = dupWarning.isLikelyDuplicate,
                        isValid = true
                    )
                )
            } catch (_: Exception) {
                invalidCount++
            }
        }

        return CsvImportPreview(
            totalRows = dataRows.size,
            validCandidates = validCandidates,
            invalidRowCount = invalidCount,
            detectedHeaders = rows.first()
        )
    }

    suspend fun commitImport(profileId: String, candidates: List<CsvRowCandidate>): Int {
        var count = 0
        val now = Instant.now()
        for (candidate in candidates) {
            if (candidate.isValid && candidate.matchedCategoryId != null && candidate.matchedPaymentMethodId != null) {
                val expense = Expense(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    title = candidate.title,
                    money = candidate.amount,
                    categoryId = candidate.matchedCategoryId,
                    paymentMethodId = candidate.matchedPaymentMethodId,
                    date = candidate.date,
                    notes = candidate.notes,
                    recurringFlag = false,
                    source = "IMPORT",
                    createdAt = now,
                    updatedAt = now
                )
                expenseRepository.addExpense(expense)
                count++
            }
        }
        return count
    }

    private fun parseCsv(content: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val currentField = StringBuilder()
        val currentRow = mutableListOf<String>()
        var inQuotes = false

        for (char in content) {
            when {
                char == '\"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.clear()
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\n' || currentField.isNotEmpty() || currentRow.isNotEmpty()) {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotBlank() }) {
                            result.add(currentRow.toList())
                        }
                        currentRow.clear()
                    }
                }
                else -> {
                    currentField.append(char)
                }
            }
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                result.add(currentRow.toList())
            }
        }
        return result
    }

    private fun parseFlexibleDate(dateStr: String): LocalDate? {
        val regexPatterns = listOf(
            Regex("""^(\d{4})[/-](\d{1,2})[/-](\d{1,2})$"""), // yyyy-mm-dd
            Regex("""^(\d{1,2})[/-](\d{1,2})[/-](\d{4})$"""), // dd-mm-yyyy
            Regex("""^(\d{1,2})[/-](\d{1,2})[/-](\d{2})$""")   // dd-mm-yy
        )
        for (pattern in regexPatterns) {
            val match = pattern.find(dateStr)
            if (match != null) {
                return try {
                    val g = match.groupValues
                    if (g[1].length == 4) {
                        LocalDate.of(g[1].toInt(), g[2].toInt(), g[3].toInt())
                    } else if (g[3].length == 4) {
                        LocalDate.of(g[3].toInt(), g[2].toInt(), g[1].toInt())
                    } else {
                        val year = 2000 + g[3].toInt()
                        LocalDate.of(year, g[2].toInt(), g[1].toInt())
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }
}

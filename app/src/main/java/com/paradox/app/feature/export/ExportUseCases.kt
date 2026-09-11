package com.paradox.app.feature.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.paradox.app.core.money.CurrencyFormatter
import com.paradox.app.domain.model.CashFlowSummary
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class ExportFormat {
    CSV,
    PDF
}

enum class ExportType {
    ALL,
    EXPENSES_ONLY,
    INCOME_ONLY
}

class ExportDataUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun generateCsv(
        profileId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        type: ExportType
    ): File {
        val categories = categoryRepository.getCategories(profileId).first().associateBy { it.id }
        val paymentMethods = paymentMethodRepository.getPaymentMethods(profileId).first().associateBy { it.id }

        val expenses = if (type != ExportType.INCOME_ONLY) {
            expenseRepository.getExpensesInRange(profileId, startDate, endDate).first()
        } else emptyList()

        val incomes = if (type != ExportType.EXPENSES_ONLY) {
            incomeRepository.getIncomesInRange(profileId, startDate, endDate).first()
        } else emptyList()

        val sb = StringBuilder()
        sb.append("Date,Type,Title/Source,Category,Payment Method,Amount,Currency,Notes\n")

        expenses.forEach { exp ->
            val catName = categories[exp.categoryId]?.name ?: "Uncategorized"
            val pmName = paymentMethods[exp.paymentMethodId]?.label ?: "Default"
            val notes = (exp.notes ?: "").replace("\"", "\"\"")
            sb.append("${exp.date},Expense,\"${exp.title.replace("\"", "\"\"")}\",\"$catName\",\"$pmName\",${exp.money.amount},${exp.money.currencyCode},\"$notes\"\n")
        }

        incomes.forEach { inc ->
            val notes = (inc.notes ?: "").replace("\"", "\"\"")
            sb.append("${inc.date},Income,\"${inc.source.displayName}\",Income,Cash/Bank,${inc.amount.amount},${inc.currency},\"$notes\"\n")
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "Paradox_Export_${startDate}_to_${endDate}.csv")
        file.writeText(sb.toString())
        return file
    }

    suspend fun generatePdf(
        profileId: String,
        profileName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        type: ExportType,
        summary: CashFlowSummary?
    ): File {
        val categories = categoryRepository.getCategories(profileId).first().associateBy { it.id }
        val paymentMethods = paymentMethodRepository.getPaymentMethods(profileId).first().associateBy { it.id }

        val expenses = if (type != ExportType.INCOME_ONLY) {
            expenseRepository.getExpensesInRange(profileId, startDate, endDate).first()
        } else emptyList()

        val incomes = if (type != ExportType.EXPENSES_ONLY) {
            incomeRepository.getIncomesInRange(profileId, startDate, endDate).first()
        } else emptyList()

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var y = 40f

        // Header Title
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 20f
        paint.color = Color.parseColor("#0F172A")
        canvas.drawText("PARADOX FINANCIAL STATEMENT", 40f, y, paint)
        y += 20f

        // Subtitle
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = Color.parseColor("#64748B")
        val dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy")
        canvas.drawText("Profile: $profileName | Range: ${startDate.format(dtf)} to ${endDate.format(dtf)}", 40f, y, paint)
        y += 30f

        // Summary box
        if (summary != null) {
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(40f, y, 555f, y + 60f, 8f, 8f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 11f

            val incText = "Total Income: ${CurrencyFormatter.format(summary.totalIncome)}"
            val expText = "Total Expenses: ${CurrencyFormatter.format(summary.totalExpense)}"
            val netText = "Net Flow: ${CurrencyFormatter.format(summary.netCashFlow)}"

            canvas.drawText(incText, 55f, y + 25f, paint)
            canvas.drawText(expText, 220f, y + 25f, paint)
            canvas.drawText(netText, 400f, y + 25f, paint)

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            paint.color = Color.parseColor("#64748B")
            canvas.drawText("Savings Rate: ${"%.1f".format(summary.savingsRatePct)}%", 55f, y + 45f, paint)
            y += 80f
        }

        // Table Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.parseColor("#334155")

        canvas.drawText("Date", 40f, y, paint)
        canvas.drawText("Type", 110f, y, paint)
        canvas.drawText("Title / Source", 170f, y, paint)
        canvas.drawText("Category", 320f, y, paint)
        canvas.drawText("Amount", 480f, y, paint)
        y += 10f

        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 18f

        // Table Rows
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f
        paint.color = Color.parseColor("#1E293B")

        val allItems = mutableListOf<ExportRow>()
        expenses.forEach { exp ->
            allItems.add(
                ExportRow(
                    date = exp.date,
                    type = "Expense",
                    title = exp.title,
                    category = categories[exp.categoryId]?.name ?: "General",
                    amount = "-${CurrencyFormatter.format(exp.money)}",
                    isIncome = false
                )
            )
        }
        incomes.forEach { inc ->
            allItems.add(
                ExportRow(
                    date = inc.date,
                    type = "Income",
                    title = inc.source.displayName,
                    category = "Income",
                    amount = "+${CurrencyFormatter.format(inc.amount)}",
                    isIncome = true
                )
            )
        }
        allItems.sortByDescending { it.date }

        var pageNumber = 1
        allItems.forEach { row ->
            if (y > 780f) {
                pdfDoc.finishPage(page)
                pageNumber++
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDoc.startPage(newPageInfo)
                canvas = page.canvas
                y = 40f
            }

            canvas.drawText(row.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 40f, y, paint)
            canvas.drawText(row.type, 110f, y, paint)
            canvas.drawText(row.title.take(22), 170f, y, paint)
            canvas.drawText(row.category.take(18), 320f, y, paint)

            paint.color = if (row.isIncome) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(row.amount, 480f, y, paint)

            paint.color = Color.parseColor("#1E293B")
            paint.typeface = Typeface.DEFAULT
            y += 16f
        }

        pdfDoc.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "Paradox_Statement_${startDate}_to_${endDate}.pdf")
        val out = FileOutputStream(file)
        pdfDoc.writeTo(out)
        out.close()
        pdfDoc.close()

        return file
    }

    private data class ExportRow(
        val date: LocalDate,
        val type: String,
        val title: String,
        val category: String,
        val amount: String,
        val isIncome: Boolean
    )
}

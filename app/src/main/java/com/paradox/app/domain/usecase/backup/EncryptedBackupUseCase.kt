package com.paradox.app.domain.usecase.backup

import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

class EncryptedBackupUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(profileId: String, passphrase: String): String {
        require(passphrase.length >= 6) { "Backup passphrase must be at least 6 characters" }

        val rootJson = JSONObject()
        rootJson.put("version", 1)
        rootJson.put("profileId", profileId)
        rootJson.put("exportedAt", Instant.now().toString())

        // 1. Categories
        val categories = categoryRepository.getCategories(profileId).first()
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isCustom", c.isCustom)
            obj.put("isDefault", c.isDefault)
            catArray.put(obj)
        }
        rootJson.put("categories", catArray)

        // 2. Expenses
        val expenses = expenseRepository.getAllExpenses(profileId).first()
        val expArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("title", e.title)
            obj.put("amount", e.money.amount.toPlainString())
            obj.put("currency", e.money.currencyCode)
            obj.put("categoryId", e.categoryId)
            obj.put("paymentMethodId", e.paymentMethodId)
            obj.put("date", e.date.toString())
            obj.put("notes", e.notes ?: "")
            obj.put("recurringFlag", e.recurringFlag)
            obj.put("source", e.source)
            expArray.put(obj)
        }
        rootJson.put("expenses", expArray)

        // 3. Incomes
        val incomes = incomeRepository.getAllIncomes(profileId).first()
        val incArray = JSONArray()
        for (i in incomes) {
            val obj = JSONObject()
            obj.put("id", i.id)
            obj.put("source", i.source.name)
            obj.put("amount", i.amount.amount.toPlainString())
            obj.put("currency", i.currency)
            obj.put("date", i.date.toString())
            obj.put("notes", i.notes ?: "")
            incArray.put(obj)
        }
        rootJson.put("incomes", incArray)

        // 4. Budgets
        val budgets = budgetRepository.getAllBudgets(profileId).first()
        val budArray = JSONArray()
        for (b in budgets) {
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("type", b.type.name)
            obj.put("amount", b.limit.amount.toPlainString())
            obj.put("currency", b.limit.currencyCode)
            obj.put("categoryId", b.categoryId ?: "")
            obj.put("thresholdPct", b.thresholdPct)
            budArray.put(obj)
        }
        rootJson.put("budgets", budArray)

        // 5. Accounts
        val accounts = accountRepository.getAccounts(profileId).first()
        val accArray = JSONArray()
        for (a in accounts) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("name", a.name)
            obj.put("type", a.type.name)
            obj.put("currency", a.currency)
            obj.put("initialBalance", a.initialBalance.amount.toPlainString())
            obj.put("colorHex", a.colorHex)
            obj.put("iconName", a.iconName)
            obj.put("isDefault", a.isDefault)
            accArray.put(obj)
        }
        rootJson.put("accounts", accArray)

        // 6. Savings Goals
        val goals = savingsGoalRepository.getAllGoals(profileId).first()
        val goalArray = JSONArray()
        for (g in goals) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("targetAmount", g.targetAmount.amount.toPlainString())
            obj.put("currentAmount", g.currentAmount.amount.toPlainString())
            obj.put("currency", g.currency)
            obj.put("targetDate", g.targetDate.toString())
            obj.put("colorHex", g.colorHex)
            obj.put("iconName", g.iconName)
            goalArray.put(obj)
        }
        rootJson.put("savingsGoals", goalArray)

        // Encrypt with AES-256-GCM via PBKDF2
        val plaintextBytes = rootJson.toString().toByteArray(StandardCharsets.UTF_8)
        val salt = ByteArray(16)
        val iv = ByteArray(12)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 12000, 256)
        val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintextBytes)

        val resultEnvelope = JSONObject()
        resultEnvelope.put("paradoxBackup", true)
        resultEnvelope.put("salt", Base64.getEncoder().encodeToString(salt))
        resultEnvelope.put("iv", Base64.getEncoder().encodeToString(iv))
        resultEnvelope.put("payload", Base64.getEncoder().encodeToString(ciphertext))

        return resultEnvelope.toString(2)
    }
}

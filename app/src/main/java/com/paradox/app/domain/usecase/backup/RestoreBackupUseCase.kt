package com.paradox.app.domain.usecase.backup

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.model.SavingsGoal
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import org.json.JSONObject
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

data class RestoreResult(
    val success: Boolean,
    val itemsRestoredCount: Int,
    val errorMessage: String? = null
)

class RestoreBackupUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(profileId: String, encryptedEnvelopeJson: String, passphrase: String): RestoreResult {
        return try {
            val envelope = JSONObject(encryptedEnvelopeJson)
            if (!envelope.optBoolean("paradoxBackup", false)) {
                return RestoreResult(false, 0, "Invalid Paradox backup format")
            }

            val salt = Base64.getDecoder().decode(envelope.getString("salt"))
            val iv = Base64.getDecoder().decode(envelope.getString("iv"))
            val ciphertext = Base64.getDecoder().decode(envelope.getString("payload"))

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, 12000, 256)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val plaintextBytes = cipher.doFinal(ciphertext)
            val rootJson = JSONObject(String(plaintextBytes, StandardCharsets.UTF_8))

            var count = 0

            // Restore Categories
            val catArray = rootJson.optJSONArray("categories")
            if (catArray != null) {
                for (i in 0 until catArray.length()) {
                    val cObj = catArray.getJSONObject(i)
                    categoryRepository.addCategory(
                        Category(
                            id = cObj.getString("id"),
                            profileId = profileId,
                            name = cObj.getString("name"),
                            iconName = cObj.getString("iconName"),
                            colorHex = cObj.getString("colorHex"),
                            isCustom = cObj.optBoolean("isCustom", true),
                            isDefault = cObj.optBoolean("isDefault", false)
                        )
                    )
                    count++
                }
            }

            // Restore Accounts
            val accArray = rootJson.optJSONArray("accounts")
            if (accArray != null) {
                for (i in 0 until accArray.length()) {
                    val aObj = accArray.getJSONObject(i)
                    accountRepository.addAccount(
                        Account(
                            id = aObj.getString("id"),
                            profileId = profileId,
                            name = aObj.getString("name"),
                            type = AccountType.valueOf(aObj.getString("type")),
                            currency = aObj.optString("currency", "INR"),
                            initialBalance = Money.of(BigDecimal(aObj.getString("initialBalance")), aObj.optString("currency", "INR")),
                            colorHex = aObj.optString("colorHex", "#3B82F6"),
                            iconName = aObj.optString("iconName", "account_balance"),
                            isDefault = aObj.optBoolean("isDefault", false)
                        )
                    )
                    count++
                }
            }

            // Restore Expenses
            val expArray = rootJson.optJSONArray("expenses")
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val eObj = expArray.getJSONObject(i)
                    expenseRepository.addExpense(
                        Expense(
                            id = eObj.getString("id"),
                            profileId = profileId,
                            title = eObj.getString("title"),
                            money = Money.of(BigDecimal(eObj.getString("amount")), eObj.optString("currency", "INR")),
                            categoryId = eObj.getString("categoryId"),
                            paymentMethodId = eObj.getString("paymentMethodId"),
                            date = LocalDate.parse(eObj.getString("date")),
                            notes = eObj.optString("notes").ifEmpty { null },
                            recurringFlag = eObj.optBoolean("recurringFlag", false),
                            source = eObj.optString("source", "MANUAL"),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now()
                        )
                    )
                    count++
                }
            }

            // Restore Incomes
            val incArray = rootJson.optJSONArray("incomes")
            if (incArray != null) {
                for (i in 0 until incArray.length()) {
                    val iObj = incArray.getJSONObject(i)
                    incomeRepository.addIncome(
                        Income(
                            id = iObj.getString("id"),
                            profileId = profileId,
                            source = IncomeSource.valueOf(iObj.getString("source")),
                            amount = Money.of(BigDecimal(iObj.getString("amount")), iObj.optString("currency", "INR")),
                            currency = iObj.optString("currency", "INR"),
                            date = LocalDate.parse(iObj.getString("date")),
                            notes = iObj.optString("notes").ifEmpty { null },
                            createdAt = Instant.now(),
                            updatedAt = Instant.now()
                        )
                    )
                    count++
                }
            }

            // Restore Budgets
            val budArray = rootJson.optJSONArray("budgets")
            if (budArray != null) {
                for (i in 0 until budArray.length()) {
                    val bObj = budArray.getJSONObject(i)
                    budgetRepository.setBudget(
                        Budget(
                            id = bObj.getString("id"),
                            profileId = profileId,
                            type = BudgetType.valueOf(bObj.getString("type")),
                            limit = Money.of(BigDecimal(bObj.getString("amount")), bObj.optString("currency", "INR")),
                            categoryId = bObj.optString("categoryId").ifEmpty { null },
                            thresholdPct = bObj.optInt("thresholdPct", 80)
                        )
                    )
                    count++
                }
            }

            // Restore Savings Goals
            val goalArray = rootJson.optJSONArray("savingsGoals")
            if (goalArray != null) {
                for (i in 0 until goalArray.length()) {
                    val gObj = goalArray.getJSONObject(i)
                    savingsGoalRepository.addGoal(
                        SavingsGoal(
                            id = gObj.getString("id"),
                            profileId = profileId,
                            name = gObj.getString("name"),
                            targetAmount = Money.of(BigDecimal(gObj.getString("targetAmount")), gObj.optString("currency", "INR")),
                            currentAmount = Money.of(BigDecimal(gObj.getString("currentAmount")), gObj.optString("currency", "INR")),
                            currency = gObj.optString("currency", "INR"),
                            targetDate = LocalDate.parse(gObj.getString("targetDate")),
                            colorHex = gObj.optString("colorHex", "#10B981"),
                            iconName = gObj.optString("iconName", "savings")
                        )
                    )
                    count++
                }
            }

            RestoreResult(true, count)
        } catch (e: Exception) {
            RestoreResult(false, 0, "Decryption or restore failed: ${e.localizedMessage}")
        }
    }
}

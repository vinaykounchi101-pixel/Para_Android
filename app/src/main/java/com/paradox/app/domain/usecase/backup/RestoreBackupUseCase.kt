package com.paradox.app.domain.usecase.backup

import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.model.DebtStatus
import com.paradox.app.domain.model.DebtType
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.model.PaymentMethodType
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.model.SavingsGoal
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.DebtRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import com.paradox.app.domain.repository.ProfileRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.first
import org.json.JSONException
import org.json.JSONObject
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
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
    private val profileRepository: ProfileRepository,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val debtRepository: DebtRepository,
    private val sessionDataStore: SessionDataStore
) {
    suspend operator fun invoke(profileId: String, encryptedEnvelopeJson: String, passphrase: String): RestoreResult {
        val cleanPassphrase = passphrase.trim()
        if (cleanPassphrase.length < 6) {
            return RestoreResult(false, 0, "Passphrase must be at least 6 characters")
        }

        return try {
            val envelope = JSONObject(encryptedEnvelopeJson.trim())
            if (!envelope.optBoolean("paradoxBackup", false)) {
                return RestoreResult(false, 0, "Invalid Paradox backup format. Please ensure you selected a valid .paradoxvault file.")
            }

            val salt = Base64.getDecoder().decode(envelope.getString("salt"))
            val iv = Base64.getDecoder().decode(envelope.getString("iv"))
            val ciphertext = Base64.getDecoder().decode(envelope.getString("payload"))

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(cleanPassphrase.toCharArray(), salt, 12000, 256)
            val secretKey = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            val plaintextBytes = cipher.doFinal(ciphertext)
            val rootJson = JSONObject(String(plaintextBytes, StandardCharsets.UTF_8))

            // 1. Resolve active profile ID safely to guarantee ProfileEntity exists in SQLite
            val allProfiles = profileRepository.getAllProfiles().first()
            val resolvedProfile = when {
                profileId.isNotBlank() && profileRepository.getProfileById(profileId) != null -> {
                    profileRepository.getProfileById(profileId)!!
                }
                allProfiles.isNotEmpty() -> {
                    allProfiles.first()
                }
                else -> {
                    profileRepository.createProfile("My Vault", AuthType.PIN, "1234", false)
                }
            }
            val targetProfileId = resolvedProfile.id
            sessionDataStore.setActiveProfileId(targetProfileId)

            var count = 0
            val validCategoryIds = mutableSetOf<String>()
            val validPaymentMethodIds = mutableSetOf<String>()

            // Load existing categories and payment methods
            try {
                categoryRepository.getCategories(targetProfileId).first().forEach { validCategoryIds.add(it.id) }
            } catch (_: Exception) {}
            try {
                paymentMethodRepository.getPaymentMethods(targetProfileId).first().forEach { validPaymentMethodIds.add(it.id) }
            } catch (_: Exception) {}

            // 2. Restore Categories
            val catArray = rootJson.optJSONArray("categories")
            if (catArray != null && catArray.length() > 0) {
                for (i in 0 until catArray.length()) {
                    val cObj = catArray.getJSONObject(i)
                    val catId = cObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                    categoryRepository.addCategory(
                        Category(
                            id = catId,
                            profileId = targetProfileId,
                            name = cObj.optString("name", "Category"),
                            iconName = cObj.optString("iconName", "category"),
                            colorHex = cObj.optString("colorHex", "#64748B"),
                            isCustom = cObj.optBoolean("isCustom", true),
                            isDefault = cObj.optBoolean("isDefault", false)
                        )
                    )
                    validCategoryIds.add(catId)
                    count++
                }
            }
            if (validCategoryIds.isEmpty()) {
                Category.createStarterCategories(targetProfileId).forEach {
                    categoryRepository.addCategory(it)
                    validCategoryIds.add(it.id)
                }
            }

            // 3. Restore Payment Methods
            val pmArray = rootJson.optJSONArray("paymentMethods")
            if (pmArray != null && pmArray.length() > 0) {
                for (i in 0 until pmArray.length()) {
                    val pmObj = pmArray.getJSONObject(i)
                    val pmId = pmObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                    val typeStr = pmObj.optString("type", "CASH")
                    val type = try {
                        PaymentMethodType.valueOf(typeStr)
                    } catch (_: Exception) {
                        PaymentMethodType.CASH
                    }
                    paymentMethodRepository.addPaymentMethod(
                        PaymentMethod(
                            id = pmId,
                            profileId = targetProfileId,
                            type = type,
                            label = pmObj.optString("label", "Payment Method"),
                            isCustom = pmObj.optBoolean("isCustom", false)
                        )
                    )
                    validPaymentMethodIds.add(pmId)
                    count++
                }
            }
            if (validPaymentMethodIds.isEmpty()) {
                PaymentMethod.createStarterPaymentMethods(targetProfileId).forEach {
                    paymentMethodRepository.addPaymentMethod(it)
                    validPaymentMethodIds.add(it.id)
                }
            }

            // 4. Restore Accounts
            val accArray = rootJson.optJSONArray("accounts")
            if (accArray != null) {
                for (i in 0 until accArray.length()) {
                    val aObj = accArray.getJSONObject(i)
                    val accId = aObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                    val typeStr = aObj.optString("type", "BANK")
                    val type = try {
                        AccountType.valueOf(typeStr)
                    } catch (_: Exception) {
                        AccountType.BANK
                    }
                    val currency = aObj.optString("currency", "INR")
                    val initBal = try {
                        BigDecimal(aObj.optString("initialBalance", "0"))
                    } catch (_: Exception) {
                        BigDecimal.ZERO
                    }
                    accountRepository.addAccount(
                        Account(
                            id = accId,
                            profileId = targetProfileId,
                            name = aObj.optString("name", "Account"),
                            type = type,
                            currency = currency,
                            initialBalance = Money.of(initBal, currency),
                            colorHex = aObj.optString("colorHex", "#3B82F6"),
                            iconName = aObj.optString("iconName", type.defaultIcon),
                            isDefault = aObj.optBoolean("isDefault", false)
                        )
                    )
                    count++
                }
            }

            // 5. Restore Budgets
            val budArray = rootJson.optJSONArray("budgets")
            if (budArray != null) {
                for (i in 0 until budArray.length()) {
                    val bObj = budArray.getJSONObject(i)
                    val budId = bObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                    val rawCatId = bObj.optString("categoryId", "").ifBlank { null }
                    val safeCatId = if (rawCatId != null && validCategoryIds.contains(rawCatId)) rawCatId else null

                    val limitAmount = try {
                        BigDecimal(bObj.optString("amount", "0"))
                    } catch (_: Exception) {
                        BigDecimal.ZERO
                    }
                    val currency = bObj.optString("currency", "INR")

                    budgetRepository.setBudget(
                        Budget(
                            id = budId,
                            profileId = targetProfileId,
                            type = try { BudgetType.valueOf(bObj.optString("type", "MONTHLY")) } catch (_: Exception) { BudgetType.MONTHLY },
                            limit = Money.of(limitAmount, currency),
                            categoryId = safeCatId,
                            thresholdPct = bObj.optInt("thresholdPct", 80)
                        )
                    )
                    count++
                }
            }

            // 6. Restore Recurring Expenses
            val recArray = rootJson.optJSONArray("recurringExpenses")
            if (recArray != null) {
                for (i in 0 until recArray.length()) {
                    val rObj = recArray.getJSONObject(i)
                    val recId = rObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }

                    var catId = rObj.optString("categoryId", "").ifBlank { null }
                    if (catId == null || !validCategoryIds.contains(catId)) {
                        val fallbackId = catId ?: "${targetProfileId}_cat_gen_${UUID.randomUUID().toString().take(6)}"
                        categoryRepository.addCategory(
                            Category(
                                id = fallbackId,
                                profileId = targetProfileId,
                                name = "General",
                                iconName = "category",
                                colorHex = "#64748B",
                                isCustom = true
                            )
                        )
                        validCategoryIds.add(fallbackId)
                        catId = fallbackId
                    }

                    var pmId = rObj.optString("paymentMethodId", "").ifBlank { null }
                    if (pmId == null || !validPaymentMethodIds.contains(pmId)) {
                        val fallbackPmId = pmId ?: "${targetProfileId}_pm_gen_${UUID.randomUUID().toString().take(6)}"
                        paymentMethodRepository.addPaymentMethod(
                            PaymentMethod(
                                id = fallbackPmId,
                                profileId = targetProfileId,
                                type = PaymentMethodType.CASH,
                                label = "Cash",
                                isCustom = true
                            )
                        )
                        validPaymentMethodIds.add(fallbackPmId)
                        pmId = fallbackPmId
                    }

                    val freq = try {
                        RecurringFrequency.valueOf(rObj.optString("frequency", "MONTHLY"))
                    } catch (_: Exception) {
                        RecurringFrequency.MONTHLY
                    }
                    val amount = try {
                        BigDecimal(rObj.optString("amount", "0"))
                    } catch (_: Exception) {
                        BigDecimal.ZERO
                    }
                    val currency = rObj.optString("currency", "INR")
                    val startDate = try { LocalDate.parse(rObj.getString("startDate")) } catch (_: Exception) { LocalDate.now() }
                    val nextDueDate = try { LocalDate.parse(rObj.getString("nextDueDate")) } catch (_: Exception) { LocalDate.now() }

                    recurringRepository.addRecurring(
                        RecurringExpense(
                            id = recId,
                            profileId = targetProfileId,
                            title = rObj.optString("title", "Subscription"),
                            amount = Money.of(amount, currency),
                            currency = currency,
                            categoryId = catId,
                            paymentMethodId = pmId,
                            frequency = freq,
                            startDate = startDate,
                            nextDueDate = nextDueDate,
                            isActive = rObj.optBoolean("isActive", true),
                            notes = rObj.optString("notes").ifBlank { null }
                        )
                    )
                    count++
                }
            }

            // 7. Restore Savings Goals
            val goalArray = rootJson.optJSONArray("savingsGoals")
            if (goalArray != null) {
                for (i in 0 until goalArray.length()) {
                    val gObj = goalArray.getJSONObject(i)
                    val goalId = gObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                    val targetAmount = try { BigDecimal(gObj.optString("targetAmount", "0")) } catch (_: Exception) { BigDecimal.ZERO }
                    val currentAmount = try { BigDecimal(gObj.optString("currentAmount", "0")) } catch (_: Exception) { BigDecimal.ZERO }
                    val currency = gObj.optString("currency", "INR")
                    val targetDate = try { LocalDate.parse(gObj.getString("targetDate")) } catch (_: Exception) { LocalDate.now().plusMonths(6) }

                    savingsGoalRepository.addGoal(
                        SavingsGoal(
                            id = goalId,
                            profileId = targetProfileId,
                            name = gObj.optString("name", "Savings Goal"),
                            targetAmount = Money.of(targetAmount, currency),
                            currentAmount = Money.of(currentAmount, currency),
                            currency = currency,
                            targetDate = targetDate,
                            colorHex = gObj.optString("colorHex", "#10B981"),
                            iconName = gObj.optString("iconName", "savings")
                        )
                    )
                    count++
                }
            }

            // 8. Restore Expenses
            val expArray = rootJson.optJSONArray("expenses")
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val eObj = expArray.getJSONObject(i)
                    val expId = eObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }

                    var catId = eObj.optString("categoryId", "").ifBlank { null }
                    if (catId == null || !validCategoryIds.contains(catId)) {
                        val fallbackId = catId ?: "${targetProfileId}_cat_gen_${UUID.randomUUID().toString().take(6)}"
                        categoryRepository.addCategory(
                            Category(
                                id = fallbackId,
                                profileId = targetProfileId,
                                name = "General",
                                iconName = "category",
                                colorHex = "#64748B",
                                isCustom = true
                            )
                        )
                        validCategoryIds.add(fallbackId)
                        catId = fallbackId
                    }

                    var pmId = eObj.optString("paymentMethodId", "").ifBlank { null }
                    if (pmId == null || !validPaymentMethodIds.contains(pmId)) {
                        val fallbackPmId = pmId ?: "${targetProfileId}_pm_gen_${UUID.randomUUID().toString().take(6)}"
                        paymentMethodRepository.addPaymentMethod(
                            PaymentMethod(
                                id = fallbackPmId,
                                profileId = targetProfileId,
                                type = PaymentMethodType.CASH,
                                label = "Cash",
                                isCustom = true
                            )
                        )
                        validPaymentMethodIds.add(fallbackPmId)
                        pmId = fallbackPmId
                    }

                    val amount = try { BigDecimal(eObj.optString("amount", "0")) } catch (_: Exception) { BigDecimal.ZERO }
                    val currency = eObj.optString("currency", "INR")
                    val date = try { LocalDate.parse(eObj.getString("date")) } catch (_: Exception) { LocalDate.now() }

                    expenseRepository.addExpense(
                        Expense(
                            id = expId,
                            profileId = targetProfileId,
                            title = eObj.optString("title", "Expense"),
                            money = Money.of(amount, currency),
                            categoryId = catId,
                            paymentMethodId = pmId,
                            date = date,
                            notes = eObj.optString("notes").ifBlank { null },
                            recurringFlag = eObj.optBoolean("recurringFlag", false),
                            source = eObj.optString("source", "MANUAL"),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now()
                        )
                    )
                    count++
                }
            }

            // 9. Restore Incomes
            val incArray = rootJson.optJSONArray("incomes")
            if (incArray != null) {
                for (i in 0 until incArray.length()) {
                    val iObj = incArray.getJSONObject(i)
                    val incId = iObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                    val srcStr = iObj.optString("source", "OTHER")
                    val source = try {
                        IncomeSource.valueOf(srcStr)
                    } catch (_: Exception) {
                        IncomeSource.OTHER
                    }
                    val amount = try { BigDecimal(iObj.optString("amount", "0")) } catch (_: Exception) { BigDecimal.ZERO }
                    val currency = iObj.optString("currency", "INR")
                    val date = try { LocalDate.parse(iObj.getString("date")) } catch (_: Exception) { LocalDate.now() }

                    incomeRepository.addIncome(
                        Income(
                            id = incId,
                            profileId = targetProfileId,
                            source = source,
                            amount = Money.of(amount, currency),
                            currency = currency,
                            date = date,
                            notes = iObj.optString("notes").ifBlank { null },
                            createdAt = Instant.now(),
                            updatedAt = Instant.now()
                        )
                    )
                    count++
                }
            }

            // 10. Restore Debts & Repayments
            val debtArray = rootJson.optJSONArray("debts")
            if (debtArray != null) {
                for (i in 0 until debtArray.length()) {
                    val dObj = debtArray.getJSONObject(i)
                    val debtId = dObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                    val initAmt = try { BigDecimal(dObj.optString("initialAmount", "0")) } catch (_: Exception) { BigDecimal.ZERO }
                    val remAmt = try { BigDecimal(dObj.optString("remainingAmount", "0")) } catch (_: Exception) { initAmt }
                    val currency = dObj.optString("currency", "INR")
                    val typeStr = dObj.optString("debtType", "LENT")
                    val debtType = try { DebtType.valueOf(typeStr) } catch (_: Exception) { DebtType.LENT }
                    val statusStr = dObj.optString("status", "ACTIVE")
                    val status = try { DebtStatus.valueOf(statusStr) } catch (_: Exception) { DebtStatus.ACTIVE }
                    val dueDate = dObj.optString("dueDate").takeIf { it.isNotBlank() }?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } }

                    debtRepository.insertDebt(
                        Debt(
                            id = debtId,
                            profileId = targetProfileId,
                            personName = dObj.optString("personName", "Contact"),
                            personContactNumber = dObj.optString("personContactNumber").takeIf { it.isNotBlank() },
                            debtType = debtType,
                            initialAmount = Money.of(initAmt, currency),
                            remainingAmount = Money.of(remAmt, currency),
                            dueDate = dueDate,
                            notes = dObj.optString("notes").takeIf { it.isNotBlank() },
                            status = status,
                            reminderEnabled = dObj.optBoolean("reminderEnabled", false),
                            createdAt = Instant.now(),
                            updatedAt = Instant.now()
                        )
                    )
                    count++

                    // Restore Repayments for this debt
                    val repArray = dObj.optJSONArray("repayments")
                    if (repArray != null) {
                        for (j in 0 until repArray.length()) {
                            val repObj = repArray.getJSONObject(j)
                            val repId = repObj.optString("id", UUID.randomUUID().toString()).ifBlank { UUID.randomUUID().toString() }
                            val repAmount = try { BigDecimal(repObj.optString("amount", "0")) } catch (_: Exception) { BigDecimal.ZERO }
                            val repCurrency = repObj.optString("currency", "INR")
                            val repDate = try { LocalDate.parse(repObj.getString("repaymentDate")) } catch (_: Exception) { LocalDate.now() }

                            debtRepository.recordRepayment(
                                DebtRepayment(
                                    id = repId,
                                    debtId = debtId,
                                    profileId = targetProfileId,
                                    amount = Money.of(repAmount, repCurrency),
                                    repaymentDate = repDate,
                                    notes = repObj.optString("notes").takeIf { it.isNotBlank() },
                                    createdAt = Instant.now()
                                )
                            )
                        }
                    }
                }
            }

            RestoreResult(true, count)
        } catch (e: AEADBadTagException) {
            RestoreResult(false, 0, "Incorrect passphrase. The entered passphrase does not match the encryption key for this backup file.")
        } catch (e: BadPaddingException) {
            RestoreResult(false, 0, "Incorrect passphrase. The entered passphrase does not match the encryption key for this backup file.")
        } catch (e: JSONException) {
            RestoreResult(false, 0, "Invalid or corrupted backup JSON envelope.")
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: ""
            if (msg.contains("BAD_DECRYPT", ignoreCase = true) || msg.contains("tag", ignoreCase = true) || msg.contains("mac check in GCM failed", ignoreCase = true)) {
                RestoreResult(false, 0, "Incorrect passphrase. The entered passphrase does not match the encryption key for this backup file.")
            } else {
                RestoreResult(false, 0, "Decryption or restore failed: $msg")
            }
        }
    }
}

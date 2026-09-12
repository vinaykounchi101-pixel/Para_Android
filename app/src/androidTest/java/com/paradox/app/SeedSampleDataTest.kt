package com.paradox.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.paradox.app.core.common.Constants
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.security.KeystoreManager
import com.paradox.app.core.security.PassphraseManager
import com.paradox.app.data.local.entity.*
import com.paradox.app.di.DatabaseModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SeedSampleDataTest {

    @Test
    fun seedSampleFinancialRecords() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keystoreManager = KeystoreManager()
        val passphraseManager = PassphraseManager(context, keystoreManager)
        val db = DatabaseModule.provideParadoxDatabase(context, passphraseManager)

        val sessionDataStore = SessionDataStore(context)
        val activeProfileIdFromStore = sessionDataStore.activeProfileId.firstOrNull()

        val existingProfiles = db.profileDao().getAllProfiles().first()
        val targetProfileId = activeProfileIdFromStore
            ?: existingProfiles.firstOrNull()?.id
            ?: "profile_default_seed".also { newId ->
                val newProfile = ProfileEntity(
                    id = newId,
                    name = "Vinay",
                    primaryAuthType = "PIN",
                    credentialHash = "default_seed_hash",
                    biometricEnabled = false
                )
                db.profileDao().insertProfile(newProfile)
                sessionDataStore.setActiveProfileId(newId)
            }

        assertNotNull("Target profile must not be null", targetProfileId)

        // 1. Seed Categories
        val categories = listOf(
            CategoryEntity(
                id = "cat_food_$targetProfileId",
                profileId = targetProfileId,
                name = "Food & Dining",
                iconName = "restaurant",
                colorHex = "#FF5722",
                isCustom = false,
                isDefault = true
            ),
            CategoryEntity(
                id = "cat_groceries_$targetProfileId",
                profileId = targetProfileId,
                name = "Groceries",
                iconName = "shopping_cart",
                colorHex = "#4CAF50",
                isCustom = false,
                isDefault = false
            ),
            CategoryEntity(
                id = "cat_shopping_$targetProfileId",
                profileId = targetProfileId,
                name = "Shopping",
                iconName = "shopping_bag",
                colorHex = "#9C27B0",
                isCustom = false,
                isDefault = false
            ),
            CategoryEntity(
                id = "cat_utilities_$targetProfileId",
                profileId = targetProfileId,
                name = "Utilities & Bills",
                iconName = "receipt_long",
                colorHex = "#FF9800",
                isCustom = false,
                isDefault = false
            ),
            CategoryEntity(
                id = "cat_entertainment_$targetProfileId",
                profileId = targetProfileId,
                name = "Entertainment",
                iconName = "movie",
                colorHex = "#E91E63",
                isCustom = false,
                isDefault = false
            ),
            CategoryEntity(
                id = "cat_salary_$targetProfileId",
                profileId = targetProfileId,
                name = "Salary & Paycheck",
                iconName = "work",
                colorHex = "#009688",
                isCustom = false,
                isDefault = true
            ),
            CategoryEntity(
                id = "cat_freelance_$targetProfileId",
                profileId = targetProfileId,
                name = "Freelance & Consulting",
                iconName = "laptop",
                colorHex = "#2196F3",
                isCustom = false,
                isDefault = false
            )
        )
        categories.forEach {
            try {
                db.categoryDao().insertCategory(it)
            } catch (_: Exception) {}
        }

        // 2. Seed Payment Methods
        val paymentMethods = listOf(
            PaymentMethodEntity(
                id = "pm_upi_$targetProfileId",
                profileId = targetProfileId,
                type = "UPI",
                label = "UPI - GPay",
                isCustom = false
            ),
            PaymentMethodEntity(
                id = "pm_hdfc_card_$targetProfileId",
                profileId = targetProfileId,
                type = "CREDIT_CARD",
                label = "HDFC Regalia Credit Card",
                isCustom = false
            ),
            PaymentMethodEntity(
                id = "pm_cash_$targetProfileId",
                profileId = targetProfileId,
                type = "CASH",
                label = "Cash in Wallet",
                isCustom = false
            )
        )
        paymentMethods.forEach {
            try {
                db.paymentMethodDao().insertPaymentMethod(it)
            } catch (_: Exception) {}
        }

        // 3. Seed Accounts
        val accounts = listOf(
            AccountEntity(
                id = "acc_primary_$targetProfileId",
                profileId = targetProfileId,
                name = "HDFC Salary Account",
                type = "BANK_ACCOUNT",
                currency = Constants.DEFAULT_CURRENCY,
                initialBalance = BigDecimal("142500.00"),
                colorHex = "#0D47A1",
                iconName = "account_balance",
                isDefault = true
            ),
            AccountEntity(
                id = "acc_savings_$targetProfileId",
                profileId = targetProfileId,
                name = "High-Yield Savings Vault",
                type = "BANK_ACCOUNT",
                currency = Constants.DEFAULT_CURRENCY,
                initialBalance = BigDecimal("350000.00"),
                colorHex = "#10B981",
                iconName = "savings",
                isDefault = false
            )
        )
        accounts.forEach {
            try {
                db.accountDao().insertAccount(it)
            } catch (_: Exception) {}
        }

        // 4. Seed Budgets
        val budgets = listOf(
            BudgetEntity(
                id = "bgt_monthly_$targetProfileId",
                profileId = targetProfileId,
                type = "MONTHLY",
                amount = BigDecimal("35000.00"),
                thresholdPct = 80
            ),
            BudgetEntity(
                id = "bgt_food_$targetProfileId",
                profileId = targetProfileId,
                type = "CATEGORY",
                amount = BigDecimal("12000.00"),
                categoryId = "cat_food_$targetProfileId",
                thresholdPct = 80
            ),
            BudgetEntity(
                id = "bgt_shopping_$targetProfileId",
                profileId = targetProfileId,
                type = "CATEGORY",
                amount = BigDecimal("8000.00"),
                categoryId = "cat_shopping_$targetProfileId",
                thresholdPct = 75
            )
        )
        budgets.forEach {
            try {
                db.budgetDao().insertBudget(it)
            } catch (_: Exception) {}
        }

        // 5. Seed Incomes
        val incomes = listOf(
            IncomeEntity(
                id = "inc_salary_${UUID.randomUUID()}",
                profileId = targetProfileId,
                source = "Monthly Tech Salary",
                amount = BigDecimal("125000.00"),
                currency = Constants.DEFAULT_CURRENCY,
                date = LocalDate.now().withDayOfMonth(1),
                notes = "September Salary credited"
            ),
            IncomeEntity(
                id = "inc_freelance_${UUID.randomUUID()}",
                profileId = targetProfileId,
                source = "Mobile App Design Consulting",
                amount = BigDecimal("28500.00"),
                currency = Constants.DEFAULT_CURRENCY,
                date = LocalDate.now().minusDays(5),
                notes = "Client Milestone 1 Payout"
            )
        )
        incomes.forEach {
            try {
                db.incomeDao().insertIncome(it)
            } catch (_: Exception) {}
        }

        // 6. Seed Expenses
        val expenses = listOf(
            ExpenseEntity(
                id = "exp_${UUID.randomUUID()}",
                profileId = targetProfileId,
                title = "Gourmet Dinner & Swiggy",
                amount = BigDecimal("1280.00"),
                currency = Constants.DEFAULT_CURRENCY,
                categoryId = "cat_food_$targetProfileId",
                paymentMethodId = "pm_upi_$targetProfileId",
                date = LocalDate.now().minusDays(1),
                notes = "Dinner with friends"
            ),
            ExpenseEntity(
                id = "exp_${UUID.randomUUID()}",
                profileId = targetProfileId,
                title = "Weekly Organic Groceries",
                amount = BigDecimal("3450.00"),
                currency = Constants.DEFAULT_CURRENCY,
                categoryId = "cat_groceries_$targetProfileId",
                paymentMethodId = "pm_hdfc_card_$targetProfileId",
                date = LocalDate.now().minusDays(3),
                notes = "Supermarket restock"
            ),
            ExpenseEntity(
                id = "exp_${UUID.randomUUID()}",
                profileId = targetProfileId,
                title = "Electricity & Internet Bill",
                amount = BigDecimal("2499.00"),
                currency = Constants.DEFAULT_CURRENCY,
                categoryId = "cat_utilities_$targetProfileId",
                paymentMethodId = "pm_upi_$targetProfileId",
                date = LocalDate.now().minusDays(6),
                notes = "Airtel Fiber & BESCOM Bill"
            ),
            ExpenseEntity(
                id = "exp_${UUID.randomUUID()}",
                profileId = targetProfileId,
                title = "IMAX Movie Night",
                amount = BigDecimal("950.00"),
                currency = Constants.DEFAULT_CURRENCY,
                categoryId = "cat_entertainment_$targetProfileId",
                paymentMethodId = "pm_upi_$targetProfileId",
                date = LocalDate.now().minusDays(8),
                notes = "Weekend cinema tickets"
            ),
            ExpenseEntity(
                id = "exp_${UUID.randomUUID()}",
                profileId = targetProfileId,
                title = "Noise Cancelling Headphones",
                amount = BigDecimal("4999.00"),
                currency = Constants.DEFAULT_CURRENCY,
                categoryId = "cat_shopping_$targetProfileId",
                paymentMethodId = "pm_hdfc_card_$targetProfileId",
                date = LocalDate.now().minusDays(10),
                notes = "Work setup upgrade"
            ),
            ExpenseEntity(
                id = "exp_${UUID.randomUUID()}",
                profileId = targetProfileId,
                title = "Artisan Espresso & Bakery",
                amount = BigDecimal("450.00"),
                currency = Constants.DEFAULT_CURRENCY,
                categoryId = "cat_food_$targetProfileId",
                paymentMethodId = "pm_cash_$targetProfileId",
                date = LocalDate.now(),
                notes = "Morning coffee"
            )
        )
        expenses.forEach {
            try {
                db.expenseDao().insertExpense(it)
            } catch (_: Exception) {}
        }

        // 7. Seed Savings Goals
        val savingsGoals = listOf(
            SavingsGoalEntity(
                id = "goal_macbook_$targetProfileId",
                profileId = targetProfileId,
                name = "MacBook Pro M3 Max",
                targetAmount = BigDecimal("240000.00"),
                currentAmount = BigDecimal("95000.00"),
                targetDate = LocalDate.now().plusMonths(6),
                iconName = "laptop_mac",
                colorHex = "#3F51B5"
            ),
            SavingsGoalEntity(
                id = "goal_travel_$targetProfileId",
                profileId = targetProfileId,
                name = "Japan Autumn Tour",
                targetAmount = BigDecimal("150000.00"),
                currentAmount = BigDecimal("60000.00"),
                targetDate = LocalDate.now().plusMonths(9),
                iconName = "flight_takeoff",
                colorHex = "#00BCD4"
            )
        )
        savingsGoals.forEach {
            try {
                db.savingsGoalDao().insertGoal(it)
            } catch (_: Exception) {}
        }

        // 8. Seed Recurring Expenses
        val recurringExpenses = listOf(
            RecurringExpenseEntity(
                id = "rec_netflix_$targetProfileId",
                profileId = targetProfileId,
                title = "Netflix 4K Ultra Subscription",
                amount = BigDecimal("649.00"),
                currency = Constants.DEFAULT_CURRENCY,
                categoryId = "cat_entertainment_$targetProfileId",
                paymentMethodId = "pm_hdfc_card_$targetProfileId",
                frequency = "MONTHLY",
                startDate = LocalDate.now().withDayOfMonth(1),
                nextDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1),
                isActive = true,
                notes = "Monthly family subscription"
            )
        )
        recurringExpenses.forEach {
            try {
                db.recurringExpenseDao().insertRecurring(it)
            } catch (_: Exception) {}
        }

        // Verify counts
        val finalExpenses = db.expenseDao().getAllExpenses(targetProfileId).first()
        val finalIncomes = db.incomeDao().getAllIncomes(targetProfileId).first()
        assertTrue("Expenses must be seeded", finalExpenses.isNotEmpty())
        assertTrue("Incomes must be seeded", finalIncomes.isNotEmpty())
    }
}

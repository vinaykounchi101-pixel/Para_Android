package com.paradox.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.paradox.app.data.local.dao.AccountDao
import com.paradox.app.data.local.dao.AiInsightLogDao
import com.paradox.app.data.local.dao.BudgetDao
import com.paradox.app.data.local.dao.CategoryDao
import com.paradox.app.data.local.dao.DebtDao
import com.paradox.app.data.local.dao.DebtRepaymentDao
import com.paradox.app.data.local.dao.ExpenseDao
import com.paradox.app.data.local.dao.IncomeDao
import com.paradox.app.data.local.dao.PaymentMethodDao
import com.paradox.app.data.local.dao.ProfileDao
import com.paradox.app.data.local.dao.RecurringExpenseDao
import com.paradox.app.data.local.dao.SavingsGoalDao
import com.paradox.app.data.local.dao.SyncQueueDao
import com.paradox.app.data.local.entity.AccountEntity
import com.paradox.app.data.local.entity.AiInsightLogEntity
import com.paradox.app.data.local.entity.BudgetEntity
import com.paradox.app.data.local.entity.CategoryEntity
import com.paradox.app.data.local.entity.DebtEntity
import com.paradox.app.data.local.entity.DebtRepaymentEntity
import com.paradox.app.data.local.entity.ExpenseEntity
import com.paradox.app.data.local.entity.IncomeEntity
import com.paradox.app.data.local.entity.PaymentMethodEntity
import com.paradox.app.data.local.entity.ProfileEntity
import com.paradox.app.data.local.entity.RecurringExpenseEntity
import com.paradox.app.data.local.entity.SavingsContributionEntity
import com.paradox.app.data.local.entity.SavingsGoalEntity
import com.paradox.app.data.local.entity.SyncQueueItemEntity

@Database(
    entities = [
        ProfileEntity::class,
        CategoryEntity::class,
        PaymentMethodEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class,
        IncomeEntity::class,
        AccountEntity::class,
        RecurringExpenseEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class,
        AiInsightLogEntity::class,
        SyncQueueItemEntity::class,
        DebtEntity::class,
        DebtRepaymentEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ParadoxDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
    abstract fun incomeDao(): IncomeDao
    abstract fun accountDao(): AccountDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun aiInsightLogDao(): AiInsightLogDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun debtDao(): DebtDao
    abstract fun debtRepaymentDao(): DebtRepaymentDao
}

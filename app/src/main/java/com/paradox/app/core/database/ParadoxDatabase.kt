package com.paradox.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.paradox.app.data.local.dao.BudgetDao
import com.paradox.app.data.local.dao.CategoryDao
import com.paradox.app.data.local.dao.ExpenseDao
import com.paradox.app.data.local.dao.PaymentMethodDao
import com.paradox.app.data.local.dao.ProfileDao
import com.paradox.app.data.local.entity.BudgetEntity
import com.paradox.app.data.local.entity.CategoryEntity
import com.paradox.app.data.local.entity.ExpenseEntity
import com.paradox.app.data.local.entity.PaymentMethodEntity
import com.paradox.app.data.local.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        CategoryEntity::class,
        PaymentMethodEntity::class,
        ExpenseEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ParadoxDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun categoryDao(): CategoryDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetDao(): BudgetDao
}

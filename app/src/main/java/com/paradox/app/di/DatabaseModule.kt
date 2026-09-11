package com.paradox.app.di

import android.content.Context
import androidx.room.Room
import com.paradox.app.core.common.Constants
import com.paradox.app.core.database.ParadoxDatabase
import com.paradox.app.core.security.PassphraseManager
import com.paradox.app.data.local.dao.BudgetDao
import com.paradox.app.data.local.dao.CategoryDao
import com.paradox.app.data.local.dao.ExpenseDao
import com.paradox.app.data.local.dao.PaymentMethodDao
import com.paradox.app.data.local.dao.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideParadoxDatabase(
        @ApplicationContext context: Context,
        passphraseManager: PassphraseManager
    ): ParadoxDatabase {
        val passphrase = passphraseManager.getOrCreatePassphrase()
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            ParadoxDatabase::class.java,
            Constants.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProfileDao(db: ParadoxDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideCategoryDao(db: ParadoxDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun providePaymentMethodDao(db: ParadoxDatabase): PaymentMethodDao = db.paymentMethodDao()

    @Provides
    fun provideExpenseDao(db: ParadoxDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideBudgetDao(db: ParadoxDatabase): BudgetDao = db.budgetDao()
}

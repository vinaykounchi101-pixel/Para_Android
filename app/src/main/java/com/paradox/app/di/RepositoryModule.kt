package com.paradox.app.di

import com.paradox.app.data.repository.AccountRepositoryImpl
import com.paradox.app.data.repository.BudgetRepositoryImpl
import com.paradox.app.data.repository.CategoryRepositoryImpl
import com.paradox.app.data.repository.ExpenseRepositoryImpl
import com.paradox.app.data.repository.IncomeRepositoryImpl
import com.paradox.app.data.repository.PaymentMethodRepositoryImpl
import com.paradox.app.data.repository.ProfileRepositoryImpl
import com.paradox.app.data.repository.RecurringExpenseRepositoryImpl
import com.paradox.app.data.repository.SavingsGoalRepositoryImpl
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import com.paradox.app.domain.repository.ProfileRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindPaymentMethodRepository(impl: PaymentMethodRepositoryImpl): PaymentMethodRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(impl: ExpenseRepositoryImpl): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindIncomeRepository(impl: IncomeRepositoryImpl): IncomeRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindRecurringExpenseRepository(impl: RecurringExpenseRepositoryImpl): RecurringExpenseRepository

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository
}

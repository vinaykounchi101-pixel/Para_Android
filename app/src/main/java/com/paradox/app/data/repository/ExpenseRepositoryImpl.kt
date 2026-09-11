package com.paradox.app.data.repository

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.dao.ExpenseDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.LedgerFilter
import com.paradox.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpenses(profileId: String): Flow<List<Expense>> {
        return expenseDao.getAllExpenses(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun filterExpenses(profileId: String, filter: LedgerFilter): Flow<List<Expense>> {
        return expenseDao.filterExpenses(
            profileId = profileId,
            query = filter.query.trim(),
            categoryId = filter.categoryId,
            paymentMethodId = filter.paymentMethodId,
            startDate = filter.startDate,
            endDate = filter.endDate,
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            sortBy = filter.sortBy.name
        ).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExpenseById(profileId: String, expenseId: String): Expense? {
        return expenseDao.getExpenseById(profileId, expenseId)?.toDomain()
    }

    override fun observeExpenseById(profileId: String, expenseId: String): Flow<Expense?> {
        return expenseDao.observeExpenseById(profileId, expenseId).map { it?.toDomain() }
    }

    override fun getRecentExpenses(profileId: String, limit: Int): Flow<List<Expense>> {
        return expenseDao.getRecentExpenses(profileId, limit).map { list -> list.map { it.toDomain() } }
    }

    override fun getExpensesInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>> {
        return expenseDao.getExpensesInRange(profileId, startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override fun observeTotalSpentInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<Money> {
        return expenseDao.observeTotalSpentInRange(profileId, startDate, endDate).map { total ->
            Money(total ?: BigDecimal.ZERO, "INR")
        }
    }

    override fun observeCategoryTotalSpentInRange(
        profileId: String,
        categoryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Money> {
        return expenseDao.observeCategoryTotalSpentInRange(profileId, categoryId, startDate, endDate).map { total ->
            Money(total ?: BigDecimal.ZERO, "INR")
        }
    }

    override suspend fun addExpense(expense: Expense) {
        expenseDao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(profileId: String, expenseId: String) {
        expenseDao.deleteExpenseById(profileId, expenseId)
    }
}

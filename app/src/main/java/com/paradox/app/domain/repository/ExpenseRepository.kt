package com.paradox.app.domain.repository

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.LedgerFilter
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ExpenseRepository {
    fun getAllExpenses(profileId: String): Flow<List<Expense>>
    fun filterExpenses(profileId: String, filter: LedgerFilter): Flow<List<Expense>>
    suspend fun getExpenseById(profileId: String, expenseId: String): Expense?
    fun observeExpenseById(profileId: String, expenseId: String): Flow<Expense?>
    fun getRecentExpenses(profileId: String, limit: Int = 5): Flow<List<Expense>>
    fun getExpensesInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>>
    fun observeTotalSpentInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<Money>
    fun observeCategoryTotalSpentInRange(profileId: String, categoryId: String, startDate: LocalDate, endDate: LocalDate): Flow<Money>
    suspend fun addExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(profileId: String, expenseId: String)
}

package com.paradox.app.domain.repository

import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(profileId: String): Flow<List<Budget>>
    fun getOverallBudget(profileId: String, type: BudgetType = BudgetType.MONTHLY): Flow<Budget?>
    fun getCategoryBudget(profileId: String, categoryId: String): Flow<Budget?>
    suspend fun getBudgetById(profileId: String, budgetId: String): Budget?
    suspend fun setBudget(budget: Budget)
    suspend fun deleteBudget(profileId: String, budgetId: String)
}

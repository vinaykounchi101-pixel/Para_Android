package com.paradox.app.data.repository

import com.paradox.app.data.local.dao.BudgetDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(profileId: String): Flow<List<Budget>> {
        return budgetDao.getAllBudgets(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun getOverallBudget(profileId: String, type: BudgetType): Flow<Budget?> {
        return budgetDao.getOverallBudget(profileId, type.name).map { it?.toDomain() }
    }

    override fun getCategoryBudget(profileId: String, categoryId: String): Flow<Budget?> {
        return budgetDao.getCategoryBudget(profileId, categoryId).map { it?.toDomain() }
    }

    override suspend fun getBudgetById(profileId: String, budgetId: String): Budget? {
        return budgetDao.getBudgetById(profileId, budgetId)?.toDomain()
    }

    override suspend fun setBudget(budget: Budget) {
        budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(profileId: String, budgetId: String) {
        budgetDao.deleteBudgetById(profileId, budgetId)
    }
}

package com.paradox.app.data.repository

import com.paradox.app.data.local.dao.SavingsGoalDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.SavingsContribution
import com.paradox.app.domain.model.SavingsGoal
import com.paradox.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepositoryImpl @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao
) : SavingsGoalRepository {

    override fun getAllGoals(profileId: String): Flow<List<SavingsGoal>> {
        return savingsGoalDao.getAllGoals(profileId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getGoalById(profileId: String, goalId: String): SavingsGoal? {
        return savingsGoalDao.getGoalById(profileId, goalId)?.toDomain()
    }

    override fun observeGoalById(profileId: String, goalId: String): Flow<SavingsGoal?> {
        return savingsGoalDao.observeGoalById(profileId, goalId).map { it?.toDomain() }
    }

    override suspend fun addGoal(goal: SavingsGoal) {
        savingsGoalDao.insertGoal(goal.toEntity())
    }

    override suspend fun updateGoal(goal: SavingsGoal) {
        savingsGoalDao.updateGoal(goal.toEntity())
    }

    override suspend fun deleteGoal(profileId: String, goalId: String) {
        savingsGoalDao.deleteGoalById(profileId, goalId)
    }

    override fun getContributionsForGoal(profileId: String, goalId: String): Flow<List<SavingsContribution>> {
        return savingsGoalDao.getContributionsForGoal(profileId, goalId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addContribution(contribution: SavingsContribution) {
        savingsGoalDao.recordContribution(contribution.toEntity())
    }
}

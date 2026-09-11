package com.paradox.app.domain.repository

import com.paradox.app.domain.model.SavingsContribution
import com.paradox.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getAllGoals(profileId: String): Flow<List<SavingsGoal>>
    suspend fun getGoalById(profileId: String, goalId: String): SavingsGoal?
    fun observeGoalById(profileId: String, goalId: String): Flow<SavingsGoal?>
    suspend fun addGoal(goal: SavingsGoal)
    suspend fun updateGoal(goal: SavingsGoal)
    suspend fun deleteGoal(profileId: String, goalId: String)
    fun getContributionsForGoal(profileId: String, goalId: String): Flow<List<SavingsContribution>>
    suspend fun addContribution(contribution: SavingsContribution)
}

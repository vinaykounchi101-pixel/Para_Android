package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.paradox.app.data.local.entity.SavingsContributionEntity
import com.paradox.app.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface SavingsGoalDao {

    @Query("SELECT * FROM savings_goals WHERE profileId = :profileId ORDER BY targetDate ASC")
    fun getAllGoals(profileId: String): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE profileId = :profileId AND id = :goalId LIMIT 1")
    suspend fun getGoalById(profileId: String, goalId: String): SavingsGoalEntity?

    @Query("SELECT * FROM savings_goals WHERE profileId = :profileId AND id = :goalId LIMIT 1")
    fun observeGoalById(profileId: String, goalId: String): Flow<SavingsGoalEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE profileId = :profileId AND id = :goalId")
    suspend fun deleteGoalById(profileId: String, goalId: String)

    // Contributions
    @Query("SELECT * FROM savings_contributions WHERE profileId = :profileId AND goalId = :goalId ORDER BY date DESC, createdAt DESC")
    fun getContributionsForGoal(profileId: String, goalId: String): Flow<List<SavingsContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertContribution(contribution: SavingsContributionEntity)

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :amount, updatedAt = :updatedAt WHERE profileId = :profileId AND id = :goalId")
    suspend fun addContributionToGoal(profileId: String, goalId: String, amount: BigDecimal, updatedAt: java.time.Instant)

    @Transaction
    suspend fun recordContribution(contribution: SavingsContributionEntity) {
        insertContribution(contribution)
        addContributionToGoal(
            profileId = contribution.profileId,
            goalId = contribution.goalId,
            amount = contribution.amount,
            updatedAt = java.time.Instant.now()
        )
    }
}

package com.paradox.app.domain.usecase.savingsgoal

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.SavingsContribution
import com.paradox.app.domain.model.SavingsGoal
import com.paradox.app.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class GetSavingsGoalsUseCase @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    operator fun invoke(profileId: String): Flow<List<SavingsGoal>> {
        return savingsGoalRepository.getAllGoals(profileId)
    }

    fun observeGoal(profileId: String, goalId: String): Flow<SavingsGoal?> {
        return savingsGoalRepository.observeGoalById(profileId, goalId)
    }

    fun getContributions(profileId: String, goalId: String): Flow<List<SavingsContribution>> {
        return savingsGoalRepository.getContributionsForGoal(profileId, goalId)
    }
}

class AddSavingsGoalUseCase @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(
        profileId: String,
        name: String,
        targetAmount: Money,
        currentAmount: Money = Money.zero(targetAmount.currencyCode),
        targetDate: LocalDate,
        colorHex: String = "#10B981",
        iconName: String = "savings"
    ): Result<SavingsGoal> {
        return try {
            if (name.isBlank()) {
                return Result.Error(IllegalArgumentException("Goal name cannot be blank"))
            }
            if (!targetAmount.isPositive()) {
                return Result.Error(IllegalArgumentException("Target amount must be greater than zero"))
            }

            val goal = SavingsGoal(
                id = "goal_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                name = name.trim(),
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                currency = targetAmount.currencyCode,
                targetDate = targetDate,
                colorHex = colorHex,
                iconName = iconName,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            savingsGoalRepository.addGoal(goal)
            Result.Success(goal)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class UpdateSavingsGoalUseCase @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(goal: SavingsGoal): Result<Unit> {
        return try {
            if (goal.name.isBlank()) {
                return Result.Error(IllegalArgumentException("Goal name cannot be blank"))
            }
            if (!goal.targetAmount.isPositive()) {
                return Result.Error(IllegalArgumentException("Target amount must be greater than zero"))
            }
            savingsGoalRepository.updateGoal(goal.copy(updatedAt = Instant.now()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeleteSavingsGoalUseCase @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(profileId: String, goalId: String): Result<Unit> {
        return try {
            savingsGoalRepository.deleteGoal(profileId, goalId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class ContributeToGoalUseCase @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    suspend operator fun invoke(
        profileId: String,
        goalId: String,
        amount: Money,
        date: LocalDate = LocalDate.now(),
        notes: String? = null
    ): Result<SavingsContribution> {
        return try {
            if (!amount.isPositive()) {
                return Result.Error(IllegalArgumentException("Contribution amount must be greater than zero"))
            }

            val contribution = SavingsContribution(
                id = "contrib_" + UUID.randomUUID().toString().replace("-", "").take(12),
                goalId = goalId,
                profileId = profileId,
                amount = amount,
                currency = amount.currencyCode,
                date = date,
                notes = notes?.trim()?.ifBlank { null },
                createdAt = Instant.now()
            )

            savingsGoalRepository.addContribution(contribution)
            Result.Success(contribution)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

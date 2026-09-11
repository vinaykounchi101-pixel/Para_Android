package com.paradox.app.domain.usecase.budget

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetStatus
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import javax.inject.Inject

class GetBudgetsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    operator fun invoke(profileId: String): Flow<List<Budget>> = budgetRepository.getAllBudgets(profileId)
}

class SetBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(
        profileId: String,
        type: BudgetType,
        limit: Money,
        categoryId: String? = null,
        thresholdPct: Int = 80
    ): Result<Budget> {
        return try {
            if (!limit.isPositive()) {
                return Result.Error(IllegalArgumentException("Budget limit must be greater than zero"))
            }

            val budget = Budget(
                id = "${profileId}_bgt_${type.name}_${categoryId ?: "overall"}",
                profileId = profileId,
                type = type,
                limit = limit,
                categoryId = categoryId,
                thresholdPct = thresholdPct
            )

            budgetRepository.setBudget(budget)
            Result.Success(budget)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeleteBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(profileId: String, budgetId: String): Result<Unit> {
        return try {
            budgetRepository.deleteBudget(profileId, budgetId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class CalculateBudgetStatusUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(profileId: String, type: BudgetType = BudgetType.MONTHLY): Flow<BudgetStatus?> {
        val now = LocalDate.now()
        val firstDay = now.with(TemporalAdjusters.firstDayOfMonth())
        val lastDay = now.with(TemporalAdjusters.lastDayOfMonth())

        return budgetRepository.getOverallBudget(profileId, type).flatMapLatest { budget ->
            if (budget == null) {
                flowOf(null)
            } else {
                expenseRepository.observeTotalSpentInRange(profileId, firstDay, lastDay).map { spent ->
                    BudgetStatus.calculate(budget, spent)
                }
            }
        }
    }

    fun observeCategoryBudgetStatus(profileId: String, categoryId: String): Flow<BudgetStatus?> {
        val now = LocalDate.now()
        val firstDay = now.with(TemporalAdjusters.firstDayOfMonth())
        val lastDay = now.with(TemporalAdjusters.lastDayOfMonth())

        return budgetRepository.getCategoryBudget(profileId, categoryId).flatMapLatest { budget ->
            if (budget == null) {
                flowOf(null)
            } else {
                expenseRepository.observeCategoryTotalSpentInRange(profileId, categoryId, firstDay, lastDay).map { spent ->
                    BudgetStatus.calculate(budget, spent)
                }
            }
        }
    }
}

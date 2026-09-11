package com.paradox.app.domain.usecase.income

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.CashFlowSummary
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class AddIncomeUseCase @Inject constructor(
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(
        profileId: String,
        source: IncomeSource,
        money: Money,
        currency: String = money.currencyCode,
        date: LocalDate,
        notes: String? = null
    ): Result<Income> {
        return try {
            if (!money.isPositive()) {
                return Result.Error(IllegalArgumentException("Income amount must be greater than zero"))
            }
            if (date.isAfter(LocalDate.now())) {
                return Result.Error(IllegalArgumentException("Income date cannot be in the future"))
            }

            val income = Income(
                id = "inc_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                source = source,
                amount = money,
                currency = currency,
                date = date,
                notes = notes?.trim()?.ifBlank { null },
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            incomeRepository.addIncome(income)
            Result.Success(income)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class UpdateIncomeUseCase @Inject constructor(
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(income: Income): Result<Unit> {
        return try {
            if (!income.amount.isPositive()) {
                return Result.Error(IllegalArgumentException("Income amount must be greater than zero"))
            }
            if (income.date.isAfter(LocalDate.now())) {
                return Result.Error(IllegalArgumentException("Income date cannot be in the future"))
            }

            incomeRepository.updateIncome(income.copy(updatedAt = Instant.now()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeleteIncomeUseCase @Inject constructor(
    private val incomeRepository: IncomeRepository
) {
    suspend operator fun invoke(profileId: String, incomeId: String): Result<Unit> {
        return try {
            incomeRepository.deleteIncome(profileId, incomeId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class GetIncomesUseCase @Inject constructor(
    private val incomeRepository: IncomeRepository
) {
    operator fun invoke(
        profileId: String,
        source: IncomeSource? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Flow<List<Income>> {
        return incomeRepository.filterIncomes(profileId, source, startDate, endDate)
    }
}

class CalculateCashFlowUseCase @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(
        profileId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        currency: String = "INR"
    ): Flow<CashFlowSummary> {
        val totalIncomeFlow = incomeRepository.observeTotalIncomeInRange(profileId, startDate, endDate, currency)
        val totalExpenseFlow = expenseRepository.observeTotalSpentInRange(profileId, startDate, endDate)

        return combine(totalIncomeFlow, totalExpenseFlow) { income, expense ->
            val netCashFlow = income - expense
            val savingsAmount = if (netCashFlow.isPositive()) netCashFlow else Money.zero(currency)
            val savingsRate = if (income.isPositive()) {
                (savingsAmount.amount.divide(income.amount, 4, RoundingMode.HALF_EVEN) * BigDecimal(100)).toDouble()
            } else {
                0.0
            }

            CashFlowSummary(
                totalIncome = income,
                totalExpense = expense,
                netCashFlow = netCashFlow,
                savingsAmount = savingsAmount,
                savingsRatePct = savingsRate
            )
        }
    }
}

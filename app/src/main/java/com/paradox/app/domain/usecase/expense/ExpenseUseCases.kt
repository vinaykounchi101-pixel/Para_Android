package com.paradox.app.domain.usecase.expense

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.LedgerFilter
import com.paradox.app.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(
        profileId: String,
        title: String,
        money: Money,
        categoryId: String,
        paymentMethodId: String,
        date: LocalDate,
        notes: String? = null,
        recurringFlag: Boolean = false,
        source: String = "MANUAL",
        attachmentRef: String? = null
    ): Result<Expense> {
        return try {
            if (title.isBlank()) {
                return Result.Error(IllegalArgumentException("Expense title cannot be blank"))
            }
            if (!money.isPositive()) {
                return Result.Error(IllegalArgumentException("Expense amount must be greater than zero"))
            }
            if (date.isAfter(LocalDate.now())) {
                return Result.Error(IllegalArgumentException("Expense date cannot be in the future"))
            }
            if (categoryId.isBlank()) {
                return Result.Error(IllegalArgumentException("Please select a category"))
            }
            if (paymentMethodId.isBlank()) {
                return Result.Error(IllegalArgumentException("Please select a payment method"))
            }

            val expense = Expense(
                id = "exp_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                title = title.trim(),
                money = money,
                categoryId = categoryId,
                paymentMethodId = paymentMethodId,
                date = date,
                notes = notes?.trim()?.ifBlank { null },
                recurringFlag = recurringFlag,
                source = source,
                attachmentRef = attachmentRef,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            expenseRepository.addExpense(expense)
            Result.Success(expense)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class UpdateExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Result<Unit> {
        return try {
            if (expense.title.isBlank()) {
                return Result.Error(IllegalArgumentException("Expense title cannot be blank"))
            }
            if (!expense.money.isPositive()) {
                return Result.Error(IllegalArgumentException("Expense amount must be greater than zero"))
            }
            if (expense.date.isAfter(LocalDate.now())) {
                return Result.Error(IllegalArgumentException("Expense date cannot be in the future"))
            }

            expenseRepository.updateExpense(expense.copy(updatedAt = Instant.now()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(profileId: String, expenseId: String): Result<Unit> {
        return try {
            expenseRepository.deleteExpense(profileId, expenseId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class GetExpenseByIdUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    suspend operator fun invoke(profileId: String, expenseId: String): Expense? {
        return expenseRepository.getExpenseById(profileId, expenseId)
    }

    fun observe(profileId: String, expenseId: String): Flow<Expense?> {
        return expenseRepository.observeExpenseById(profileId, expenseId)
    }
}

class GetExpensesUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository
) {
    operator fun invoke(profileId: String, filter: LedgerFilter = LedgerFilter()): Flow<List<Expense>> {
        return expenseRepository.filterExpenses(profileId, filter)
    }
}

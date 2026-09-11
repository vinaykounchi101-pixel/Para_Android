package com.paradox.app.domain.usecase.recurring

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class GetRecurringExpensesUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository
) {
    operator fun invoke(profileId: String, activeOnly: Boolean = false): Flow<List<RecurringExpense>> {
        return if (activeOnly) {
            recurringRepository.getActiveRecurring(profileId)
        } else {
            recurringRepository.getAllRecurring(profileId)
        }
    }
}

class AddRecurringExpenseUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository
) {
    suspend operator fun invoke(
        profileId: String,
        title: String,
        amount: Money,
        currency: String = amount.currencyCode,
        categoryId: String,
        paymentMethodId: String,
        frequency: RecurringFrequency,
        startDate: LocalDate,
        notes: String? = null
    ): Result<RecurringExpense> {
        return try {
            if (title.isBlank()) {
                return Result.Error(IllegalArgumentException("Subscription title cannot be blank"))
            }
            if (!amount.isPositive()) {
                return Result.Error(IllegalArgumentException("Amount must be greater than zero"))
            }
            if (categoryId.isBlank()) {
                return Result.Error(IllegalArgumentException("Please select a category"))
            }
            if (paymentMethodId.isBlank()) {
                return Result.Error(IllegalArgumentException("Please select a payment method"))
            }

            val nextDueDate = when (frequency) {
                RecurringFrequency.DAILY -> startDate.plusDays(1)
                RecurringFrequency.WEEKLY -> startDate.plusWeeks(1)
                RecurringFrequency.BIWEEKLY -> startDate.plusWeeks(2)
                RecurringFrequency.MONTHLY -> startDate.plusMonths(1)
                RecurringFrequency.QUARTERLY -> startDate.plusMonths(3)
                RecurringFrequency.YEARLY -> startDate.plusYears(1)
            }

            val recurring = RecurringExpense(
                id = "rec_" + UUID.randomUUID().toString().replace("-", "").take(12),
                profileId = profileId,
                title = title.trim(),
                amount = amount,
                currency = currency,
                categoryId = categoryId,
                paymentMethodId = paymentMethodId,
                frequency = frequency,
                startDate = startDate,
                nextDueDate = nextDueDate,
                isActive = true,
                notes = notes?.trim()?.ifBlank { null },
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            recurringRepository.addRecurring(recurring)
            Result.Success(recurring)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class UpdateRecurringExpenseUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository
) {
    suspend operator fun invoke(recurring: RecurringExpense): Result<Unit> {
        return try {
            if (recurring.title.isBlank()) {
                return Result.Error(IllegalArgumentException("Title cannot be blank"))
            }
            if (!recurring.amount.isPositive()) {
                return Result.Error(IllegalArgumentException("Amount must be greater than zero"))
            }
            recurringRepository.updateRecurring(recurring.copy(updatedAt = Instant.now()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeleteRecurringExpenseUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository
) {
    suspend operator fun invoke(profileId: String, id: String): Result<Unit> {
        return try {
            recurringRepository.deleteRecurring(profileId, id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class CalculateMonthlyCommitmentUseCase @Inject constructor(
    private val recurringRepository: RecurringExpenseRepository
) {
    operator fun invoke(profileId: String, currency: String = "INR"): Flow<Money> {
        return recurringRepository.getActiveRecurring(profileId).map { list ->
            var total = BigDecimal.ZERO
            list.forEach { item ->
                total = total.add(item.monthlyNormalizedAmount().amount)
            }
            Money(total, currency)
        }
    }
}

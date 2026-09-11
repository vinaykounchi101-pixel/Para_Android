package com.paradox.app.data.mapper

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.entity.RecurringExpenseEntity
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency

fun RecurringExpenseEntity.toDomain(): RecurringExpense {
    return RecurringExpense(
        id = id,
        profileId = profileId,
        title = title,
        amount = Money(amount, currency),
        currency = currency,
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        frequency = try {
            RecurringFrequency.valueOf(frequency)
        } catch (e: Exception) {
            RecurringFrequency.MONTHLY
        },
        startDate = startDate,
        nextDueDate = nextDueDate,
        isActive = isActive,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun RecurringExpense.toEntity(): RecurringExpenseEntity {
    return RecurringExpenseEntity(
        id = id,
        profileId = profileId,
        title = title,
        amount = amount.amount,
        currency = currency,
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        frequency = frequency.name,
        startDate = startDate,
        nextDueDate = nextDueDate,
        isActive = isActive,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

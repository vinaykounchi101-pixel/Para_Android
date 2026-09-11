package com.paradox.app.data.mapper

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.entity.ExpenseEntity
import com.paradox.app.domain.model.Expense

fun ExpenseEntity.toDomain(): Expense {
    return Expense(
        id = id,
        profileId = profileId,
        title = title,
        money = Money(amount, currency),
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        date = date,
        notes = notes,
        recurringFlag = recurringFlag,
        source = source,
        attachmentRef = attachmentRef,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Expense.toEntity(): ExpenseEntity {
    return ExpenseEntity(
        id = id,
        profileId = profileId,
        title = title,
        amount = money.amount,
        currency = money.currencyCode,
        categoryId = categoryId,
        paymentMethodId = paymentMethodId,
        date = date,
        notes = notes,
        recurringFlag = recurringFlag,
        source = source,
        attachmentRef = attachmentRef,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

package com.paradox.app.data.mapper

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.entity.BudgetEntity
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType

fun BudgetEntity.toDomain(currencyCode: String = "INR"): Budget {
    val budgetType = try {
        BudgetType.valueOf(type)
    } catch (_: Exception) {
        BudgetType.MONTHLY
    }
    return Budget(
        id = id,
        profileId = profileId,
        type = budgetType,
        limit = Money(amount, currencyCode),
        categoryId = categoryId,
        thresholdPct = thresholdPct
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        profileId = profileId,
        type = type.name,
        amount = limit.amount,
        categoryId = categoryId,
        thresholdPct = thresholdPct
    )
}

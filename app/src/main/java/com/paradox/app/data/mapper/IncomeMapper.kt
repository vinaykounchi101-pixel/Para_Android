package com.paradox.app.data.mapper

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.entity.IncomeEntity
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource

fun IncomeEntity.toDomain(): Income {
    return Income(
        id = id,
        profileId = profileId,
        source = try {
            IncomeSource.valueOf(source)
        } catch (e: Exception) {
            IncomeSource.OTHER
        },
        amount = Money(amount, currency),
        currency = currency,
        date = date,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Income.toEntity(): IncomeEntity {
    return IncomeEntity(
        id = id,
        profileId = profileId,
        source = source.name,
        amount = amount.amount,
        currency = currency,
        date = date,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

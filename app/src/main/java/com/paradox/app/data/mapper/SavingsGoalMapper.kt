package com.paradox.app.data.mapper

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.entity.SavingsContributionEntity
import com.paradox.app.data.local.entity.SavingsGoalEntity
import com.paradox.app.domain.model.SavingsContribution
import com.paradox.app.domain.model.SavingsGoal

fun SavingsGoalEntity.toDomain(): SavingsGoal {
    return SavingsGoal(
        id = id,
        profileId = profileId,
        name = name,
        targetAmount = Money(targetAmount, currency),
        currentAmount = Money(currentAmount, currency),
        currency = currency,
        targetDate = targetDate,
        colorHex = colorHex,
        iconName = iconName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SavingsGoal.toEntity(): SavingsGoalEntity {
    return SavingsGoalEntity(
        id = id,
        profileId = profileId,
        name = name,
        targetAmount = targetAmount.amount,
        currentAmount = currentAmount.amount,
        currency = currency,
        targetDate = targetDate,
        colorHex = colorHex,
        iconName = iconName,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SavingsContributionEntity.toDomain(): SavingsContribution {
    return SavingsContribution(
        id = id,
        goalId = goalId,
        profileId = profileId,
        amount = Money(amount, currency),
        currency = currency,
        date = date,
        notes = notes,
        createdAt = createdAt
    )
}

fun SavingsContribution.toEntity(): SavingsContributionEntity {
    return SavingsContributionEntity(
        id = id,
        goalId = goalId,
        profileId = profileId,
        amount = amount.amount,
        currency = currency,
        date = date,
        notes = notes,
        createdAt = createdAt
    )
}

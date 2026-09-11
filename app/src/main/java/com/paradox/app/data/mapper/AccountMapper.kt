package com.paradox.app.data.mapper

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.entity.AccountEntity
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType

fun AccountEntity.toDomain(): Account {
    return Account(
        id = id,
        profileId = profileId,
        name = name,
        type = try {
            AccountType.valueOf(type)
        } catch (e: Exception) {
            AccountType.CUSTOM
        },
        currency = currency,
        initialBalance = Money(initialBalance, currency),
        colorHex = colorHex,
        iconName = iconName,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        profileId = profileId,
        name = name,
        type = type.name,
        currency = currency,
        initialBalance = initialBalance.amount,
        colorHex = colorHex,
        iconName = iconName,
        isDefault = isDefault,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

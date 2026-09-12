package com.paradox.app.data.mapper

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.entity.DebtEntity
import com.paradox.app.data.local.entity.DebtRepaymentEntity
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.model.DebtStatus
import com.paradox.app.domain.model.DebtType

fun DebtEntity.toDomain(): Debt {
    return Debt(
        id = id,
        profileId = profileId,
        personName = personName,
        personContactNumber = personContactNumber,
        debtType = when (debtType.uppercase()) {
            "BORROWED" -> DebtType.BORROWED
            else -> DebtType.LENT
        },
        initialAmount = Money(initialAmount, currency),
        remainingAmount = Money(remainingAmount, currency),
        dueDate = dueDate,
        notes = notes,
        status = when (status.uppercase()) {
            "SETTLED" -> DebtStatus.SETTLED
            else -> DebtStatus.ACTIVE
        },
        reminderEnabled = reminderEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Debt.toEntity(): DebtEntity {
    return DebtEntity(
        id = id,
        profileId = profileId,
        personName = personName,
        personContactNumber = personContactNumber,
        debtType = debtType.name,
        initialAmount = initialAmount.amount,
        remainingAmount = remainingAmount.amount,
        currency = initialAmount.currencyCode,
        dueDate = dueDate,
        notes = notes,
        status = status.name,
        reminderEnabled = reminderEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun DebtRepaymentEntity.toDomain(): DebtRepayment {
    return DebtRepayment(
        id = id,
        debtId = debtId,
        profileId = profileId,
        amount = Money(amount, currency),
        repaymentDate = repaymentDate,
        notes = notes,
        createdAt = createdAt
    )
}

fun DebtRepayment.toEntity(): DebtRepaymentEntity {
    return DebtRepaymentEntity(
        id = id,
        debtId = debtId,
        profileId = profileId,
        amount = amount.amount,
        currency = amount.currencyCode,
        repaymentDate = repaymentDate,
        notes = notes,
        createdAt = createdAt
    )
}

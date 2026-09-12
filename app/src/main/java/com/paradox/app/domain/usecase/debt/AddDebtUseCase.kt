package com.paradox.app.domain.usecase.debt

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtStatus
import com.paradox.app.domain.model.DebtType
import com.paradox.app.domain.repository.DebtRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class AddDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(
        profileId: String,
        personName: String,
        personContactNumber: String?,
        debtType: DebtType,
        amount: BigDecimal,
        currency: String = "INR",
        dueDate: LocalDate? = null,
        notes: String? = null,
        reminderEnabled: Boolean = false
    ): Result<Debt> {
        val trimmedName = personName.trim()
        if (trimmedName.isBlank()) {
            return Result.Error(IllegalArgumentException("Person name cannot be blank"))
        }

        if (amount <= BigDecimal.ZERO) {
            return Result.Error(IllegalArgumentException("Debt amount must be greater than zero"))
        }

        val money = Money(amount, currency)
        val debt = Debt(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            personName = trimmedName,
            personContactNumber = personContactNumber?.trim()?.takeIf { it.isNotBlank() },
            debtType = debtType,
            initialAmount = money,
            remainingAmount = money,
            dueDate = dueDate,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            status = DebtStatus.ACTIVE,
            reminderEnabled = reminderEnabled,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        return try {
            debtRepository.insertDebt(debt)
            Result.Success(debt)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

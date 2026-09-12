package com.paradox.app.domain.usecase.debt

import com.paradox.app.core.common.Result
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.repository.DebtRepository
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

class UpdateDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(debt: Debt): Result<Unit> {
        if (debt.personName.isBlank()) {
            return Result.Error(IllegalArgumentException("Person name cannot be blank"))
        }
        if (debt.initialAmount.amount <= BigDecimal.ZERO) {
            return Result.Error(IllegalArgumentException("Debt amount must be greater than zero"))
        }

        return try {
            debtRepository.updateDebt(debt.copy(updatedAt = Instant.now()))
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

package com.paradox.app.domain.usecase.debt

import com.paradox.app.core.common.Result
import com.paradox.app.core.money.CurrencyFormatter
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.repository.DebtRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

class RecordDebtRepaymentUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(
        profileId: String,
        debtId: String,
        amount: BigDecimal,
        currency: String = "INR",
        repaymentDate: LocalDate = LocalDate.now(),
        notes: String? = null
    ): Result<DebtRepayment> {
        if (amount <= BigDecimal.ZERO) {
            return Result.Error(IllegalArgumentException("Repayment amount must be greater than zero"))
        }

        val debt = debtRepository.getDebtById(profileId, debtId)
            ?: return Result.Error(IllegalArgumentException("Debt not found"))

        if (amount > debt.remainingAmount.amount) {
            return Result.Error(IllegalArgumentException("Repayment amount cannot exceed remaining debt (${CurrencyFormatter.format(debt.remainingAmount)})"))
        }

        val repayment = DebtRepayment(
            id = UUID.randomUUID().toString(),
            debtId = debtId,
            profileId = profileId,
            amount = Money(amount, currency),
            repaymentDate = repaymentDate,
            notes = notes?.trim()?.takeIf { it.isNotBlank() },
            createdAt = Instant.now()
        )

        return try {
            debtRepository.recordRepayment(repayment)
            Result.Success(repayment)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

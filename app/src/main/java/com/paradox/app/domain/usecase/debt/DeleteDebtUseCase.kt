package com.paradox.app.domain.usecase.debt

import com.paradox.app.core.common.Result
import com.paradox.app.domain.repository.DebtRepository
import javax.inject.Inject

class DeleteDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    suspend operator fun invoke(profileId: String, id: String): Result<Unit> {
        return try {
            debtRepository.deleteDebt(profileId, id)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

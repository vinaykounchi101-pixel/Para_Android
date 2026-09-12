package com.paradox.app.domain.usecase.debt

import com.paradox.app.domain.model.DebtSummary
import com.paradox.app.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDebtSummaryUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    operator fun invoke(profileId: String): Flow<DebtSummary> {
        return debtRepository.observeDebtSummary(profileId)
    }

    suspend fun sync(profileId: String): DebtSummary {
        return debtRepository.getDebtSummarySync(profileId)
    }
}

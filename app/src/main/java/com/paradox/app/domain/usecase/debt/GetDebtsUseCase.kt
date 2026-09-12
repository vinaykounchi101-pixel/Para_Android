package com.paradox.app.domain.usecase.debt

import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtType
import com.paradox.app.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

enum class DebtFilter {
    ALL,
    LENT,      // To Receive
    BORROWED,  // To Pay
    ACTIVE,
    SETTLED
}

class GetDebtsUseCase @Inject constructor(
    private val debtRepository: DebtRepository
) {
    operator fun invoke(profileId: String, filter: DebtFilter = DebtFilter.ALL): Flow<List<Debt>> {
        return when (filter) {
            DebtFilter.ALL -> debtRepository.getAllDebts(profileId)
            DebtFilter.LENT -> debtRepository.getDebtsByType(profileId, DebtType.LENT)
            DebtFilter.BORROWED -> debtRepository.getDebtsByType(profileId, DebtType.BORROWED)
            DebtFilter.ACTIVE -> debtRepository.getActiveDebts(profileId)
            DebtFilter.SETTLED -> debtRepository.getSettledDebts(profileId)
        }
    }
}

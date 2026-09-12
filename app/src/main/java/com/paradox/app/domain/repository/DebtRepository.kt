package com.paradox.app.domain.repository

import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.model.DebtSummary
import com.paradox.app.domain.model.DebtType
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun getAllDebts(profileId: String): Flow<List<Debt>>
    fun getDebtsByType(profileId: String, debtType: DebtType): Flow<List<Debt>>
    fun getActiveDebts(profileId: String): Flow<List<Debt>>
    fun getSettledDebts(profileId: String): Flow<List<Debt>>
    fun observeDebtById(profileId: String, id: String): Flow<Debt?>
    suspend fun getDebtById(profileId: String, id: String): Debt?

    fun observeDebtSummary(profileId: String): Flow<DebtSummary>
    suspend fun getDebtSummarySync(profileId: String): DebtSummary

    suspend fun insertDebt(debt: Debt)
    suspend fun updateDebt(debt: Debt)
    suspend fun deleteDebt(profileId: String, id: String)

    fun getRepaymentsForDebt(profileId: String, debtId: String): Flow<List<DebtRepayment>>
    suspend fun recordRepayment(repayment: DebtRepayment)
    suspend fun deleteRepayment(profileId: String, id: String)
}

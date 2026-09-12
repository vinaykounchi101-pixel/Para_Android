package com.paradox.app.data.repository

import com.paradox.app.core.common.Constants
import com.paradox.app.core.money.Money
import com.paradox.app.data.local.dao.DebtDao
import com.paradox.app.data.local.dao.DebtRepaymentDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.model.DebtSummary
import com.paradox.app.domain.model.DebtType
import com.paradox.app.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepositoryImpl @Inject constructor(
    private val debtDao: DebtDao,
    private val debtRepaymentDao: DebtRepaymentDao
) : DebtRepository {

    override fun getAllDebts(profileId: String): Flow<List<Debt>> {
        return debtDao.getAllDebts(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun getDebtsByType(profileId: String, debtType: DebtType): Flow<List<Debt>> {
        return debtDao.getDebtsByType(profileId, debtType.name).map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveDebts(profileId: String): Flow<List<Debt>> {
        return debtDao.getActiveDebts(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun getSettledDebts(profileId: String): Flow<List<Debt>> {
        return debtDao.getSettledDebts(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun observeDebtById(profileId: String, id: String): Flow<Debt?> {
        return debtDao.observeDebtById(profileId, id).map { it?.toDomain() }
    }

    override suspend fun getDebtById(profileId: String, id: String): Debt? {
        return debtDao.getDebtById(profileId, id)?.toDomain()
    }

    override fun observeDebtSummary(profileId: String): Flow<DebtSummary> {
        return combine(
            debtDao.observeTotalReceivable(profileId),
            debtDao.observeTotalPayable(profileId)
        ) { receivable, payable ->
            val rec = receivable ?: BigDecimal.ZERO
            val pay = payable ?: BigDecimal.ZERO
            DebtSummary(
                totalReceivable = Money(rec, Constants.DEFAULT_CURRENCY),
                totalPayable = Money(pay, Constants.DEFAULT_CURRENCY),
                netBalance = Money(rec - pay, Constants.DEFAULT_CURRENCY)
            )
        }
    }

    override suspend fun getDebtSummarySync(profileId: String): DebtSummary {
        val rec = debtDao.getTotalReceivableSync(profileId) ?: BigDecimal.ZERO
        val pay = debtDao.getTotalPayableSync(profileId) ?: BigDecimal.ZERO
        return DebtSummary(
            totalReceivable = Money(rec, Constants.DEFAULT_CURRENCY),
            totalPayable = Money(pay, Constants.DEFAULT_CURRENCY),
            netBalance = Money(rec - pay, Constants.DEFAULT_CURRENCY)
        )
    }

    override suspend fun insertDebt(debt: Debt) {
        debtDao.insertDebt(debt.toEntity())
    }

    override suspend fun updateDebt(debt: Debt) {
        debtDao.updateDebt(debt.toEntity())
    }

    override suspend fun deleteDebt(profileId: String, id: String) {
        debtDao.deleteDebtById(profileId, id)
    }

    override fun getRepaymentsForDebt(profileId: String, debtId: String): Flow<List<DebtRepayment>> {
        return debtRepaymentDao.getRepaymentsForDebt(profileId, debtId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordRepayment(repayment: DebtRepayment) {
        debtRepaymentDao.recordRepaymentAndUpdateDebt(repayment.toEntity(), debtDao)
    }

    override suspend fun deleteRepayment(profileId: String, id: String) {
        debtRepaymentDao.deleteRepaymentById(profileId, id)
    }
}

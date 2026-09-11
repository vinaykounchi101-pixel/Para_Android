package com.paradox.app.data.repository

import com.paradox.app.core.money.Money
import com.paradox.app.data.local.dao.IncomeDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.repository.IncomeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepositoryImpl @Inject constructor(
    private val incomeDao: IncomeDao
) : IncomeRepository {

    override fun getAllIncomes(profileId: String): Flow<List<Income>> {
        return incomeDao.getAllIncomes(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun filterIncomes(
        profileId: String,
        source: IncomeSource?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Flow<List<Income>> {
        return incomeDao.filterIncomes(
            profileId = profileId,
            source = source?.name,
            startDate = startDate,
            endDate = endDate
        ).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getIncomeById(profileId: String, incomeId: String): Income? {
        return incomeDao.getIncomeById(profileId, incomeId)?.toDomain()
    }

    override fun observeIncomeById(profileId: String, incomeId: String): Flow<Income?> {
        return incomeDao.observeIncomeById(profileId, incomeId).map { it?.toDomain() }
    }

    override fun getIncomesInRange(
        profileId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<Income>> {
        return incomeDao.getIncomesInRange(profileId, startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override fun observeTotalIncomeInRange(
        profileId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        currency: String
    ): Flow<Money> {
        return incomeDao.observeTotalIncomeInRange(profileId, startDate, endDate).map { total ->
            Money(total ?: BigDecimal.ZERO, currency)
        }
    }

    override suspend fun addIncome(income: Income) {
        incomeDao.insertIncome(income.toEntity())
    }

    override suspend fun updateIncome(income: Income) {
        incomeDao.updateIncome(income.toEntity())
    }

    override suspend fun deleteIncome(profileId: String, incomeId: String) {
        incomeDao.deleteIncomeById(profileId, incomeId)
    }
}

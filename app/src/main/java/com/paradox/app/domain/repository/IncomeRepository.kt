package com.paradox.app.domain.repository

import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface IncomeRepository {
    fun getAllIncomes(profileId: String): Flow<List<Income>>
    fun filterIncomes(
        profileId: String,
        source: IncomeSource? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Flow<List<Income>>
    suspend fun getIncomeById(profileId: String, incomeId: String): Income?
    fun observeIncomeById(profileId: String, incomeId: String): Flow<Income?>
    fun getIncomesInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<Income>>
    fun observeTotalIncomeInRange(profileId: String, startDate: LocalDate, endDate: LocalDate, currency: String = "INR"): Flow<Money>
    suspend fun addIncome(income: Income)
    suspend fun updateIncome(income: Income)
    suspend fun deleteIncome(profileId: String, incomeId: String)
}

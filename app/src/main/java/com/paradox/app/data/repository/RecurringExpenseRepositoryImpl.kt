package com.paradox.app.data.repository

import com.paradox.app.data.local.dao.RecurringExpenseDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.repository.RecurringExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringExpenseRepositoryImpl @Inject constructor(
    private val recurringExpenseDao: RecurringExpenseDao
) : RecurringExpenseRepository {

    override fun getAllRecurring(profileId: String): Flow<List<RecurringExpense>> {
        return recurringExpenseDao.getAllRecurring(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun getActiveRecurring(profileId: String): Flow<List<RecurringExpense>> {
        return recurringExpenseDao.getActiveRecurring(profileId).map { list -> list.map { it.toDomain() } }
    }

    override fun getUpcomingRecurring(profileId: String, beforeDate: LocalDate): Flow<List<RecurringExpense>> {
        return recurringExpenseDao.getUpcomingRecurring(profileId, beforeDate).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getRecurringById(profileId: String, id: String): RecurringExpense? {
        return recurringExpenseDao.getRecurringById(profileId, id)?.toDomain()
    }

    override fun observeRecurringById(profileId: String, id: String): Flow<RecurringExpense?> {
        return recurringExpenseDao.observeRecurringById(profileId, id).map { it?.toDomain() }
    }

    override suspend fun addRecurring(recurring: RecurringExpense) {
        recurringExpenseDao.insertRecurring(recurring.toEntity())
    }

    override suspend fun updateRecurring(recurring: RecurringExpense) {
        recurringExpenseDao.updateRecurring(recurring.toEntity())
    }

    override suspend fun deleteRecurring(profileId: String, id: String) {
        recurringExpenseDao.deleteRecurringById(profileId, id)
    }
}

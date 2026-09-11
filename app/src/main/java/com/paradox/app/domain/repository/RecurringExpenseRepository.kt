package com.paradox.app.domain.repository

import com.paradox.app.domain.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecurringExpenseRepository {
    fun getAllRecurring(profileId: String): Flow<List<RecurringExpense>>
    fun getActiveRecurring(profileId: String): Flow<List<RecurringExpense>>
    fun getUpcomingRecurring(profileId: String, beforeDate: LocalDate): Flow<List<RecurringExpense>>
    suspend fun getRecurringById(profileId: String, id: String): RecurringExpense?
    fun observeRecurringById(profileId: String, id: String): Flow<RecurringExpense?>
    suspend fun addRecurring(recurring: RecurringExpense)
    suspend fun updateRecurring(recurring: RecurringExpense)
    suspend fun deleteRecurring(profileId: String, id: String)
}

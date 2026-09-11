package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RecurringExpenseDao {

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId ORDER BY nextDueDate ASC")
    fun getAllRecurring(profileId: String): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId AND isActive = 1 ORDER BY nextDueDate ASC")
    fun getActiveRecurring(profileId: String): Flow<List<RecurringExpenseEntity>>

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId AND id = :id LIMIT 1")
    suspend fun getRecurringById(profileId: String, id: String): RecurringExpenseEntity?

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId AND id = :id LIMIT 1")
    fun observeRecurringById(profileId: String, id: String): Flow<RecurringExpenseEntity?>

    @Query("SELECT * FROM recurring_expenses WHERE profileId = :profileId AND isActive = 1 AND nextDueDate <= :beforeDate ORDER BY nextDueDate ASC")
    fun getUpcomingRecurring(profileId: String, beforeDate: LocalDate): Flow<List<RecurringExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRecurring(recurring: RecurringExpenseEntity)

    @Update
    suspend fun updateRecurring(recurring: RecurringExpenseEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringExpenseEntity)

    @Query("DELETE FROM recurring_expenses WHERE profileId = :profileId AND id = :id")
    suspend fun deleteRecurringById(profileId: String, id: String)
}

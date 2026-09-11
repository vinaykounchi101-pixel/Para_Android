package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes WHERE profileId = :profileId ORDER BY date DESC, createdAt DESC")
    fun getAllIncomes(profileId: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE profileId = :profileId AND id = :incomeId LIMIT 1")
    suspend fun getIncomeById(profileId: String, incomeId: String): IncomeEntity?

    @Query("SELECT * FROM incomes WHERE profileId = :profileId AND id = :incomeId LIMIT 1")
    fun observeIncomeById(profileId: String, incomeId: String): Flow<IncomeEntity?>

    @Query("""
        SELECT * FROM incomes 
        WHERE profileId = :profileId 
          AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getIncomesInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<IncomeEntity>>

    @Query("""
        SELECT SUM(amount) FROM incomes 
        WHERE profileId = :profileId 
          AND date >= :startDate AND date <= :endDate
    """)
    fun observeTotalIncomeInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<BigDecimal?>

    @Query("""
        SELECT * FROM incomes 
        WHERE profileId = :profileId 
          AND (:source IS NULL OR source = :source)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC, createdAt DESC
    """)
    fun filterIncomes(
        profileId: String,
        source: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Flow<List<IncomeEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertIncome(income: IncomeEntity)

    @Update
    suspend fun updateIncome(income: IncomeEntity)

    @Delete
    suspend fun deleteIncome(income: IncomeEntity)

    @Query("DELETE FROM incomes WHERE profileId = :profileId AND id = :incomeId")
    suspend fun deleteIncomeById(profileId: String, incomeId: String)
}

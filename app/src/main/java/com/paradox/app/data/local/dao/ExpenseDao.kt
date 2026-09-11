package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY date DESC, createdAt DESC")
    fun getAllExpenses(profileId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE profileId = :profileId AND id = :expenseId LIMIT 1")
    suspend fun getExpenseById(profileId: String, expenseId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE profileId = :profileId AND id = :expenseId LIMIT 1")
    fun observeExpenseById(profileId: String, expenseId: String): Flow<ExpenseEntity?>

    @Query("""
        SELECT * FROM expenses 
        WHERE profileId = :profileId 
          AND (:query = '' OR LOWER(title) LIKE '%' || LOWER(:query) || '%' OR LOWER(notes) LIKE '%' || LOWER(:query) || '%')
          AND (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:paymentMethodId IS NULL OR paymentMethodId = :paymentMethodId)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
        ORDER BY 
          CASE WHEN :sortBy = 'DATE_DESC' THEN date END DESC,
          CASE WHEN :sortBy = 'DATE_ASC' THEN date END ASC,
          CASE WHEN :sortBy = 'AMOUNT_DESC' THEN amount END DESC,
          CASE WHEN :sortBy = 'AMOUNT_ASC' THEN amount END ASC,
          createdAt DESC
    """)
    fun filterExpenses(
        profileId: String,
        query: String = "",
        categoryId: String? = null,
        paymentMethodId: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        minAmount: BigDecimal? = null,
        maxAmount: BigDecimal? = null,
        sortBy: String = "DATE_DESC"
    ): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT * FROM expenses 
        WHERE profileId = :profileId 
          AND date >= :startDate AND date <= :endDate 
        ORDER BY date DESC, createdAt DESC
    """)
    fun getExpensesInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<List<ExpenseEntity>>

    @Query("""
        SELECT SUM(amount) FROM expenses 
        WHERE profileId = :profileId 
          AND date >= :startDate AND date <= :endDate
    """)
    fun observeTotalSpentInRange(profileId: String, startDate: LocalDate, endDate: LocalDate): Flow<BigDecimal?>

    @Query("""
        SELECT SUM(amount) FROM expenses 
        WHERE profileId = :profileId 
          AND categoryId = :categoryId
          AND date >= :startDate AND date <= :endDate
    """)
    fun observeCategoryTotalSpentInRange(profileId: String, categoryId: String, startDate: LocalDate, endDate: LocalDate): Flow<BigDecimal?>

    @Query("SELECT * FROM expenses WHERE profileId = :profileId ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentExpenses(profileId: String, limit: Int = 5): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE profileId = :profileId AND id = :expenseId")
    suspend fun deleteExpenseById(profileId: String, expenseId: String)
}

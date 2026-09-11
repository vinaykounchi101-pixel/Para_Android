package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE profileId = :profileId ORDER BY type ASC")
    fun getAllBudgets(profileId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND type = :type AND categoryId IS NULL LIMIT 1")
    fun getOverallBudget(profileId: String, type: String = "MONTHLY"): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND type = 'CATEGORY' AND categoryId = :categoryId LIMIT 1")
    fun getCategoryBudget(profileId: String, categoryId: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE profileId = :profileId AND id = :budgetId LIMIT 1")
    suspend fun getBudgetById(profileId: String, budgetId: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE profileId = :profileId AND id = :budgetId")
    suspend fun deleteBudgetById(profileId: String, budgetId: String)
}

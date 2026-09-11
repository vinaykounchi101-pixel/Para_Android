package com.paradox.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paradox.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY isDefault DESC, name ASC")
    fun getCategoriesByProfile(profileId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND id = :categoryId LIMIT 1")
    suspend fun getCategoryById(profileId: String, categoryId: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE profileId = :profileId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(profileId: String, name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM expenses WHERE profileId = :profileId AND categoryId = :categoryId")
    suspend fun getDependentExpenseCount(profileId: String, categoryId: String): Int

    @Query("UPDATE expenses SET categoryId = :targetCategoryId WHERE profileId = :profileId AND categoryId = :sourceCategoryId")
    suspend fun reassignExpensesCategory(profileId: String, sourceCategoryId: String, targetCategoryId: String)
}

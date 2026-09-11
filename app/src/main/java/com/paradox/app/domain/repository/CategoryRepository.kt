package com.paradox.app.domain.repository

import com.paradox.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(profileId: String): Flow<List<Category>>
    suspend fun getCategoryById(profileId: String, categoryId: String): Category?
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(profileId: String, categoryId: String, targetReassignCategoryId: String? = null)
    suspend fun getDependentExpenseCount(profileId: String, categoryId: String): Int
}

package com.paradox.app.data.repository

import com.paradox.app.data.local.dao.CategoryDao
import com.paradox.app.data.mapper.toDomain
import com.paradox.app.data.mapper.toEntity
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategories(profileId: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByProfile(profileId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getCategoryById(profileId: String, categoryId: String): Category? {
        return categoryDao.getCategoryById(profileId, categoryId)?.toDomain()
    }

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(profileId: String, categoryId: String, targetReassignCategoryId: String?) {
        if (targetReassignCategoryId != null) {
            categoryDao.reassignExpensesCategory(profileId, categoryId, targetReassignCategoryId)
        }
        val entity = categoryDao.getCategoryById(profileId, categoryId)
        if (entity != null) {
            categoryDao.deleteCategory(entity)
        }
    }

    override suspend fun getDependentExpenseCount(profileId: String, categoryId: String): Int {
        return categoryDao.getDependentExpenseCount(profileId, categoryId)
    }
}

package com.paradox.app.domain.usecase.category

import com.paradox.app.core.common.Result
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(profileId: String): Flow<List<Category>> = categoryRepository.getCategories(profileId)
}

class CreateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        profileId: String,
        name: String,
        iconName: String,
        colorHex: String
    ): Result<Category> {
        return try {
            if (name.isBlank()) {
                return Result.Error(IllegalArgumentException("Category name cannot be blank"))
            }

            val category = Category(
                id = "${profileId}_cat_" + UUID.randomUUID().toString().take(8),
                profileId = profileId,
                name = name.trim(),
                iconName = iconName,
                colorHex = colorHex,
                isCustom = true,
                isDefault = false
            )

            categoryRepository.addCategory(category)
            Result.Success(category)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class UpdateCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Result<Unit> {
        return try {
            if (category.name.isBlank()) {
                return Result.Error(IllegalArgumentException("Category name cannot be blank"))
            }
            categoryRepository.updateCategory(category)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

class DeleteCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        profileId: String,
        categoryId: String,
        targetReassignCategoryId: String? = null
    ): Result<Unit> {
        return try {
            val dependentCount = categoryRepository.getDependentExpenseCount(profileId, categoryId)
            if (dependentCount > 0 && targetReassignCategoryId == null) {
                return Result.Error(
                    IllegalStateException("Cannot delete category with $dependentCount expenses without specifying a reassignment category")
                )
            }
            categoryRepository.deleteCategory(profileId, categoryId, targetReassignCategoryId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}

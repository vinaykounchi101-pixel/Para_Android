package com.paradox.app.domain.usecase.category

import com.paradox.app.core.common.Result
import com.paradox.app.domain.repository.CategoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeleteCategoryUseCaseTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var deleteCategoryUseCase: DeleteCategoryUseCase

    @Before
    fun setup() {
        categoryRepository = mockk(relaxed = true)
        deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepository)
    }

    @Test
    fun `deleting category with no dependent expenses succeeds directly`() = runTest {
        coEvery { categoryRepository.getDependentExpenseCount("prof_1", "cat_1") } returns 0

        val result = deleteCategoryUseCase("prof_1", "cat_1", null)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.deleteCategory("prof_1", "cat_1", null) }
    }

    @Test
    fun `deleting category with dependent expenses fails if no reassignment provided`() = runTest {
        coEvery { categoryRepository.getDependentExpenseCount("prof_1", "cat_1") } returns 5

        val result = deleteCategoryUseCase("prof_1", "cat_1", null)

        assertTrue(result.isError)
        coVerify(exactly = 0) { categoryRepository.deleteCategory("prof_1", "cat_1", any()) }
    }

    @Test
    fun `deleting category with dependent expenses succeeds when reassignment target is provided`() = runTest {
        coEvery { categoryRepository.getDependentExpenseCount("prof_1", "cat_1") } returns 5

        val result = deleteCategoryUseCase("prof_1", "cat_1", "cat_other")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { categoryRepository.deleteCategory("prof_1", "cat_1", "cat_other") }
    }
}

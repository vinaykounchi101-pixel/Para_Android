package com.paradox.app.feature.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.usecase.category.CreateCategoryUseCase
import com.paradox.app.domain.usecase.category.DeleteCategoryUseCase
import com.paradox.app.domain.usecase.category.GetCategoriesUseCase
import com.paradox.app.domain.usecase.category.UpdateCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface CategoryEvent {
    data class ShowToast(val message: String) : CategoryEvent
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val createCategoryUseCase: CreateCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CategoryEvent>()
    val events: SharedFlow<CategoryEvent> = _events.asSharedFlow()

    init {
        observeCategories()
    }

    private fun observeCategories() {
        viewModelScope.launch {
            sessionDataStore.activeProfileId.flatMapLatest { profileId ->
                if (profileId == null) flowOf(emptyList()) else getCategoriesUseCase(profileId)
            }.collect { list ->
                _uiState.update {
                    it.copy(
                        categories = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createCategory(name: String, colorHex: String = "#5B7C99") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch
            val result = createCategoryUseCase(
                profileId = profileId,
                name = name.trim(),
                iconName = "category",
                colorHex = colorHex
            )
            if (result is Result.Error) {
                _events.emit(CategoryEvent.ShowToast(result.message ?: "Failed to add category"))
            }
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            updateCategoryUseCase(category)
        }
    }

    fun deleteCategory(categoryId: String, targetReassignCategoryId: String? = null) {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch
            val result = deleteCategoryUseCase(profileId, categoryId, targetReassignCategoryId)
            if (result is Result.Error) {
                _events.emit(CategoryEvent.ShowToast(result.message ?: "Cannot delete category"))
            }
        }
    }
}

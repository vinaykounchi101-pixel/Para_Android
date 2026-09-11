package com.paradox.app.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.ExpenseSortOrder
import com.paradox.app.domain.model.LedgerFilter
import com.paradox.app.domain.usecase.category.GetCategoriesUseCase
import com.paradox.app.domain.usecase.expense.GetExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LedgerUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: LedgerFilter = LedgerFilter(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class LedgerViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val getExpensesUseCase: GetExpensesUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LedgerUiState())
    val uiState: StateFlow<LedgerUiState> = _uiState.asStateFlow()

    init {
        observeLedger()
    }

    private fun observeLedger() {
        viewModelScope.launch {
            sessionDataStore.activeProfileId.flatMapLatest { profileId ->
                if (profileId == null) {
                    flowOf(emptyList())
                } else {
                    getCategoriesUseCase(profileId)
                }
            }.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }

        viewModelScope.launch {
            _uiState.flatMapLatest { state ->
                sessionDataStore.activeProfileId.flatMapLatest { profileId ->
                    if (profileId == null) {
                        flowOf(emptyList())
                    } else {
                        getExpensesUseCase(profileId, state.filter)
                    }
                }
            }.collect { expensesList ->
                _uiState.update {
                    it.copy(
                        expenses = expensesList,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(filter = it.filter.copy(query = query)) }
    }

    fun onCategoryFilterToggled(categoryId: String?) {
        val newCatId = if (_uiState.value.filter.categoryId == categoryId) null else categoryId
        _uiState.update { it.copy(filter = it.filter.copy(categoryId = newCatId)) }
    }

    fun onSortOrderChanged(sortOrder: ExpenseSortOrder) {
        _uiState.update { it.copy(filter = it.filter.copy(sortBy = sortOrder)) }
    }
}

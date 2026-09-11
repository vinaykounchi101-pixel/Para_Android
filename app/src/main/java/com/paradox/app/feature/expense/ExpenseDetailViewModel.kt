package com.paradox.app.feature.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.usecase.category.GetCategoriesUseCase
import com.paradox.app.domain.usecase.expense.DeleteExpenseUseCase
import com.paradox.app.domain.usecase.expense.GetExpenseByIdUseCase
import com.paradox.app.domain.usecase.paymentmethod.GetPaymentMethodsUseCase
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

data class ExpenseDetailUiState(
    val expense: Expense? = null,
    val category: Category? = null,
    val paymentMethod: PaymentMethod? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ExpenseDetailEvent {
    data object NavigateBack : ExpenseDetailEvent
}

@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDataStore: SessionDataStore,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase
) : ViewModel() {

    private val expenseId: String = checkNotNull(savedStateHandle["expenseId"])

    private val _uiState = MutableStateFlow(ExpenseDetailUiState())
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExpenseDetailEvent>()
    val events: SharedFlow<ExpenseDetailEvent> = _events.asSharedFlow()

    init {
        observeExpenseDetail()
    }

    private fun observeExpenseDetail() {
        viewModelScope.launch {
            sessionDataStore.activeProfileId.flatMapLatest { profileId ->
                if (profileId == null) {
                    flowOf(null)
                } else {
                    getExpenseByIdUseCase.observe(profileId, expenseId)
                }
            }.collect { expense ->
                if (expense == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Expense not found") }
                    return@collect
                }

                val profileId = expense.profileId
                val categories = getCategoriesUseCase(profileId).firstOrNull() ?: emptyList()
                val paymentMethods = getPaymentMethodsUseCase(profileId).firstOrNull() ?: emptyList()

                val category = categories.find { it.id == expense.categoryId }
                val paymentMethod = paymentMethods.find { it.id == expense.paymentMethodId }

                _uiState.update {
                    it.copy(
                        expense = expense,
                        category = category,
                        paymentMethod = paymentMethod,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch
            deleteExpenseUseCase(profileId, expenseId)
            _events.emit(ExpenseDetailEvent.NavigateBack)
        }
    }
}

package com.paradox.app.feature.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.usecase.category.GetCategoriesUseCase
import com.paradox.app.domain.usecase.expense.AddExpenseUseCase
import com.paradox.app.domain.usecase.expense.GetExpenseByIdUseCase
import com.paradox.app.domain.usecase.expense.UpdateExpenseUseCase
import com.paradox.app.domain.usecase.paymentmethod.GetPaymentMethodsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class AddEditExpenseUiState(
    val isEditMode: Boolean = false,
    val expenseId: String? = null,
    val title: String = "",
    val amountInput: String = "",
    val currencyCode: String = "INR",
    val selectedCategoryId: String = "",
    val selectedPaymentMethodId: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface AddEditExpenseEvent {
    data object NavigateBack : AddEditExpenseEvent
    data class ShowToast(val message: String) : AddEditExpenseEvent
}

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDataStore: SessionDataStore,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase
) : ViewModel() {

    private val expenseIdArg: String? = savedStateHandle.get<String>("expenseId")

    private val _uiState = MutableStateFlow(AddEditExpenseUiState())
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEditExpenseEvent>()
    val events: SharedFlow<AddEditExpenseEvent> = _events.asSharedFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch

            val categories = getCategoriesUseCase(profileId).firstOrNull() ?: emptyList()
            val paymentMethods = getPaymentMethodsUseCase(profileId).firstOrNull() ?: emptyList()
            val currency = sessionDataStore.preferredCurrency.firstOrNull() ?: "INR"

            if (!expenseIdArg.isNullOrBlank()) {
                val existing = getExpenseByIdUseCase(profileId, expenseIdArg)
                if (existing != null) {
                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            expenseId = existing.id,
                            title = existing.title,
                            amountInput = existing.money.amount.toPlainString(),
                            currencyCode = existing.money.currencyCode,
                            selectedCategoryId = existing.categoryId,
                            selectedPaymentMethodId = existing.paymentMethodId,
                            selectedDate = existing.date,
                            notes = existing.notes ?: "",
                            categories = categories,
                            paymentMethods = paymentMethods,
                            isInitialLoading = false
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    categories = categories,
                    paymentMethods = paymentMethods,
                    currencyCode = currency,
                    selectedCategoryId = categories.firstOrNull()?.id ?: "",
                    selectedPaymentMethodId = paymentMethods.firstOrNull()?.id ?: "",
                    isInitialLoading = false
                )
            }
        }
    }

    fun onTitleChanged(title: String) = _uiState.update { it.copy(title = title, errorMessage = null) }
    fun onAmountChanged(amount: String) = _uiState.update { it.copy(amountInput = amount, errorMessage = null) }
    fun onCategorySelected(categoryId: String) = _uiState.update { it.copy(selectedCategoryId = categoryId) }
    fun onPaymentMethodSelected(pmId: String) = _uiState.update { it.copy(selectedPaymentMethodId = pmId) }
    fun onDateSelected(date: LocalDate) = _uiState.update { it.copy(selectedDate = date, errorMessage = null) }
    fun onNotesChanged(notes: String) = _uiState.update { it.copy(notes = notes) }

    fun saveExpense() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an expense title") }
            return
        }

        val parsedAmount = try {
            BigDecimal(state.amountInput.trim())
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "Invalid amount entered") }
            return
        }

        if (parsedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            _uiState.update { it.copy(errorMessage = "Amount must be greater than zero") }
            return
        }

        if (state.selectedDate.isAfter(LocalDate.now())) {
            _uiState.update { it.copy(errorMessage = "Expense date cannot be in the future") }
            return
        }

        if (state.selectedCategoryId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select a category") }
            return
        }

        if (state.selectedPaymentMethodId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please select a payment method") }
            return
        }

        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val money = Money(parsedAmount, state.currencyCode)

            val result = if (state.isEditMode && state.expenseId != null) {
                val expense = Expense(
                    id = state.expenseId,
                    profileId = profileId,
                    title = state.title,
                    money = money,
                    categoryId = state.selectedCategoryId,
                    paymentMethodId = state.selectedPaymentMethodId,
                    date = state.selectedDate,
                    notes = state.notes.ifBlank { null }
                )
                updateExpenseUseCase(expense)
            } else {
                addExpenseUseCase(
                    profileId = profileId,
                    title = state.title,
                    money = money,
                    categoryId = state.selectedCategoryId,
                    paymentMethodId = state.selectedPaymentMethodId,
                    date = state.selectedDate,
                    notes = state.notes.ifBlank { null }
                )
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(AddEditExpenseEvent.NavigateBack)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Failed to save expense"
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }
}

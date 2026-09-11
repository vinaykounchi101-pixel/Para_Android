package com.paradox.app.feature.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.model.PaymentMethod
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.usecase.category.GetCategoriesUseCase
import com.paradox.app.domain.usecase.paymentmethod.GetPaymentMethodsUseCase
import com.paradox.app.domain.usecase.recurring.AddRecurringExpenseUseCase
import com.paradox.app.domain.usecase.recurring.CalculateMonthlyCommitmentUseCase
import com.paradox.app.domain.usecase.recurring.DeleteRecurringExpenseUseCase
import com.paradox.app.domain.usecase.recurring.GetRecurringExpensesUseCase
import com.paradox.app.domain.usecase.recurring.UpdateRecurringExpenseUseCase
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

data class RecurringListUiState(
    val recurringList: List<RecurringExpense> = emptyList(),
    val monthlyCommitment: Money = Money(BigDecimal.ZERO, "INR"),
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class AddEditRecurringUiState(
    val isEditMode: Boolean = false,
    val recurringId: String? = null,
    val title: String = "",
    val amountInput: String = "",
    val currencyCode: String = "INR",
    val selectedCategoryId: String = "",
    val selectedPaymentMethodId: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val startDate: LocalDate = LocalDate.now(),
    val isActive: Boolean = true,
    val notes: String = "",
    val categories: List<Category> = emptyList(),
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface RecurringEvent {
    data object NavigateBack : RecurringEvent
    data class ShowToast(val message: String) : RecurringEvent
}

@HiltViewModel
class RecurringViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDataStore: SessionDataStore,
    private val getRecurringExpensesUseCase: GetRecurringExpensesUseCase,
    private val addRecurringExpenseUseCase: AddRecurringExpenseUseCase,
    private val updateRecurringExpenseUseCase: UpdateRecurringExpenseUseCase,
    private val deleteRecurringExpenseUseCase: DeleteRecurringExpenseUseCase,
    private val calculateMonthlyCommitmentUseCase: CalculateMonthlyCommitmentUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getPaymentMethodsUseCase: GetPaymentMethodsUseCase,
    private val recurringRepository: RecurringExpenseRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(RecurringListUiState())
    val listState: StateFlow<RecurringListUiState> = _listState.asStateFlow()

    private val _editState = MutableStateFlow(AddEditRecurringUiState())
    val editState: StateFlow<AddEditRecurringUiState> = _editState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<RecurringEvent>()
    val eventFlow: SharedFlow<RecurringEvent> = _eventFlow.asSharedFlow()

    private var currentProfileId: String? = null

    init {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull()
            currentProfileId = profileId
            if (profileId != null) {
                loadCategoriesAndPaymentMethods(profileId)
                loadRecurringList(profileId)
                loadMonthlyCommitment(profileId)
            }
        }
    }

    private fun loadCategoriesAndPaymentMethods(profileId: String) {
        viewModelScope.launch {
            getCategoriesUseCase(profileId).collect { categories ->
                _listState.update { it.copy(categories = categories) }
                _editState.update { state ->
                    val selectedCategory = if (state.selectedCategoryId.isBlank() && categories.isNotEmpty()) {
                        categories.first().id
                    } else state.selectedCategoryId
                    state.copy(categories = categories, selectedCategoryId = selectedCategory)
                }
            }
        }
        viewModelScope.launch {
            getPaymentMethodsUseCase(profileId).collect { paymentMethods ->
                _listState.update { it.copy(paymentMethods = paymentMethods) }
                _editState.update { state ->
                    val selectedMethod = if (state.selectedPaymentMethodId.isBlank() && paymentMethods.isNotEmpty()) {
                        paymentMethods.first().id
                    } else state.selectedPaymentMethodId
                    state.copy(paymentMethods = paymentMethods, selectedPaymentMethodId = selectedMethod)
                }
            }
        }
    }

    private fun loadRecurringList(profileId: String) {
        viewModelScope.launch {
            getRecurringExpensesUseCase(profileId).collect { list ->
                _listState.update {
                    it.copy(
                        recurringList = list,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun loadMonthlyCommitment(profileId: String) {
        viewModelScope.launch {
            calculateMonthlyCommitmentUseCase(profileId).collect { commitment ->
                _listState.update { it.copy(monthlyCommitment = commitment) }
            }
        }
    }

    fun initAddEdit(recurringId: String?) {
        if (recurringId != null && recurringId.isNotBlank()) {
            _editState.update { it.copy(isEditMode = true, recurringId = recurringId, isInitialLoading = true) }
            val profileId = currentProfileId ?: return
            viewModelScope.launch {
                val item = recurringRepository.getRecurringById(profileId, recurringId)
                if (item != null) {
                    _editState.update {
                        it.copy(
                            title = item.title,
                            amountInput = item.amount.amount.toPlainString(),
                            currencyCode = item.currency,
                            selectedCategoryId = item.categoryId,
                            selectedPaymentMethodId = item.paymentMethodId,
                            frequency = item.frequency,
                            startDate = item.startDate,
                            isActive = item.isActive,
                            notes = item.notes ?: "",
                            isInitialLoading = false
                        )
                    }
                } else {
                    _editState.update { it.copy(isInitialLoading = false, errorMessage = "Subscription not found") }
                }
            }
        } else {
            _editState.update { AddEditRecurringUiState() }
            currentProfileId?.let { loadCategoriesAndPaymentMethods(it) }
        }
    }

    fun onTitleChange(title: String) {
        _editState.update { it.copy(title = title, errorMessage = null) }
    }

    fun onAmountChange(amount: String) {
        _editState.update { it.copy(amountInput = amount, errorMessage = null) }
    }

    fun onCategorySelected(categoryId: String) {
        _editState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onPaymentMethodSelected(paymentMethodId: String) {
        _editState.update { it.copy(selectedPaymentMethodId = paymentMethodId) }
    }

    fun onFrequencySelected(frequency: RecurringFrequency) {
        _editState.update { it.copy(frequency = frequency) }
    }

    fun onStartDateSelected(startDate: LocalDate) {
        _editState.update { it.copy(startDate = startDate) }
    }

    fun onNotesChange(notes: String) {
        _editState.update { it.copy(notes = notes) }
    }

    fun onActiveToggled(isActive: Boolean) {
        _editState.update { it.copy(isActive = isActive) }
    }

    fun saveRecurring() {
        val profileId = currentProfileId ?: return
        val state = _editState.value

        if (state.title.isBlank()) {
            _editState.update { it.copy(errorMessage = "Please enter a title") }
            return
        }

        val amountDecimal = state.amountInput.toBigDecimalOrNull()
        if (amountDecimal == null || amountDecimal <= BigDecimal.ZERO) {
            _editState.update { it.copy(errorMessage = "Please enter a valid amount greater than 0") }
            return
        }

        if (state.selectedCategoryId.isBlank()) {
            _editState.update { it.copy(errorMessage = "Please select a category") }
            return
        }

        if (state.selectedPaymentMethodId.isBlank()) {
            _editState.update { it.copy(errorMessage = "Please select a payment method") }
            return
        }

        viewModelScope.launch {
            _editState.update { it.copy(isLoading = true, errorMessage = null) }
            val money = Money(amountDecimal, state.currencyCode)

            if (state.isEditMode && state.recurringId != null) {
                val existing = recurringRepository.getRecurringById(profileId, state.recurringId)
                if (existing != null) {
                    val updated = existing.copy(
                        title = state.title.trim(),
                        amount = money,
                        currency = state.currencyCode,
                        categoryId = state.selectedCategoryId,
                        paymentMethodId = state.selectedPaymentMethodId,
                        frequency = state.frequency,
                        startDate = state.startDate,
                        isActive = state.isActive,
                        notes = state.notes.trim().ifBlank { null }
                    )
                    when (val result = updateRecurringExpenseUseCase(updated)) {
                        is Result.Success -> {
                            _eventFlow.emit(RecurringEvent.ShowToast("Subscription updated"))
                            _eventFlow.emit(RecurringEvent.NavigateBack)
                        }
                        is Result.Error -> {
                            _editState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                        }
                        is Result.Loading -> Unit
                    }
                }
            } else {
                when (val result = addRecurringExpenseUseCase(
                    profileId = profileId,
                    title = state.title,
                    amount = money,
                    currency = state.currencyCode,
                    categoryId = state.selectedCategoryId,
                    paymentMethodId = state.selectedPaymentMethodId,
                    frequency = state.frequency,
                    startDate = state.startDate,
                    notes = state.notes
                )) {
                    is Result.Success -> {
                        _eventFlow.emit(RecurringEvent.ShowToast("Subscription added"))
                        _eventFlow.emit(RecurringEvent.NavigateBack)
                    }
                    is Result.Error -> {
                        _editState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                    }
                    is Result.Loading -> Unit
                }
            }
        }
    }

    fun deleteRecurring(id: String) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            when (val result = deleteRecurringExpenseUseCase(profileId, id)) {
                is Result.Success -> {
                    _eventFlow.emit(RecurringEvent.ShowToast("Subscription removed"))
                }
                is Result.Error -> {
                    _eventFlow.emit(RecurringEvent.ShowToast(result.exception.message ?: "Failed to remove subscription"))
                }
                is Result.Loading -> Unit
            }
        }
    }
}

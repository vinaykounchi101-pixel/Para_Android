package com.paradox.app.feature.income

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.CashFlowSummary
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.usecase.income.AddIncomeUseCase
import com.paradox.app.domain.usecase.income.CalculateCashFlowUseCase
import com.paradox.app.domain.usecase.income.DeleteIncomeUseCase
import com.paradox.app.domain.usecase.income.GetIncomesUseCase
import com.paradox.app.domain.usecase.income.UpdateIncomeUseCase
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
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class IncomeListUiState(
    val incomes: List<Income> = emptyList(),
    val selectedSource: IncomeSource? = null,
    val cashFlowSummary: CashFlowSummary? = null,
    val currentMonth: LocalDate = LocalDate.now(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class AddEditIncomeUiState(
    val isEditMode: Boolean = false,
    val incomeId: String? = null,
    val amountInput: String = "",
    val currencyCode: String = "INR",
    val selectedSource: IncomeSource = IncomeSource.SALARY,
    val selectedDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface IncomeEvent {
    data object NavigateBack : IncomeEvent
    data class ShowToast(val message: String) : IncomeEvent
}

@HiltViewModel
class IncomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDataStore: SessionDataStore,
    private val getIncomesUseCase: GetIncomesUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val updateIncomeUseCase: UpdateIncomeUseCase,
    private val deleteIncomeUseCase: DeleteIncomeUseCase,
    private val calculateCashFlowUseCase: CalculateCashFlowUseCase,
    private val incomeRepository: IncomeRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(IncomeListUiState())
    val listState: StateFlow<IncomeListUiState> = _listState.asStateFlow()

    private val _editState = MutableStateFlow(AddEditIncomeUiState())
    val editState: StateFlow<AddEditIncomeUiState> = _editState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<IncomeEvent>()
    val eventFlow: SharedFlow<IncomeEvent> = _eventFlow.asSharedFlow()

    private var currentProfileId: String? = null

    init {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull()
            currentProfileId = profileId
            if (profileId != null) {
                loadIncomes(profileId)
                loadCashFlowSummary(profileId)
            }
        }
    }

    fun selectSourceFilter(source: IncomeSource?) {
        _listState.update { it.copy(selectedSource = source) }
        currentProfileId?.let { loadIncomes(it) }
    }

    private fun loadIncomes(profileId: String) {
        val selectedSource = _listState.value.selectedSource
        val startOfMonth = _listState.value.currentMonth.with(TemporalAdjusters.firstDayOfMonth())
        val endOfMonth = _listState.value.currentMonth.with(TemporalAdjusters.lastDayOfMonth())

        viewModelScope.launch {
            getIncomesUseCase(
                profileId = profileId,
                source = selectedSource,
                startDate = startOfMonth,
                endDate = endOfMonth
            ).collect { incomesList ->
                _listState.update {
                    it.copy(
                        incomes = incomesList,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun loadCashFlowSummary(profileId: String) {
        val startOfMonth = _listState.value.currentMonth.with(TemporalAdjusters.firstDayOfMonth())
        val endOfMonth = _listState.value.currentMonth.with(TemporalAdjusters.lastDayOfMonth())

        viewModelScope.launch {
            calculateCashFlowUseCase(profileId, startOfMonth, endOfMonth).collect { summary ->
                _listState.update { it.copy(cashFlowSummary = summary) }
            }
        }
    }

    fun deleteIncome(incomeId: String) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            when (val result = deleteIncomeUseCase(profileId, incomeId)) {
                is Result.Success -> {
                    _eventFlow.emit(IncomeEvent.ShowToast("Income deleted"))
                }
                is Result.Error -> {
                    _eventFlow.emit(IncomeEvent.ShowToast(result.exception.message ?: "Failed to delete income"))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun initAddEdit(incomeId: String?) {
        if (incomeId != null && incomeId.isNotBlank()) {
            _editState.update { it.copy(isEditMode = true, incomeId = incomeId, isInitialLoading = true) }
            val profileId = currentProfileId ?: return
            viewModelScope.launch {
                val income = incomeRepository.getIncomeById(profileId, incomeId)
                if (income != null) {
                    _editState.update {
                        it.copy(
                            amountInput = income.amount.amount.toPlainString(),
                            currencyCode = income.currency,
                            selectedSource = income.source,
                            selectedDate = income.date,
                            notes = income.notes ?: "",
                            isInitialLoading = false
                        )
                    }
                } else {
                    _editState.update { it.copy(isInitialLoading = false, errorMessage = "Income not found") }
                }
            }
        } else {
            _editState.update { AddEditIncomeUiState() }
        }
    }

    fun onAmountChange(amount: String) {
        _editState.update { it.copy(amountInput = amount, errorMessage = null) }
    }

    fun onSourceSelected(source: IncomeSource) {
        _editState.update { it.copy(selectedSource = source) }
    }

    fun onDateSelected(date: LocalDate) {
        _editState.update { it.copy(selectedDate = date) }
    }

    fun onNotesChange(notes: String) {
        _editState.update { it.copy(notes = notes) }
    }

    fun saveIncome() {
        val profileId = currentProfileId ?: return
        val state = _editState.value

        val amountDecimal = state.amountInput.toBigDecimalOrNull()
        if (amountDecimal == null || amountDecimal <= BigDecimal.ZERO) {
            _editState.update { it.copy(errorMessage = "Please enter a valid amount greater than 0") }
            return
        }

        viewModelScope.launch {
            _editState.update { it.copy(isLoading = true, errorMessage = null) }
            val money = Money(amountDecimal, state.currencyCode)

            if (state.isEditMode && state.incomeId != null) {
                val existing = incomeRepository.getIncomeById(profileId, state.incomeId)
                if (existing != null) {
                    val updated = existing.copy(
                        amount = money,
                        currency = state.currencyCode,
                        source = state.selectedSource,
                        date = state.selectedDate,
                        notes = state.notes.trim().ifBlank { null }
                    )
                    when (val result = updateIncomeUseCase(updated)) {
                        is Result.Success -> {
                            _eventFlow.emit(IncomeEvent.ShowToast("Income updated"))
                            _eventFlow.emit(IncomeEvent.NavigateBack)
                        }
                        is Result.Error -> {
                            _editState.update {
                                it.copy(isLoading = false, errorMessage = result.exception.message)
                            }
                        }
                        is Result.Loading -> Unit
                    }
                }
            } else {
                when (val result = addIncomeUseCase(
                    profileId = profileId,
                    source = state.selectedSource,
                    money = money,
                    currency = state.currencyCode,
                    date = state.selectedDate,
                    notes = state.notes
                )) {
                    is Result.Success -> {
                        _eventFlow.emit(IncomeEvent.ShowToast("Income recorded"))
                        _eventFlow.emit(IncomeEvent.NavigateBack)
                    }
                    is Result.Error -> {
                        _editState.update {
                            it.copy(isLoading = false, errorMessage = result.exception.message)
                        }
                    }
                    is Result.Loading -> Unit
                }
            }
        }
    }
}

package com.paradox.app.feature.savingsgoal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.SavingsContribution
import com.paradox.app.domain.model.SavingsGoal
import com.paradox.app.domain.repository.SavingsGoalRepository
import com.paradox.app.domain.usecase.savingsgoal.AddSavingsGoalUseCase
import com.paradox.app.domain.usecase.savingsgoal.ContributeToGoalUseCase
import com.paradox.app.domain.usecase.savingsgoal.DeleteSavingsGoalUseCase
import com.paradox.app.domain.usecase.savingsgoal.GetSavingsGoalsUseCase
import com.paradox.app.domain.usecase.savingsgoal.UpdateSavingsGoalUseCase
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

data class SavingsGoalListUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class AddEditSavingsGoalUiState(
    val isEditMode: Boolean = false,
    val goalId: String? = null,
    val name: String = "",
    val targetAmountInput: String = "",
    val currentAmountInput: String = "0.00",
    val currency: String = "INR",
    val targetDate: LocalDate = LocalDate.now().plusMonths(6),
    val colorHex: String = "#10B981",
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SavingsGoalEvent {
    data object NavigateBack : SavingsGoalEvent
    data class ShowToast(val message: String) : SavingsGoalEvent
}

@HiltViewModel
class SavingsGoalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDataStore: SessionDataStore,
    private val getSavingsGoalsUseCase: GetSavingsGoalsUseCase,
    private val addSavingsGoalUseCase: AddSavingsGoalUseCase,
    private val updateSavingsGoalUseCase: UpdateSavingsGoalUseCase,
    private val deleteSavingsGoalUseCase: DeleteSavingsGoalUseCase,
    private val contributeToGoalUseCase: ContributeToGoalUseCase,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(SavingsGoalListUiState())
    val listState: StateFlow<SavingsGoalListUiState> = _listState.asStateFlow()

    private val _editState = MutableStateFlow(AddEditSavingsGoalUiState())
    val editState: StateFlow<AddEditSavingsGoalUiState> = _editState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<SavingsGoalEvent>()
    val eventFlow: SharedFlow<SavingsGoalEvent> = _eventFlow.asSharedFlow()

    private var currentProfileId: String? = null

    init {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull()
            currentProfileId = profileId
            if (profileId != null) {
                loadGoals(profileId)
            }
        }
    }

    private fun loadGoals(profileId: String) {
        viewModelScope.launch {
            getSavingsGoalsUseCase(profileId).collect { list ->
                _listState.update {
                    it.copy(
                        goals = list,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun initAddEdit(goalId: String?) {
        if (goalId != null && goalId.isNotBlank()) {
            _editState.update { it.copy(isEditMode = true, goalId = goalId, isInitialLoading = true) }
            val profileId = currentProfileId ?: return
            viewModelScope.launch {
                val goal = savingsGoalRepository.getGoalById(profileId, goalId)
                if (goal != null) {
                    _editState.update {
                        it.copy(
                            name = goal.name,
                            targetAmountInput = goal.targetAmount.amount.toPlainString(),
                            currentAmountInput = goal.currentAmount.amount.toPlainString(),
                            currency = goal.currency,
                            targetDate = goal.targetDate,
                            colorHex = goal.colorHex,
                            isInitialLoading = false
                        )
                    }
                } else {
                    _editState.update { it.copy(isInitialLoading = false, errorMessage = "Goal not found") }
                }
            }
        } else {
            _editState.update { AddEditSavingsGoalUiState() }
        }
    }

    fun onNameChange(name: String) {
        _editState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onTargetAmountChange(amount: String) {
        _editState.update { it.copy(targetAmountInput = amount, errorMessage = null) }
    }

    fun onTargetDateSelected(date: LocalDate) {
        _editState.update { it.copy(targetDate = date) }
    }

    fun onColorChange(colorHex: String) {
        _editState.update { it.copy(colorHex = colorHex) }
    }

    fun saveGoal() {
        val profileId = currentProfileId ?: return
        val state = _editState.value

        if (state.name.isBlank()) {
            _editState.update { it.copy(errorMessage = "Please enter a goal name") }
            return
        }

        val targetDecimal = state.targetAmountInput.toBigDecimalOrNull()
        if (targetDecimal == null || targetDecimal <= BigDecimal.ZERO) {
            _editState.update { it.copy(errorMessage = "Please enter a valid target amount greater than 0") }
            return
        }

        val currentDecimal = state.currentAmountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO

        viewModelScope.launch {
            _editState.update { it.copy(isLoading = true, errorMessage = null) }
            val targetMoney = Money(targetDecimal, state.currency)
            val currentMoney = Money(currentDecimal, state.currency)

            if (state.isEditMode && state.goalId != null) {
                val existing = savingsGoalRepository.getGoalById(profileId, state.goalId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = state.name.trim(),
                        targetAmount = targetMoney,
                        currency = state.currency,
                        targetDate = state.targetDate,
                        colorHex = state.colorHex
                    )
                    when (val result = updateSavingsGoalUseCase(updated)) {
                        is Result.Success -> {
                            _eventFlow.emit(SavingsGoalEvent.ShowToast("Goal updated"))
                            _eventFlow.emit(SavingsGoalEvent.NavigateBack)
                        }
                        is Result.Error -> {
                            _editState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                        }
                        is Result.Loading -> Unit
                    }
                }
            } else {
                when (val result = addSavingsGoalUseCase(
                    profileId = profileId,
                    name = state.name,
                    targetAmount = targetMoney,
                    currentAmount = currentMoney,
                    targetDate = state.targetDate,
                    colorHex = state.colorHex
                )) {
                    is Result.Success -> {
                        _eventFlow.emit(SavingsGoalEvent.ShowToast("Goal created"))
                        _eventFlow.emit(SavingsGoalEvent.NavigateBack)
                    }
                    is Result.Error -> {
                        _editState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                    }
                    is Result.Loading -> Unit
                }
            }
        }
    }

    fun contributeToGoal(goalId: String, amountDecimal: BigDecimal) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            val money = Money(amountDecimal, "INR")
            when (val result = contributeToGoalUseCase(profileId, goalId, money)) {
                is Result.Success -> {
                    _eventFlow.emit(SavingsGoalEvent.ShowToast("Contribution recorded! 🎉"))
                }
                is Result.Error -> {
                    _eventFlow.emit(SavingsGoalEvent.ShowToast(result.exception.message ?: "Failed to add contribution"))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun deleteGoal(goalId: String) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            when (val result = deleteSavingsGoalUseCase(profileId, goalId)) {
                is Result.Success -> {
                    _eventFlow.emit(SavingsGoalEvent.ShowToast("Goal deleted"))
                }
                is Result.Error -> {
                    _eventFlow.emit(SavingsGoalEvent.ShowToast(result.exception.message ?: "Failed to delete goal"))
                }
                is Result.Loading -> Unit
            }
        }
    }
}

package com.paradox.app.feature.engagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.engagement.LoggingStreak
import com.paradox.app.domain.model.engagement.MonthlyDigest
import com.paradox.app.domain.model.engagement.SplitExpenseResult
import com.paradox.app.domain.usecase.engagement.CalculateSplitExpenseUseCase
import com.paradox.app.domain.usecase.engagement.GenerateMonthlyDigestUseCase
import com.paradox.app.domain.usecase.engagement.TrackLoggingStreakUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class EngagementUiState(
    val isLoading: Boolean = true,
    val isAiEnabled: Boolean = false,
    val digest: MonthlyDigest? = null,
    val streak: LoggingStreak? = null,
    val splitResult: SplitExpenseResult? = null,
    val splitAmountInput: String = "",
    val splitPeopleInput: String = "2",
    val splitTipInput: String = "0"
)

@HiltViewModel
class EngagementViewModel @Inject constructor(
    private val generateMonthlyDigestUseCase: GenerateMonthlyDigestUseCase,
    private val trackLoggingStreakUseCase: TrackLoggingStreakUseCase,
    private val calculateSplitExpenseUseCase: CalculateSplitExpenseUseCase,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EngagementUiState())
    val uiState: StateFlow<EngagementUiState> = _uiState.asStateFlow()

    init {
        observeAiState()
        loadData()
    }

    private fun observeAiState() {
        viewModelScope.launch {
            sessionDataStore.isAiEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(isAiEnabled = enabled)
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""

            val digest = generateMonthlyDigestUseCase(profileId)
            val streak = trackLoggingStreakUseCase(profileId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                digest = digest,
                streak = streak
            )
        }
    }

    fun updateSplitAmount(amount: String) {
        _uiState.value = _uiState.value.copy(splitAmountInput = amount)
        recomputeSplit()
    }

    fun updateSplitPeople(people: String) {
        _uiState.value = _uiState.value.copy(splitPeopleInput = people)
        recomputeSplit()
    }

    fun updateSplitTip(tip: String) {
        _uiState.value = _uiState.value.copy(splitTipInput = tip)
        recomputeSplit()
    }

    private fun recomputeSplit() {
        val amountStr = _uiState.value.splitAmountInput.trim()
        val peopleStr = _uiState.value.splitPeopleInput.trim()
        val tipStr = _uiState.value.splitTipInput.trim()

        if (amountStr.isBlank()) {
            _uiState.value = _uiState.value.copy(splitResult = null)
            return
        }

        try {
            val amount = BigDecimal(amountStr.replace(",", ""))
            val people = peopleStr.toIntOrNull() ?: 2
            val tip = if (tipStr.isNotBlank()) BigDecimal(tipStr.replace(",", "")) else BigDecimal.ZERO

            if (people >= 1 && amount > BigDecimal.ZERO) {
                val result = calculateSplitExpenseUseCase(
                    totalAmount = Money.of(amount, "INR"),
                    peopleCount = people,
                    tipAmount = Money.of(tip, "INR")
                )
                _uiState.value = _uiState.value.copy(splitResult = result)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(splitResult = null)
        }
    }
}

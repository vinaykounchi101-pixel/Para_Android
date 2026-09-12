package com.paradox.app.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.intelligence.FinancialHealthScore
import com.paradox.app.domain.model.intelligence.PurchaseSimulationResult
import com.paradox.app.domain.model.intelligence.SafeToSpendResult
import com.paradox.app.domain.model.intelligence.SpendingForecast
import com.paradox.app.domain.model.intelligence.SpendingLeak
import com.paradox.app.domain.usecase.insights.CalculateFinancialHealthScoreUseCase
import com.paradox.app.domain.usecase.insights.CalculateSafeToSpendUseCase
import com.paradox.app.domain.usecase.insights.DetectSpendingLeaksUseCase
import com.paradox.app.domain.usecase.insights.ForecastSpendingUseCase
import com.paradox.app.domain.usecase.insights.SimulatePurchaseUseCase
import com.paradox.app.domain.repository.AiSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val isAiEnabled: Boolean = false,
    val safeToSpend: SafeToSpendResult? = null,
    val healthScore: FinancialHealthScore? = null,
    val leaks: List<SpendingLeak> = emptyList(),
    val forecast: SpendingForecast? = null,
    val simulationResult: PurchaseSimulationResult? = null,
    val simulatedAmountInput: String = ""
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val calculateSafeToSpendUseCase: CalculateSafeToSpendUseCase,
    private val calculateHealthScoreUseCase: CalculateFinancialHealthScoreUseCase,
    private val detectSpendingLeaksUseCase: DetectSpendingLeaksUseCase,
    private val forecastSpendingUseCase: ForecastSpendingUseCase,
    private val simulatePurchaseUseCase: SimulatePurchaseUseCase,
    private val sessionDataStore: SessionDataStore,
    private val aiSettingsRepository: AiSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            aiSettingsRepository.isAiEnabled.collect { enabled ->
                _uiState.update { it.copy(isAiEnabled = enabled) }
                if (enabled) {
                    loadAllInsights()
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            safeToSpend = null,
                            healthScore = null,
                            leaks = emptyList(),
                            forecast = null,
                            simulationResult = null
                        ) 
                    }
                }
            }
        }
    }

    fun loadAllInsights() {
        viewModelScope.launch {
            val isEnabled = _uiState.value.isAiEnabled
            if (!isEnabled) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""

            val safe = calculateSafeToSpendUseCase(profileId)
            val health = calculateHealthScoreUseCase(profileId)
            val leaks = detectSpendingLeaksUseCase(profileId)
            val forecast = forecastSpendingUseCase(profileId)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                safeToSpend = safe,
                healthScore = health,
                leaks = leaks,
                forecast = forecast
            )
        }
    }

    fun updateSimulatedAmount(amountStr: String) {
        _uiState.value = _uiState.value.copy(simulatedAmountInput = amountStr)
    }

    fun simulatePurchase() {
        if (!_uiState.value.isAiEnabled) return

        val input = _uiState.value.simulatedAmountInput.trim()
        if (input.isBlank()) return

        val parsedAmount = try {
            BigDecimal(input.replace(",", ""))
        } catch (e: Exception) {
            return
        }

        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
            val result = simulatePurchaseUseCase(profileId, Money.of(parsedAmount, "INR"))
            _uiState.value = _uiState.value.copy(simulationResult = result)
        }
    }
}

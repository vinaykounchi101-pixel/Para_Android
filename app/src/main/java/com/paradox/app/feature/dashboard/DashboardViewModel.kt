package com.paradox.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.DashboardSummary
import com.paradox.app.domain.usecase.dashboard.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import java.time.YearMonth
import javax.inject.Inject

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val availableMonths: List<YearMonth> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase
) : ViewModel() {

    private val _selectedYearMonth = MutableStateFlow(YearMonth.now())
    private val _uiState = MutableStateFlow(
        DashboardUiState(
            availableMonths = generateAvailableMonths()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun selectMonth(yearMonth: YearMonth) {
        _selectedYearMonth.value = yearMonth
        _uiState.update { it.copy(selectedYearMonth = yearMonth, isLoading = true) }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                sessionDataStore.activeProfileId,
                _selectedYearMonth
            ) { profileId, yearMonth ->
                Pair(profileId, yearMonth)
            }.flatMapLatest { (profileId, yearMonth) ->
                if (profileId == null) {
                    flowOf(null)
                } else {
                    getDashboardSummaryUseCase(profileId, yearMonth)
                }
            }.collect { summary ->
                _uiState.update {
                    it.copy(
                        summary = summary,
                        selectedYearMonth = _selectedYearMonth.value,
                        isLoading = false,
                        errorMessage = if (summary == null) "No active profile found" else null
                    )
                }
            }
        }
    }

    companion object {
        fun generateAvailableMonths(): List<YearMonth> {
            val current = YearMonth.now()
            // Generates current month and past 11 months (total 12 months) + 1 future month
            val months = mutableListOf<YearMonth>()
            months.add(current.plusMonths(1))
            months.add(current)
            for (i in 1..11) {
                months.add(current.minusMonths(i.toLong()))
            }
            return months
        }
    }
}

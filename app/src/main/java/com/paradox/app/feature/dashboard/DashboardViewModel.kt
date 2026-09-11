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
import javax.inject.Inject

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            sessionDataStore.activeProfileId.flatMapLatest { profileId ->
                if (profileId == null) {
                    flowOf(null)
                } else {
                    getDashboardSummaryUseCase(profileId)
                }
            }.collect { summary ->
                _uiState.update {
                    it.copy(
                        summary = summary,
                        isLoading = false,
                        errorMessage = if (summary == null) "No active profile found" else null
                    )
                }
            }
        }
    }
}

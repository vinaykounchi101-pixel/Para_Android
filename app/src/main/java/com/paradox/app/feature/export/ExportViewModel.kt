package com.paradox.app.feature.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.CashFlowSummary
import com.paradox.app.domain.repository.ProfileRepository
import com.paradox.app.domain.usecase.income.CalculateCashFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

enum class ExportDateRange(val displayName: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    YEAR_TO_DATE("Year to Date"),
    ALL_TIME("All Time")
}

data class ExportUiState(
    val selectedRange: ExportDateRange = ExportDateRange.THIS_MONTH,
    val selectedFormat: ExportFormat = ExportFormat.PDF,
    val selectedType: ExportType = ExportType.ALL,
    val isExporting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ExportEvent {
    data class ShareFile(val file: File, val mimeType: String) : ExportEvent
    data class ShowToast(val message: String) : ExportEvent
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val profileRepository: ProfileRepository,
    private val exportDataUseCase: ExportDataUseCase,
    private val calculateCashFlowUseCase: CalculateCashFlowUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ExportEvent>()
    val eventFlow: SharedFlow<ExportEvent> = _eventFlow.asSharedFlow()

    fun selectRange(range: ExportDateRange) {
        _uiState.update { it.copy(selectedRange = range) }
    }

    fun selectFormat(format: ExportFormat) {
        _uiState.update { it.copy(selectedFormat = format) }
    }

    fun selectType(type: ExportType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun exportAndShare() {
        val now = LocalDate.now()
        val (startDate, endDate) = when (_uiState.value.selectedRange) {
            ExportDateRange.THIS_MONTH -> Pair(
                now.with(TemporalAdjusters.firstDayOfMonth()),
                now.with(TemporalAdjusters.lastDayOfMonth())
            )
            ExportDateRange.LAST_MONTH -> {
                val lastMonth = now.minusMonths(1)
                Pair(
                    lastMonth.with(TemporalAdjusters.firstDayOfMonth()),
                    lastMonth.with(TemporalAdjusters.lastDayOfMonth())
                )
            }
            ExportDateRange.LAST_3_MONTHS -> Pair(
                now.minusMonths(3).with(TemporalAdjusters.firstDayOfMonth()),
                now
            )
            ExportDateRange.YEAR_TO_DATE -> Pair(
                now.with(TemporalAdjusters.firstDayOfYear()),
                now
            )
            ExportDateRange.ALL_TIME -> Pair(
                LocalDate.of(2020, 1, 1),
                now
            )
        }

        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch
            val profile = profileRepository.getProfileById(profileId)
            val profileName = profile?.name ?: "Personal"

            _uiState.update { it.copy(isExporting = true, errorMessage = null) }

            try {
                val file = if (_uiState.value.selectedFormat == ExportFormat.CSV) {
                    exportDataUseCase.generateCsv(
                        profileId = profileId,
                        startDate = startDate,
                        endDate = endDate,
                        type = _uiState.value.selectedType
                    )
                } else {
                    val summary = calculateCashFlowUseCase(profileId, startDate, endDate).firstOrNull()
                    exportDataUseCase.generatePdf(
                        profileId = profileId,
                        profileName = profileName,
                        startDate = startDate,
                        endDate = endDate,
                        type = _uiState.value.selectedType,
                        summary = summary
                    )
                }

                val mimeType = if (_uiState.value.selectedFormat == ExportFormat.CSV) "text/csv" else "application/pdf"
                _eventFlow.emit(ExportEvent.ShareFile(file, mimeType))
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, errorMessage = e.message) }
                _eventFlow.emit(ExportEvent.ShowToast(e.message ?: "Export failed"))
            }
        }
    }
}

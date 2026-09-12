package com.paradox.app.feature.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Constants
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Debt
import com.paradox.app.domain.model.DebtRepayment
import com.paradox.app.domain.model.DebtStatus
import com.paradox.app.domain.model.DebtSummary
import com.paradox.app.domain.model.DebtType
import com.paradox.app.domain.usecase.debt.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class DebtUiState(
    val debts: List<Debt> = emptyList(),
    val filteredDebts: List<Debt> = emptyList(),
    val summary: DebtSummary = DebtSummary(
        totalReceivable = Money.of(BigDecimal.ZERO, Constants.DEFAULT_CURRENCY),
        totalPayable = Money.of(BigDecimal.ZERO, Constants.DEFAULT_CURRENCY),
        netBalance = Money.of(BigDecimal.ZERO, Constants.DEFAULT_CURRENCY)
    ),
    val selectedFilter: DebtFilter = DebtFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val activeProfileId: String? = null,
    val preferredCurrency: String = Constants.DEFAULT_CURRENCY,

    // Dialog & Action States
    val isAddDebtDialogOpen: Boolean = false,
    val isRepayDialogOpen: Boolean = false,
    val selectedDebtForRepayment: Debt? = null,
    val repaymentsForSelectedDebt: List<DebtRepayment> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val getDebtsUseCase: GetDebtsUseCase,
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase,
    private val addDebtUseCase: AddDebtUseCase,
    private val recordDebtRepaymentUseCase: RecordDebtRepaymentUseCase,
    private val deleteDebtUseCase: DeleteDebtUseCase,
    private val updateDebtUseCase: UpdateDebtUseCase,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebtUiState(isLoading = true))
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionDataStore.activeProfileId.collectLatest { profileId ->
                if (profileId != null) {
                    _uiState.update { it.copy(activeProfileId = profileId) }
                    observeDebtsAndSummary(profileId)
                } else {
                    _uiState.update { it.copy(isLoading = false, debts = emptyList(), filteredDebts = emptyList()) }
                }
            }
        }
        viewModelScope.launch {
            sessionDataStore.preferredCurrency.collectLatest { currency ->
                _uiState.update { it.copy(preferredCurrency = currency) }
            }
        }
    }

    private fun observeDebtsAndSummary(profileId: String) {
        viewModelScope.launch {
            getDebtSummaryUseCase(profileId).collectLatest { summary ->
                _uiState.update { it.copy(summary = summary) }
            }
        }

        viewModelScope.launch {
            getDebtsUseCase(profileId, DebtFilter.ALL).collectLatest { debts ->
                _uiState.update { state ->
                    val filtered = applyFilterAndSearch(debts, state.selectedFilter, state.searchQuery)
                    state.copy(debts = debts, filteredDebts = filtered, isLoading = false)
                }
            }
        }
    }

    fun setFilter(filter: DebtFilter) {
        _uiState.update { state ->
            val filtered = applyFilterAndSearch(state.debts, filter, state.searchQuery)
            state.copy(selectedFilter = filter, filteredDebts = filtered)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = applyFilterAndSearch(state.debts, state.selectedFilter, query)
            state.copy(searchQuery = query, filteredDebts = filtered)
        }
    }

    private fun applyFilterAndSearch(debts: List<Debt>, filter: DebtFilter, query: String): List<Debt> {
        val byFilter = when (filter) {
            DebtFilter.ALL -> debts
            DebtFilter.LENT -> debts.filter { it.debtType == DebtType.LENT && it.status == DebtStatus.ACTIVE }
            DebtFilter.BORROWED -> debts.filter { it.debtType == DebtType.BORROWED && it.status == DebtStatus.ACTIVE }
            DebtFilter.ACTIVE -> debts.filter { it.status == DebtStatus.ACTIVE }
            DebtFilter.SETTLED -> debts.filter { it.status == DebtStatus.SETTLED }
        }
        val cleanQuery = query.trim()
        return if (cleanQuery.isBlank()) {
            byFilter
        } else {
            byFilter.filter {
                it.personName.contains(cleanQuery, ignoreCase = true) ||
                        (it.notes?.contains(cleanQuery, ignoreCase = true) == true) ||
                        (it.personContactNumber?.contains(cleanQuery) == true)
            }
        }
    }

    fun openAddDebtDialog() {
        _uiState.update { it.copy(isAddDebtDialogOpen = true, errorMessage = null, successMessage = null) }
    }

    fun closeAddDebtDialog() {
        _uiState.update { it.copy(isAddDebtDialogOpen = false) }
    }

    fun addDebt(
        personName: String,
        personContactNumber: String?,
        debtType: DebtType,
        amount: BigDecimal,
        dueDate: LocalDate?,
        notes: String?,
        reminderEnabled: Boolean
    ) {
        val profileId = _uiState.value.activeProfileId ?: return
        viewModelScope.launch {
            val result = addDebtUseCase(
                profileId = profileId,
                personName = personName,
                personContactNumber = personContactNumber,
                debtType = debtType,
                amount = amount,
                currency = _uiState.value.preferredCurrency,
                dueDate = dueDate,
                notes = notes,
                reminderEnabled = reminderEnabled
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isAddDebtDialogOpen = false,
                            successMessage = "Debt record created for $personName"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
                else -> {}
            }
        }
    }

    fun openRepayDialog(debt: Debt) {
        _uiState.update {
            it.copy(
                isRepayDialogOpen = true,
                selectedDebtForRepayment = debt,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun closeRepayDialog() {
        _uiState.update { it.copy(isRepayDialogOpen = false, selectedDebtForRepayment = null) }
    }

    fun recordRepayment(amount: BigDecimal, notes: String?) {
        val profileId = _uiState.value.activeProfileId ?: return
        val debt = _uiState.value.selectedDebtForRepayment ?: return

        viewModelScope.launch {
            val result = recordDebtRepaymentUseCase(
                profileId = profileId,
                debtId = debt.id,
                amount = amount,
                currency = debt.remainingAmount.currencyCode,
                repaymentDate = LocalDate.now(),
                notes = notes
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isRepayDialogOpen = false,
                            selectedDebtForRepayment = null,
                            successMessage = "Payment recorded successfully"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
                else -> {}
            }
        }
    }

    fun deleteDebt(id: String) {
        val profileId = _uiState.value.activeProfileId ?: return
        viewModelScope.launch {
            when (val result = deleteDebtUseCase(profileId, id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(successMessage = "Debt record deleted") }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.exception.message) }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}

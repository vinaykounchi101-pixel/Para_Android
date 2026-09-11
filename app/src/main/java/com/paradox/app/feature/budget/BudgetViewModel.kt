package com.paradox.app.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetStatus
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.Category
import com.paradox.app.domain.usecase.budget.CalculateBudgetStatusUseCase
import com.paradox.app.domain.usecase.budget.DeleteBudgetUseCase
import com.paradox.app.domain.usecase.budget.GetBudgetsUseCase
import com.paradox.app.domain.usecase.budget.SetBudgetUseCase
import com.paradox.app.domain.usecase.category.GetCategoriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class BudgetUiState(
    val overallStatus: BudgetStatus? = null,
    val categoryStatuses: List<BudgetStatus> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val calculateBudgetStatusUseCase: CalculateBudgetStatusUseCase,
    private val setBudgetUseCase: SetBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        observeBudgets()
    }

    private fun observeBudgets() {
        viewModelScope.launch {
            val currency = sessionDataStore.preferredCurrency.firstOrNull() ?: "INR"
            _uiState.update { it.copy(currencyCode = currency) }

            sessionDataStore.activeProfileId.flatMapLatest { profileId ->
                if (profileId == null) {
                    flowOf(null)
                } else {
                    calculateBudgetStatusUseCase(profileId, BudgetType.MONTHLY)
                }
            }.collect { overallStatus ->
                _uiState.update {
                    it.copy(
                        overallStatus = overallStatus,
                        isLoading = false
                    )
                }
            }
        }

        viewModelScope.launch {
            sessionDataStore.activeProfileId.flatMapLatest { profileId ->
                if (profileId == null) flowOf(emptyList()) else getCategoriesUseCase(profileId)
            }.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun setMonthlyBudget(amountInput: String, thresholdPct: Int = 80) {
        val amount = try {
            BigDecimal(amountInput.trim())
        } catch (_: Exception) {
            return
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) return

        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch
            val money = Money(amount, _uiState.value.currencyCode)
            setBudgetUseCase(
                profileId = profileId,
                type = BudgetType.MONTHLY,
                limit = money,
                categoryId = null,
                thresholdPct = thresholdPct
            )
        }
    }

    fun deleteMonthlyBudget() {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: return@launch
            val overall = _uiState.value.overallStatus?.budget ?: return@launch
            deleteBudgetUseCase(profileId, overall.id)
        }
    }
}

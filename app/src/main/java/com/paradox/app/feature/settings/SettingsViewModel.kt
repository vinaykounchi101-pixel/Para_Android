package com.paradox.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.security.BiometricPromptManager
import com.paradox.app.domain.model.Profile
import com.paradox.app.domain.repository.ProfileRepository
import com.paradox.app.domain.usecase.profile.DeleteProfileUseCase
import com.paradox.app.domain.usecase.profile.GetProfilesUseCase
import com.paradox.app.domain.usecase.profile.SwitchProfileUseCase
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.model.Budget
import com.paradox.app.domain.model.BudgetType
import com.paradox.app.domain.model.Expense
import com.paradox.app.domain.model.Income
import com.paradox.app.domain.model.IncomeSource
import com.paradox.app.domain.model.RecurringExpense
import com.paradox.app.domain.model.RecurringFrequency
import com.paradox.app.domain.model.SavingsGoal
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.repository.BudgetRepository
import com.paradox.app.domain.repository.CategoryRepository
import com.paradox.app.domain.repository.ExpenseRepository
import com.paradox.app.domain.repository.IncomeRepository
import com.paradox.app.domain.repository.PaymentMethodRepository
import com.paradox.app.domain.repository.RecurringExpenseRepository
import com.paradox.app.domain.repository.SavingsGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class SettingsUiState(
    val activeProfile: Profile? = null,
    val allProfiles: List<Profile> = emptyList(),
    val currency: String = "INR",
    val canUseBiometric: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val themeMode: String = "SYSTEM",
    val themePalette: String = "SLATE",
    val isSeeding: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface SettingsEvent {
    data object NavigateToUnlock : SettingsEvent
    data object NavigateToOnboarding : SettingsEvent
    data class ShowToast(val message: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val profileRepository: ProfileRepository,
    private val getProfilesUseCase: GetProfilesUseCase,
    private val switchProfileUseCase: SwitchProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val biometricPromptManager: BiometricPromptManager,
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringExpenseRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val activeId = sessionDataStore.activeProfileId.firstOrNull()
            val currency = sessionDataStore.preferredCurrency.firstOrNull() ?: "INR"
            val canBio = biometricPromptManager.canAuthenticate()

            if (activeId != null) {
                val profile = profileRepository.getProfileById(activeId)
                _uiState.update {
                    it.copy(
                        activeProfile = profile,
                        currency = currency,
                        canUseBiometric = canBio,
                        isBiometricEnabled = profile?.biometricEnabled ?: false
                    )
                }
            }

            launch {
                sessionDataStore.themeMode.collect { mode ->
                    _uiState.update { it.copy(themeMode = mode) }
                }
            }

            launch {
                sessionDataStore.themePalette.collect { palette ->
                    _uiState.update { it.copy(themePalette = palette) }
                }
            }

            getProfilesUseCase().collect { profiles ->
                _uiState.update { it.copy(allProfiles = profiles) }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            sessionDataStore.setThemeMode(mode)
        }
    }

    fun setThemePalette(palette: String) {
        viewModelScope.launch {
            sessionDataStore.setThemePalette(palette)
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        val profile = _uiState.value.activeProfile ?: return
        viewModelScope.launch {
            val updated = profile.copy(biometricEnabled = enabled)
            profileRepository.updateProfile(updated)
            _uiState.update { it.copy(activeProfile = updated, isBiometricEnabled = enabled) }
        }
    }

    fun lockVault() {
        viewModelScope.launch {
            sessionDataStore.setAppLocked(true)
            _events.emit(SettingsEvent.NavigateToUnlock)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            sessionDataStore.setAppLocked(true)
            _events.emit(SettingsEvent.NavigateToUnlock)
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            switchProfileUseCase(profileId)
            _events.emit(SettingsEvent.NavigateToUnlock)
        }
    }

    fun deleteProfile() {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            deleteProfileUseCase(profileId)
            _events.emit(SettingsEvent.NavigateToOnboarding)
        }
    }

    fun seedSampleData(onSuccess: () -> Unit) {
        val profileId = _uiState.value.activeProfile?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSeeding = true) }
            val categories = categoryRepository.getCategories(profileId).first()
            val pms = paymentMethodRepository.getPaymentMethods(profileId).first()
            val defaultCat = categories.firstOrNull()?.id ?: "cat_default"
            val foodCat = categories.firstOrNull { it.name.contains("Food", ignoreCase = true) }?.id ?: defaultCat
            val billsCat = categories.firstOrNull { it.name.contains("Bill", ignoreCase = true) }?.id ?: defaultCat
            val shopCat = categories.firstOrNull { it.name.contains("Shop", ignoreCase = true) }?.id ?: defaultCat
            val defaultPm = pms.firstOrNull()?.id ?: "pm_default"

            // 1. Accounts
            accountRepository.addAccount(
                Account(
                    id = "acc_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    name = "HDFC Salary Bank",
                    type = AccountType.BANK,
                    initialBalance = Money(BigDecimal("58000.00"), "INR"),
                    isDefault = true
                )
            )
            accountRepository.addAccount(
                Account(
                    id = "acc_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    name = "Cash Pocket Wallet",
                    type = AccountType.CASH,
                    initialBalance = Money(BigDecimal("4500.00"), "INR")
                )
            )

            // 2. Incomes
            incomeRepository.addIncome(
                Income(
                    id = "inc_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    source = IncomeSource.SALARY,
                    amount = Money(BigDecimal("85000.00"), "INR"),
                    currency = "INR",
                    date = LocalDate.now().withDayOfMonth(1),
                    notes = "Monthly Salary Credited"
                )
            )

            // 3. Budgets
            budgetRepository.setBudget(
                Budget(
                    id = "bud_overall",
                    profileId = profileId,
                    type = BudgetType.MONTHLY,
                    limit = Money(BigDecimal("40000.00"), "INR"),
                    thresholdPct = 80
                )
            )
            budgetRepository.setBudget(
                Budget(
                    id = "bud_food",
                    profileId = profileId,
                    type = BudgetType.CATEGORY,
                    categoryId = foodCat,
                    limit = Money(BigDecimal("12000.00"), "INR"),
                    thresholdPct = 80
                )
            )

            // 4. Expenses
            expenseRepository.addExpense(
                Expense(
                    id = "exp_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    title = "Nature's Basket Groceries",
                    money = Money(BigDecimal("2450.00"), "INR"),
                    categoryId = foodCat,
                    paymentMethodId = defaultPm,
                    date = LocalDate.now().minusDays(1)
                )
            )
            expenseRepository.addExpense(
                Expense(
                    id = "exp_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    title = "Dinner at Mainland China",
                    money = Money(BigDecimal("1850.00"), "INR"),
                    categoryId = foodCat,
                    paymentMethodId = defaultPm,
                    date = LocalDate.now().minusDays(2)
                )
            )
            expenseRepository.addExpense(
                Expense(
                    id = "exp_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    title = "Electricity & Wi-Fi Bill",
                    money = Money(BigDecimal("1650.00"), "INR"),
                    categoryId = billsCat,
                    paymentMethodId = defaultPm,
                    date = LocalDate.now().minusDays(4)
                )
            )
            expenseRepository.addExpense(
                Expense(
                    id = "exp_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    title = "Blue Tokai Coffee",
                    money = Money(BigDecimal("320.00"), "INR"),
                    categoryId = foodCat,
                    paymentMethodId = defaultPm,
                    date = LocalDate.now()
                )
            )
            expenseRepository.addExpense(
                Expense(
                    id = "exp_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    title = "Amazon Shopping",
                    money = Money(BigDecimal("1299.00"), "INR"),
                    categoryId = shopCat,
                    paymentMethodId = defaultPm,
                    date = LocalDate.now().minusDays(3)
                )
            )

            // 5. Recurring
            recurringRepository.addRecurring(
                RecurringExpense(
                    id = "rec_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    title = "Netflix Premium 4K",
                    amount = Money(BigDecimal("649.00"), "INR"),
                    frequency = RecurringFrequency.MONTHLY,
                    categoryId = billsCat,
                    paymentMethodId = defaultPm,
                    startDate = LocalDate.now().withDayOfMonth(1),
                    nextDueDate = LocalDate.now().plusWeeks(2)
                )
            )
            recurringRepository.addRecurring(
                RecurringExpense(
                    id = "rec_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    title = "Spotify Family Plan",
                    amount = Money(BigDecimal("179.00"), "INR"),
                    frequency = RecurringFrequency.MONTHLY,
                    categoryId = billsCat,
                    paymentMethodId = defaultPm,
                    startDate = LocalDate.now().withDayOfMonth(5),
                    nextDueDate = LocalDate.now().plusWeeks(3)
                )
            )

            // 6. Savings Goals
            savingsGoalRepository.addGoal(
                SavingsGoal(
                    id = "goal_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    name = "Emergency Buffer Fund",
                    targetAmount = Money(BigDecimal("100000.00"), "INR"),
                    currentAmount = Money(BigDecimal("45000.00"), "INR"),
                    targetDate = LocalDate.now().plusMonths(6)
                )
            )
            savingsGoalRepository.addGoal(
                SavingsGoal(
                    id = "goal_" + UUID.randomUUID().toString().take(8),
                    profileId = profileId,
                    name = "Japan Autumn Tour 2027",
                    targetAmount = Money(BigDecimal("200000.00"), "INR"),
                    currentAmount = Money(BigDecimal("75000.00"), "INR"),
                    targetDate = LocalDate.now().plusYears(1)
                )
            )

            _uiState.update { it.copy(isSeeding = false) }
            _events.emit(SettingsEvent.ShowToast("Sample records populated successfully!"))
            onSuccess()
        }
    }
}

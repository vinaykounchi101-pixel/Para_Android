package com.paradox.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.security.BiometricPromptManager
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.usecase.profile.CreateProfileUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class OnboardingUiState(
    val name: String = "",
    val authType: AuthType = AuthType.PIN,
    val credential: String = "",
    val confirmCredential: String = "",
    val patternStep: Int = 0, // 0 = Set Pattern, 1 = Confirm Pattern
    val biometricEnabled: Boolean = false,
    val canEnableBiometrics: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface OnboardingEvent {
    data object NavigateToDashboard : OnboardingEvent
    data class ShowToast(val message: String) : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val createProfileUseCase: CreateProfileUseCase,
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

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    init {
        _uiState.update { it.copy(canEnableBiometrics = biometricPromptManager.canAuthenticate()) }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name, errorMessage = null) }
    fun onAuthTypeChanged(authType: AuthType) = _uiState.update { 
        it.copy(
            authType = authType, 
            credential = "", 
            confirmCredential = "", 
            patternStep = 0,
            errorMessage = null
        ) 
    }
    fun onCredentialChanged(credential: String) = _uiState.update { it.copy(credential = credential, errorMessage = null) }
    fun onConfirmCredentialChanged(confirm: String) = _uiState.update { it.copy(confirmCredential = confirm, errorMessage = null) }
    fun onBiometricToggled(enabled: Boolean) = _uiState.update { it.copy(biometricEnabled = enabled) }

    fun onPatternDrawn(pattern: List<Int>) {
        if (pattern.size < 4) {
            _uiState.update { it.copy(errorMessage = "Pattern must connect at least 4 dots") }
            return
        }
        val patternString = pattern.joinToString("-")
        val state = _uiState.value
        if (state.patternStep == 0) {
            _uiState.update { 
                it.copy(
                    credential = patternString, 
                    patternStep = 1, 
                    errorMessage = null 
                ) 
            }
        } else {
            if (patternString == state.credential) {
                _uiState.update { 
                    it.copy(
                        confirmCredential = patternString, 
                        errorMessage = null 
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        errorMessage = "Patterns do not match. Please redraw to confirm." 
                    ) 
                }
            }
        }
    }

    fun onResetPattern() {
        _uiState.update { 
            it.copy(
                credential = "", 
                confirmCredential = "", 
                patternStep = 0, 
                errorMessage = null 
            ) 
        }
    }

    fun createProfile() {
        val state = _uiState.value
        val effectiveName = if (state.name.isNotBlank()) state.name.trim() else "Srushti"
        
        // Validation per lock method
        when (state.authType) {
            AuthType.PIN -> {
                val pin = state.credential.trim()
                val confirmPin = state.confirmCredential.trim()
                if (pin.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Please enter a 4-digit PIN") }
                    return
                }
                if (pin.length < 4) {
                    _uiState.update { it.copy(errorMessage = "PIN must be at least 4 digits") }
                    return
                }
                if (confirmPin.isEmpty() || pin != confirmPin) {
                    _uiState.update { it.copy(errorMessage = "PINs do not match") }
                    return
                }
            }
            AuthType.PASSWORD -> {
                val pwd = state.credential
                val confirmPwd = state.confirmCredential
                if (pwd.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Please enter a password") }
                    return
                }
                if (pwd.length < 4) {
                    _uiState.update { it.copy(errorMessage = "Password must be at least 4 characters") }
                    return
                }
                if (confirmPwd.isEmpty() || pwd != confirmPwd) {
                    _uiState.update { it.copy(errorMessage = "Passwords do not match") }
                    return
                }
            }
            AuthType.PATTERN -> {
                if (state.credential.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Please draw a pattern with at least 4 dots") }
                    return
                }
                if (state.confirmCredential.isEmpty() || state.credential != state.confirmCredential) {
                    _uiState.update { it.copy(errorMessage = "Please confirm your pattern by drawing it again") }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = createProfileUseCase(
                name = effectiveName,
                authType = state.authType,
                credentialPlainText = state.credential,
                biometricEnabled = state.biometricEnabled
            )

            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(OnboardingEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message ?: "Failed to create profile") }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun createDemoProfileWithData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = createProfileUseCase(
                name = "Vinay",
                authType = AuthType.PIN,
                credentialPlainText = "1234",
                biometricEnabled = false
            )

            when (result) {
                is Result.Success -> {
                    val profileId = result.data.id
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

                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(OnboardingEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message ?: "Failed to create demo profile") }
                }
                Result.Loading -> Unit
            }
        }
    }
}

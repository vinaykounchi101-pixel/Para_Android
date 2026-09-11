package com.paradox.app.feature.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.money.Money
import com.paradox.app.domain.model.Account
import com.paradox.app.domain.model.AccountType
import com.paradox.app.domain.repository.AccountRepository
import com.paradox.app.domain.usecase.account.AddAccountUseCase
import com.paradox.app.domain.usecase.account.DeleteAccountUseCase
import com.paradox.app.domain.usecase.account.GetAccountsUseCase
import com.paradox.app.domain.usecase.account.UpdateAccountUseCase
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
import javax.inject.Inject

data class AccountListUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class AddEditAccountUiState(
    val isEditMode: Boolean = false,
    val accountId: String? = null,
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val initialBalanceInput: String = "0.00",
    val currency: String = "INR",
    val colorHex: String = "#3B82F6",
    val isDefault: Boolean = false,
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AccountEvent {
    data object NavigateBack : AccountEvent
    data class ShowToast(val message: String) : AccountEvent
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionDataStore: SessionDataStore,
    private val getAccountsUseCase: GetAccountsUseCase,
    private val addAccountUseCase: AddAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(AccountListUiState())
    val listState: StateFlow<AccountListUiState> = _listState.asStateFlow()

    private val _editState = MutableStateFlow(AddEditAccountUiState())
    val editState: StateFlow<AddEditAccountUiState> = _editState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AccountEvent>()
    val eventFlow: SharedFlow<AccountEvent> = _eventFlow.asSharedFlow()

    private var currentProfileId: String? = null

    init {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull()
            currentProfileId = profileId
            if (profileId != null) {
                getAccountsUseCase.seedDefaultsIfNeeded(profileId)
                loadAccounts(profileId)
            }
        }
    }

    private fun loadAccounts(profileId: String) {
        viewModelScope.launch {
            getAccountsUseCase(profileId).collect { list ->
                _listState.update {
                    it.copy(
                        accounts = list,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun initAddEdit(accountId: String?) {
        if (accountId != null && accountId.isNotBlank()) {
            _editState.update { it.copy(isEditMode = true, accountId = accountId, isInitialLoading = true) }
            val profileId = currentProfileId ?: return
            viewModelScope.launch {
                val account = accountRepository.getAccountById(profileId, accountId)
                if (account != null) {
                    _editState.update {
                        it.copy(
                            name = account.name,
                            type = account.type,
                            initialBalanceInput = account.initialBalance.amount.toPlainString(),
                            currency = account.currency,
                            colorHex = account.colorHex,
                            isDefault = account.isDefault,
                            isInitialLoading = false
                        )
                    }
                } else {
                    _editState.update { it.copy(isInitialLoading = false, errorMessage = "Account not found") }
                }
            }
        } else {
            _editState.update { AddEditAccountUiState() }
        }
    }

    fun onNameChange(name: String) {
        _editState.update { it.copy(name = name, errorMessage = null) }
    }

    fun onTypeSelected(type: AccountType) {
        _editState.update { it.copy(type = type) }
    }

    fun onInitialBalanceChange(balance: String) {
        _editState.update { it.copy(initialBalanceInput = balance) }
    }

    fun onColorChange(colorHex: String) {
        _editState.update { it.copy(colorHex = colorHex) }
    }

    fun onDefaultToggled(isDefault: Boolean) {
        _editState.update { it.copy(isDefault = isDefault) }
    }

    fun saveAccount() {
        val profileId = currentProfileId ?: return
        val state = _editState.value

        if (state.name.isBlank()) {
            _editState.update { it.copy(errorMessage = "Please enter an account name") }
            return
        }

        val balanceDecimal = state.initialBalanceInput.toBigDecimalOrNull() ?: BigDecimal.ZERO

        viewModelScope.launch {
            _editState.update { it.copy(isLoading = true, errorMessage = null) }
            val money = Money(balanceDecimal, state.currency)

            if (state.isEditMode && state.accountId != null) {
                val existing = accountRepository.getAccountById(profileId, state.accountId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = state.name.trim(),
                        type = state.type,
                        currency = state.currency,
                        initialBalance = money,
                        colorHex = state.colorHex,
                        iconName = state.type.defaultIcon,
                        isDefault = state.isDefault
                    )
                    when (val result = updateAccountUseCase(updated)) {
                        is Result.Success -> {
                            _eventFlow.emit(AccountEvent.ShowToast("Account updated"))
                            _eventFlow.emit(AccountEvent.NavigateBack)
                        }
                        is Result.Error -> {
                            _editState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                        }
                        is Result.Loading -> Unit
                    }
                }
            } else {
                when (val result = addAccountUseCase(
                    profileId = profileId,
                    name = state.name,
                    type = state.type,
                    currency = state.currency,
                    initialBalance = money,
                    colorHex = state.colorHex,
                    iconName = state.type.defaultIcon,
                    isDefault = state.isDefault
                )) {
                    is Result.Success -> {
                        _eventFlow.emit(AccountEvent.ShowToast("Account created"))
                        _eventFlow.emit(AccountEvent.NavigateBack)
                    }
                    is Result.Error -> {
                        _editState.update { it.copy(isLoading = false, errorMessage = result.exception.message) }
                    }
                    is Result.Loading -> Unit
                }
            }
        }
    }

    fun deleteAccount(accountId: String) {
        val profileId = currentProfileId ?: return
        viewModelScope.launch {
            when (val result = deleteAccountUseCase(profileId, accountId)) {
                is Result.Success -> {
                    _eventFlow.emit(AccountEvent.ShowToast("Account deleted"))
                }
                is Result.Error -> {
                    _eventFlow.emit(AccountEvent.ShowToast(result.exception.message ?: "Failed to delete account"))
                }
                is Result.Loading -> Unit
            }
        }
    }
}

package com.paradox.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.security.BiometricPromptManager
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.usecase.profile.CreateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val name: String = "",
    val authType: AuthType = AuthType.PIN,
    val credential: String = "",
    val confirmCredential: String = "",
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
    private val biometricPromptManager: BiometricPromptManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>()
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    init {
        _uiState.update { it.copy(canEnableBiometrics = biometricPromptManager.canAuthenticate()) }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name, errorMessage = null) }
    fun onAuthTypeChanged(authType: AuthType) = _uiState.update { it.copy(authType = authType, credential = "", confirmCredential = "", errorMessage = null) }
    fun onCredentialChanged(credential: String) = _uiState.update { it.copy(credential = credential, errorMessage = null) }
    fun onConfirmCredentialChanged(confirm: String) = _uiState.update { it.copy(confirmCredential = confirm, errorMessage = null) }
    fun onBiometricToggled(enabled: Boolean) = _uiState.update { it.copy(biometricEnabled = enabled) }

    fun createProfile() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your name") }
            return
        }
        if (state.credential.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please set a security PIN / Password") }
            return
        }
        if (state.authType == AuthType.PIN && state.credential.length < 4) {
            _uiState.update { it.copy(errorMessage = "PIN must be at least 4 digits") }
            return
        }
        if (state.credential != state.confirmCredential) {
            _uiState.update { it.copy(errorMessage = "Credentials do not match") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = createProfileUseCase(
                name = state.name,
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
}

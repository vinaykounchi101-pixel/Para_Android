package com.paradox.app.feature.profile

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.common.Result
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.security.BiometricPromptManager
import com.paradox.app.domain.model.AuthType
import com.paradox.app.domain.model.Profile
import com.paradox.app.domain.usecase.profile.GetProfilesUseCase
import com.paradox.app.domain.usecase.profile.UnlockProfileUseCase
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
import javax.inject.Inject

data class UnlockUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedProfile: Profile? = null,
    val credentialInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val canUseBiometric: Boolean = false
)

sealed interface UnlockEvent {
    data object NavigateToDashboard : UnlockEvent
    data object NavigateToOnboarding : UnlockEvent
}

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val unlockProfileUseCase: UnlockProfileUseCase,
    private val sessionDataStore: SessionDataStore,
    private val biometricPromptManager: BiometricPromptManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UnlockEvent>()
    val events: SharedFlow<UnlockEvent> = _events.asSharedFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            getProfilesUseCase().collect { profilesList ->
                if (profilesList.isEmpty()) {
                    _events.emit(UnlockEvent.NavigateToOnboarding)
                    return@collect
                }

                val activeId = sessionDataStore.activeProfileId.firstOrNull()
                val current = profilesList.find { it.id == activeId } ?: profilesList.first()

                _uiState.update {
                    it.copy(
                        profiles = profilesList,
                        selectedProfile = current,
                        canUseBiometric = current.biometricEnabled && biometricPromptManager.canAuthenticate()
                    )
                }
            }
        }
    }

    fun onProfileSelected(profile: Profile) {
        _uiState.update {
            it.copy(
                selectedProfile = profile,
                credentialInput = "",
                errorMessage = null,
                canUseBiometric = profile.biometricEnabled && biometricPromptManager.canAuthenticate()
            )
        }
    }

    fun onCredentialChanged(input: String) {
        _uiState.update { it.copy(credentialInput = input, errorMessage = null) }
        // If 4-digit PIN, attempt auto-submit
        if (input.length == 4 && _uiState.value.selectedProfile?.primaryAuthType?.name == "PIN") {
            unlock()
        }
    }

    fun onPatternCompleted(pattern: List<Int>) {
        val patternString = pattern.joinToString("-")
        _uiState.update { it.copy(credentialInput = patternString) }
        unlock()
    }

    fun unlock() {
        val state = _uiState.value
        val profile = state.selectedProfile ?: return

        if (state.credentialInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your credential") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = unlockProfileUseCase(profile.id, state.credentialInput)
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.emit(UnlockEvent.NavigateToDashboard)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            credentialInput = "",
                            errorMessage = if (profile.primaryAuthType == AuthType.PATTERN) "Incorrect Pattern" else "Incorrect PIN or Password"
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun triggerBiometricPrompt(activity: FragmentActivity) {
        val state = _uiState.value
        val profile = state.selectedProfile ?: return
        if (!state.canUseBiometric) return

        biometricPromptManager.showPrompt(
            activity = activity,
            title = "Unlock Paradox",
            subtitle = "Verify your identity to open ${profile.name}'s vault",
            negativeButtonText = "Use PIN/Password"
        ) { result ->
            when (result) {
                BiometricPromptManager.BiometricResult.Success -> {
                    viewModelScope.launch {
                        unlockProfileUseCase.unlockViaBiometric(profile.id)
                        _events.emit(UnlockEvent.NavigateToDashboard)
                    }
                }
                is BiometricPromptManager.BiometricResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.errString.toString()) }
                }
                BiometricPromptManager.BiometricResult.Failed -> {
                    _uiState.update { it.copy(errorMessage = "Biometric authentication failed") }
                }
                else -> Unit
            }
        }
    }
}

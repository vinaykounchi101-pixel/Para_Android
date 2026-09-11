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

data class SettingsUiState(
    val activeProfile: Profile? = null,
    val allProfiles: List<Profile> = emptyList(),
    val currency: String = "INR",
    val canUseBiometric: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = false
)

sealed interface SettingsEvent {
    data object NavigateToUnlock : SettingsEvent
    data object NavigateToOnboarding : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val profileRepository: ProfileRepository,
    private val getProfilesUseCase: GetProfilesUseCase,
    private val switchProfileUseCase: SwitchProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val biometricPromptManager: BiometricPromptManager
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

            getProfilesUseCase().collect { profiles ->
                _uiState.update { it.copy(allProfiles = profiles) }
            }
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
}

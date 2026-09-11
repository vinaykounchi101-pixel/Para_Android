package com.paradox.app.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.usecase.backup.EncryptedBackupUseCase
import com.paradox.app.domain.usecase.backup.RestoreBackupUseCase
import com.paradox.app.domain.usecase.backup.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val generatedBackupPayload: String? = null,
    val restoreResult: RestoreResult? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val encryptedBackupUseCase: EncryptedBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun createEncryptedBackup(passphrase: String) {
        if (passphrase.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passphrase must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
            try {
                val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
                val payload = encryptedBackupUseCase(profileId, passphrase)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    generatedBackupPayload = payload
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "Export failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun restoreEncryptedBackup(payload: String, passphrase: String) {
        if (payload.isBlank() || passphrase.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please provide both backup payload and passphrase")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, errorMessage = null)
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
            val result = restoreBackupUseCase(profileId, payload, passphrase)
            _uiState.value = _uiState.value.copy(
                isRestoring = false,
                restoreResult = result,
                errorMessage = if (!result.success) result.errorMessage else null
            )
        }
    }
}

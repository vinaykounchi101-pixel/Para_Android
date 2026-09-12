package com.paradox.app.feature.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.usecase.backup.EncryptedBackupUseCase
import com.paradox.app.domain.usecase.backup.RestoreBackupUseCase
import com.paradox.app.domain.usecase.backup.RestoreResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class BackupUiState(
    val isExporting: Boolean = false,
    val isRestoring: Boolean = false,
    val generatedBackupPayload: String? = null,
    val exportedFileName: String? = null,
    val selectedFileName: String? = null,
    val selectedFileSizeText: String? = null,
    val selectedFileContent: String? = null,
    val restoreResult: RestoreResult? = null,
    val errorMessage: String? = null
)

sealed interface BackupEvent {
    data class ShareFile(val file: File, val uri: Uri) : BackupEvent
    data class ShowToast(val message: String) : BackupEvent
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val encryptedBackupUseCase: EncryptedBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<BackupEvent>()
    val eventFlow: SharedFlow<BackupEvent> = _eventFlow.asSharedFlow()

    fun createEncryptedBackupFile(passphrase: String, context: Context) {
        if (passphrase.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passphrase must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
            try {
                val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
                val payload = encryptedBackupUseCase(profileId, passphrase)

                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                val fileName = "paradox_vault_backup_$timestamp.paradoxvault"
                val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
                val backupFile = File(backupDir, fileName)

                FileOutputStream(backupFile).use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    generatedBackupPayload = payload,
                    exportedFileName = fileName
                )

                _eventFlow.emit(BackupEvent.ShareFile(backupFile, uri))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "Export failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun saveBackupToUri(passphrase: String, targetUri: Uri, context: Context) {
        if (passphrase.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = "Passphrase must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
            try {
                val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
                val payload = encryptedBackupUseCase(profileId, passphrase)

                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    generatedBackupPayload = payload
                )
                _eventFlow.emit(BackupEvent.ShowToast("Encrypted vault backup saved successfully"))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    errorMessage = "Failed to write backup file: ${e.localizedMessage}"
                )
            }
        }
    }

    fun loadBackupFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                var fileName = "Selected Backup File"
                var fileSize = 0L

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val content = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""

                val sizeText = if (fileSize > 0) "${fileSize / 1024 + 1} KB" else ""

                _uiState.value = _uiState.value.copy(
                    selectedFileName = fileName,
                    selectedFileSizeText = sizeText,
                    selectedFileContent = content,
                    errorMessage = null,
                    restoreResult = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to load backup file: ${e.localizedMessage}"
                )
            }
        }
    }

    fun restoreLoadedBackup(passphrase: String) {
        val payload = _uiState.value.selectedFileContent
        if (payload.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a valid backup file first")
            return
        }
        if (passphrase.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter the decryption passphrase")
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
            if (result.success) {
                _eventFlow.emit(BackupEvent.ShowToast("Restored ${result.itemsRestoredCount} records into vault!"))
            }
        }
    }

    fun restoreRawPayload(payload: String, passphrase: String) {
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
            if (result.success) {
                _eventFlow.emit(BackupEvent.ShowToast("Restored ${result.itemsRestoredCount} records into vault!"))
            }
        }
    }

    fun clearErrors() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

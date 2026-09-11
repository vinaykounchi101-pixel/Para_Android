package com.paradox.app.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.data.local.entity.SyncQueueItemEntity
import com.paradox.app.domain.usecase.sync.SyncEngine
import com.paradox.app.domain.usecase.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncUiState(
    val syncStatus: SyncStatus,
    val pendingItems: List<SyncQueueItemEntity> = emptyList(),
    val isAutoSyncEnabled: Boolean = false
)

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncEngine: SyncEngine,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState(syncStatus = syncEngine.syncStatus.value))
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            syncEngine.syncStatus.collect { status ->
                _uiState.value = _uiState.value.copy(syncStatus = status)
            }
        }
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
            syncEngine.observeQueueItems(profileId).collect { items ->
                _uiState.value = _uiState.value.copy(pendingItems = items)
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
            syncEngine.triggerSync(profileId)
        }
    }
}

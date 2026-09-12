package com.paradox.app.feature.askparadox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.intelligence.GroundedChatMessage
import com.paradox.app.domain.repository.AiSettingsRepository
import com.paradox.app.domain.usecase.askparadox.AskParadoxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class AskParadoxUiState(
    val messages: List<GroundedChatMessage> = emptyList(),
    val isThinking: Boolean = false,
    val isAiEnabled: Boolean = false,
    val hasApiKey: Boolean = false,
    val suggestedPrompts: List<String> = listOf(
        "How much can I safely spend today?",
        "🔥 Roast my spending habits this week",
        "💡 What if I save ₹500 every day?",
        "🛡️ Can I afford a ₹15,000 purchase?",
        "What is my Financial Health Score?",
        "Show my active subscriptions",
        "What was my biggest expense this month?",
        "Am I on track for my savings goals?"
    )
)

@HiltViewModel
class AskParadoxViewModel @Inject constructor(
    private val askParadoxUseCase: AskParadoxUseCase,
    private val sessionDataStore: SessionDataStore,
    private val aiSettingsRepository: AiSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AskParadoxUiState())
    val uiState: StateFlow<AskParadoxUiState> = _uiState.asStateFlow()

    init {
        // Welcome greeting message
        val greeting = GroundedChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            message = "Hello! I am **Ask Paradox**, your grounded financial AI companion.\n\nI answer your questions directly from your encrypted records and deterministic financial engines. What would you like to explore today?",
            timestamp = Instant.now()
        )
        _uiState.value = _uiState.value.copy(messages = listOf(greeting))

        viewModelScope.launch {
            aiSettingsRepository.isAiEnabled.collect { enabled ->
                _uiState.update { 
                    it.copy(
                        isAiEnabled = enabled,
                        hasApiKey = aiSettingsRepository.hasApiKey()
                    )
                }
            }
        }
    }

    fun submitQuery(query: String) {
        if (query.isBlank()) return
        if (!_uiState.value.isAiEnabled) return

        val userMsg = GroundedChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            message = query.trim(),
            timestamp = Instant.now()
        )

        val currentList = _uiState.value.messages.toMutableList().apply { add(userMsg) }
        _uiState.value = _uiState.value.copy(
            messages = currentList,
            isThinking = true
        )

        viewModelScope.launch {
            val profileId = sessionDataStore.activeProfileId.firstOrNull() ?: ""
            val response = askParadoxUseCase(profileId, query.trim())
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + response,
                isThinking = false
            )
        }
    }
}

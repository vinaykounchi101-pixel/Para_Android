package com.paradox.app.domain.usecase.ai

import com.paradox.app.domain.repository.AiSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class AiConfigurationStatus(
    val isEnabled: Boolean,
    val isKeyConfigured: Boolean
)

class GetAiStatusUseCase @Inject constructor(
    private val aiSettingsRepository: AiSettingsRepository
) {
    operator fun invoke(): Flow<AiConfigurationStatus> {
        return aiSettingsRepository.isAiEnabled.map { enabled ->
            AiConfigurationStatus(
                isEnabled = enabled,
                isKeyConfigured = aiSettingsRepository.hasApiKey()
            )
        }
    }
}

class SetAiEnabledUseCase @Inject constructor(
    private val aiSettingsRepository: AiSettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        return try {
            aiSettingsRepository.setAiEnabled(enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SaveApiKeyUseCase @Inject constructor(
    private val aiSettingsRepository: AiSettingsRepository
) {
    suspend operator fun invoke(apiKey: String): Result<Unit> {
        return aiSettingsRepository.saveApiKey(apiKey)
    }
}

class RemoveApiKeyUseCase @Inject constructor(
    private val aiSettingsRepository: AiSettingsRepository
) {
    suspend operator fun invoke(disableAi: Boolean = true): Result<Unit> {
        val removeResult = aiSettingsRepository.removeApiKey()
        if (removeResult.isSuccess && disableAi) {
            aiSettingsRepository.setAiEnabled(false)
        }
        return removeResult
    }
}

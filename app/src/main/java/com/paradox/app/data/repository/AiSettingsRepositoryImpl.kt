package com.paradox.app.data.repository

import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.core.security.SecureApiKeyStore
import com.paradox.app.domain.repository.AiSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSettingsRepositoryImpl @Inject constructor(
    private val sessionDataStore: SessionDataStore,
    private val secureApiKeyStore: SecureApiKeyStore
) : AiSettingsRepository {

    override val isAiEnabled: Flow<Boolean> = sessionDataStore.isAiEnabled

    override suspend fun setAiEnabled(enabled: Boolean) {
        sessionDataStore.setAiEnabled(enabled)
    }

    override fun hasApiKey(): Boolean {
        return secureApiKeyStore.hasApiKey()
    }

    override suspend fun saveApiKey(apiKey: String): Result<Unit> {
        return secureApiKeyStore.saveApiKey(apiKey)
    }

    override suspend fun getApiKey(): String? {
        return secureApiKeyStore.getApiKey()
    }

    override suspend fun removeApiKey(): Result<Unit> {
        return secureApiKeyStore.removeApiKey()
    }
}

package com.paradox.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface AiSettingsRepository {
    val isAiEnabled: Flow<Boolean>
    suspend fun setAiEnabled(enabled: Boolean)
    fun hasApiKey(): Boolean
    suspend fun saveApiKey(apiKey: String): Result<Unit>
    suspend fun getApiKey(): String?
    suspend fun removeApiKey(): Result<Unit>
}

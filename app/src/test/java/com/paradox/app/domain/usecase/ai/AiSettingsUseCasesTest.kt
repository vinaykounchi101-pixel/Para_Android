package com.paradox.app.domain.usecase.ai

import com.paradox.app.domain.repository.AiSettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiSettingsUseCasesTest {

    private val aiSettingsRepository: AiSettingsRepository = mockk(relaxed = true)

    private lateinit var getAiStatusUseCase: GetAiStatusUseCase
    private lateinit var setAiEnabledUseCase: SetAiEnabledUseCase
    private lateinit var saveApiKeyUseCase: SaveApiKeyUseCase
    private lateinit var removeApiKeyUseCase: RemoveApiKeyUseCase

    @Before
    fun setUp() {
        getAiStatusUseCase = GetAiStatusUseCase(aiSettingsRepository)
        setAiEnabledUseCase = SetAiEnabledUseCase(aiSettingsRepository)
        saveApiKeyUseCase = SaveApiKeyUseCase(aiSettingsRepository)
        removeApiKeyUseCase = RemoveApiKeyUseCase(aiSettingsRepository)
    }

    @Test
    fun `initial state shows AI disabled and key not configured`() = runTest {
        every { aiSettingsRepository.isAiEnabled } returns flowOf(false)
        every { aiSettingsRepository.hasApiKey() } returns false

        val status = getAiStatusUseCase().first()

        assertFalse(status.isEnabled)
        assertFalse(status.isKeyConfigured)
    }

    @Test
    fun `saving valid API key delegates to repository and succeeds`() = runTest {
        coEvery { aiSettingsRepository.saveApiKey("AIzaSyValidKey123") } returns Result.success(Unit)

        val result = saveApiKeyUseCase("AIzaSyValidKey123")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { aiSettingsRepository.saveApiKey("AIzaSyValidKey123") }
    }

    @Test
    fun `saving empty or blank API key fails validation`() = runTest {
        coEvery { aiSettingsRepository.saveApiKey(match { it.isBlank() }) } returns Result.failure(
            IllegalArgumentException("API Key cannot be empty")
        )

        val result = saveApiKeyUseCase("   ")

        assertTrue(result.isFailure)
        assertEquals("API Key cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `disabling AI preserves stored API key and disables feature`() = runTest {
        coEvery { aiSettingsRepository.setAiEnabled(false) } returns Unit
        every { aiSettingsRepository.hasApiKey() } returns true

        val result = setAiEnabledUseCase(false)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { aiSettingsRepository.setAiEnabled(false) }
        coVerify(exactly = 0) { aiSettingsRepository.removeApiKey() }
        assertTrue(aiSettingsRepository.hasApiKey())
    }

    @Test
    fun `re-enabling AI when key exists turns AI ON without clearing key`() = runTest {
        coEvery { aiSettingsRepository.setAiEnabled(true) } returns Unit
        every { aiSettingsRepository.hasApiKey() } returns true

        val result = setAiEnabledUseCase(true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { aiSettingsRepository.setAiEnabled(true) }
        assertTrue(aiSettingsRepository.hasApiKey())
    }

    @Test
    fun `removing API key deletes key and automatically disables AI`() = runTest {
        coEvery { aiSettingsRepository.removeApiKey() } returns Result.success(Unit)
        coEvery { aiSettingsRepository.setAiEnabled(false) } returns Unit

        val result = removeApiKeyUseCase(disableAi = true)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { aiSettingsRepository.removeApiKey() }
        coVerify(exactly = 1) { aiSettingsRepository.setAiEnabled(false) }
    }
}

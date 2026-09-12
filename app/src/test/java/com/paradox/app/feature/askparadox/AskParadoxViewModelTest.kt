package com.paradox.app.feature.askparadox

import com.paradox.app.core.datastore.SessionDataStore
import com.paradox.app.domain.model.intelligence.GroundedChatMessage
import com.paradox.app.domain.repository.AiSettingsRepository
import com.paradox.app.domain.usecase.askparadox.AskParadoxUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AskParadoxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val askParadoxUseCase: AskParadoxUseCase = mockk(relaxed = true)
    private val sessionDataStore: SessionDataStore = mockk(relaxed = true)
    private val aiSettingsRepository: AiSettingsRepository = mockk(relaxed = true)

    private lateinit var viewModel: AskParadoxViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { sessionDataStore.activeProfileId } returns flowOf("prof_1")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when AI is disabled, submitQuery does not invoke askParadoxUseCase`() = runTest {
        every { aiSettingsRepository.isAiEnabled } returns flowOf(false)
        every { aiSettingsRepository.hasApiKey() } returns false

        viewModel = AskParadoxViewModel(askParadoxUseCase, sessionDataStore, aiSettingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAiEnabled)

        viewModel.submitQuery("What is my budget?")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { askParadoxUseCase(any(), any()) }
        assertEquals(1, viewModel.uiState.value.messages.size) // Only initial greeting
    }

    @Test
    fun `when AI is enabled, submitQuery executes askParadoxUseCase and appends response`() = runTest {
        every { aiSettingsRepository.isAiEnabled } returns flowOf(true)
        every { aiSettingsRepository.hasApiKey() } returns true
        coEvery { askParadoxUseCase("prof_1", "What is my budget?") } returns GroundedChatMessage(
            id = "res_1",
            isUser = false,
            message = "Your budget is ₹50,000",
            timestamp = Instant.now()
        )

        viewModel = AskParadoxViewModel(askParadoxUseCase, sessionDataStore, aiSettingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAiEnabled)

        viewModel.submitQuery("What is my budget?")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { askParadoxUseCase("prof_1", "What is my budget?") }
        assertEquals(3, viewModel.uiState.value.messages.size) // Greeting + User + AI Response
    }
}

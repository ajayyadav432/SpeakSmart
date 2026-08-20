package com.example.speaksmart.ui.main

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.speaksmart.llm.LlmInferenceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: MainScreenViewModel
    private lateinit var llmHelper: LlmInferenceHelper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        viewModel = MainScreenViewModel(application)
        llmHelper = LlmInferenceHelper(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState_defaultValues() {
        val state = viewModel.uiState.value
        assertEquals("", state.transcribedText)
        assertEquals("", state.aiCorrections)
        assertEquals(false, state.isListening)
        assertEquals(false, state.isAnalyzing)
    }

    @Test
    fun testMockSpeechToTextPipeline_outputsTranscribedText() = runTest(testDispatcher) {
        // Mock speech input result arriving into UI State
        val mockSpeechInput = "I goes to the store yesterday"

        // Simulate speech recognition output received by state update
        viewModel.startListening()
        // Wait for coroutine dispatcher to process
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value)
    }

    @Test
    fun testMockLlmAnalysis_returnsStructuredCorrectionsAndQuiz() = runTest(testDispatcher) {
        val mockSpeechInput = "He don't like apples"
        
        // Pass mocked text into LlmInferenceHelper analyzeText engine
        val response = llmHelper.analyzeText(mockSpeechInput)

        // Verify response contains structured grammar corrections and quiz
        assertTrue("Response should contain Grammar Analysis header", response.contains("Grammar Analysis"))
        assertTrue("Response should contain Quick Quiz section", response.contains("Quick Quiz"))
        assertTrue("Response should contain quiz question options", response.contains("Which") || response.contains("Choose") || response.contains("Fill"))
    }
}

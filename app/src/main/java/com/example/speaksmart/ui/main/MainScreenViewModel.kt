package com.example.speaksmart.ui.main

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speaksmart.llm.DownloadStatus
import com.example.speaksmart.llm.LlmInferenceHelper
import com.example.speaksmart.llm.ModelDownloader
import com.example.speaksmart.speech.SpeechRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SpeakSmartUiState(
    val transcribedText: String = "",
    val partialText: String = "",
    val aiCorrections: String = "",
    val isListening: Boolean = false,
    val isAnalyzing: Boolean = false,
    val isModelLoaded: Boolean = false,
    val downloadStatus: DownloadStatus = DownloadStatus.Idle,
    val errorMessage: String? = null,
    val rmsLevel: Float = 0f,
    val sessionHistory: List<SessionEntry> = emptyList(),
)

data class SessionEntry(
    val speech: String,
    val corrections: String,
    val timestamp: Long = System.currentTimeMillis(),
)

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainScreenViewModel"
    }

    private val speechHelper = SpeechRecognitionHelper(application)
    private val llmHelper = LlmInferenceHelper(application)
    private val modelDownloader = ModelDownloader(application)

    private val _uiState = MutableStateFlow(SpeakSmartUiState())
    val uiState: StateFlow<SpeakSmartUiState> = _uiState.asStateFlow()

    init {
        // Observe speech recognition state changes
        viewModelScope.launch {
            speechHelper.state.collectLatest { state ->
                when (state) {
                    is SpeechRecognitionHelper.RecognitionState.Idle -> {
                        _uiState.value = _uiState.value.copy(isListening = false)
                    }
                    is SpeechRecognitionHelper.RecognitionState.Listening -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = true,
                            errorMessage = null
                        )
                    }
                    is SpeechRecognitionHelper.RecognitionState.PartialResult -> {
                        _uiState.value = _uiState.value.copy(partialText = state.text)
                    }
                    is SpeechRecognitionHelper.RecognitionState.Result -> {
                        _uiState.value = _uiState.value.copy(
                            transcribedText = state.text,
                            partialText = "",
                            isListening = false
                        )
                        // Automatically analyze the text
                        analyzeTranscription(state.text)
                    }
                    is SpeechRecognitionHelper.RecognitionState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isListening = false,
                            errorMessage = state.message
                        )
                    }
                }
            }
        }

        // Observe audio RMS levels for visual feedback
        viewModelScope.launch {
            speechHelper.rmsLevel.collectLatest { level ->
                _uiState.value = _uiState.value.copy(rmsLevel = level)
            }
        }

        // Observe model download status
        viewModelScope.launch {
            modelDownloader.status.collectLatest { status ->
                _uiState.value = _uiState.value.copy(downloadStatus = status)
                if (status is DownloadStatus.Completed) {
                    // Initialize LLM automatically after download completes
                    initializeLlm()
                }
            }
        }

        // Initial check and load of LLM
        initializeLlm()
    }

    private fun initializeLlm() {
        viewModelScope.launch {
            val loaded = llmHelper.initialize()
            _uiState.value = _uiState.value.copy(isModelLoaded = loaded)
            if (!loaded) {
                Log.i(TAG, "LLM model not available - using built-in rule analysis until downloaded")
            }
        }
    }

    fun startModelDownload(customUrl: String? = null) {
        viewModelScope.launch {
            val url = if (!customUrl.isNullOrBlank()) customUrl else ModelDownloader.DEFAULT_MODEL_URL
            modelDownloader.downloadModel(url)
        }
    }

    fun startListening() {
        if (!speechHelper.isAvailable()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Speech recognition not available on this device"
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            partialText = "",
            errorMessage = null
        )
        speechHelper.startListening()
    }

    fun stopListening() {
        speechHelper.stopListening()
    }

    private fun analyzeTranscription(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnalyzing = true, aiCorrections = "")
            try {
                val corrections = llmHelper.analyzeText(text)
                val entry = SessionEntry(speech = text, corrections = corrections)
                _uiState.value = _uiState.value.copy(
                    aiCorrections = corrections,
                    isAnalyzing = false,
                    sessionHistory = _uiState.value.sessionHistory + entry
                )
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    aiCorrections = "Analysis failed: ${e.message}",
                    isAnalyzing = false
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSession() {
        _uiState.value = SpeakSmartUiState(
            isModelLoaded = _uiState.value.isModelLoaded,
            downloadStatus = _uiState.value.downloadStatus
        )
        speechHelper.resetState()
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper.destroy()
        llmHelper.close()
    }
}

package com.example.speaksmart.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.speaksmart.data.ChatMessage
import com.example.speaksmart.data.MessageSender
import com.example.speaksmart.llm.LlmInferenceHelper
import com.example.speaksmart.speech.SpeechRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val isListening: Boolean = false,
    val isModelLoaded: Boolean = false,
    val errorMessage: String? = null,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
        
        val INITIAL_WELCOME_MESSAGE = ChatMessage(
            text = "Hello! 👋 I'm SpeakSmart AI, your personal English language tutor.\n\nAsk me anything about English grammar, vocabulary, sentence corrections, or practice a conversation with me!",
            sender = MessageSender.AI
        )
    }

    private val llmHelper = LlmInferenceHelper(application)
    private val speechHelper = SpeechRecognitionHelper(application)

    private val _uiState = MutableStateFlow(
        ChatUiState(messages = listOf(INITIAL_WELCOME_MESSAGE))
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Initialize LLM helper
        viewModelScope.launch {
            val loaded = llmHelper.initialize()
            _uiState.value = _uiState.value.copy(isModelLoaded = loaded)
        }

        // Observe speech recognition for voice input into chat
        viewModelScope.launch {
            speechHelper.state.collectLatest { state ->
                when (state) {
                    is SpeechRecognitionHelper.RecognitionState.Idle -> {
                        _uiState.value = _uiState.value.copy(isListening = false)
                    }
                    is SpeechRecognitionHelper.RecognitionState.Listening -> {
                        _uiState.value = _uiState.value.copy(isListening = true, errorMessage = null)
                    }
                    is SpeechRecognitionHelper.RecognitionState.PartialResult -> {
                        _uiState.value = _uiState.value.copy(inputText = state.text)
                    }
                    is SpeechRecognitionHelper.RecognitionState.Result -> {
                        _uiState.value = _uiState.value.copy(
                            inputText = state.text,
                            isListening = false
                        )
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
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage(promptText: String = _uiState.value.inputText) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(text = trimmed, sender = MessageSender.USER)
        val pendingAiMessage = ChatMessage(text = "...", sender = MessageSender.AI, isPending = true)

        val updatedMessages = _uiState.value.messages + userMessage + pendingAiMessage

        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            inputText = "",
            isGenerating = true
        )

        viewModelScope.launch {
            try {
                // Build history pairs
                val history = _uiState.value.messages
                    .filter { !it.isPending }
                    .chunked(2)
                    .mapNotNull { pair ->
                        if (pair.size == 2 && pair[0].sender == MessageSender.USER && pair[1].sender == MessageSender.AI) {
                            Pair(pair[0].text, pair[1].text)
                        } else null
                    }

                val aiResponseText = llmHelper.generateChatResponse(trimmed, history)
                val finalAiMessage = ChatMessage(id = pendingAiMessage.id, text = aiResponseText, sender = MessageSender.AI)

                // Replace pending message with final AI response
                val finalMessages = _uiState.value.messages.map { msg ->
                    if (msg.id == pendingAiMessage.id) finalAiMessage else msg
                }

                _uiState.value = _uiState.value.copy(
                    messages = finalMessages,
                    isGenerating = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Chat response failed: ${e.message}", e)
                val errorMessage = ChatMessage(
                    id = pendingAiMessage.id,
                    text = "Sorry, I encountered an error: ${e.message}",
                    sender = MessageSender.AI
                )
                val finalMessages = _uiState.value.messages.map { msg ->
                    if (msg.id == pendingAiMessage.id) errorMessage else msg
                }
                _uiState.value = _uiState.value.copy(
                    messages = finalMessages,
                    isGenerating = false
                )
            }
        }
    }

    fun startListening() {
        if (!speechHelper.isAvailable()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Speech recognition unavailable")
            return
        }
        speechHelper.startListening()
    }

    fun stopListening() {
        speechHelper.stopListening()
    }

    fun clearChat() {
        _uiState.value = _uiState.value.copy(
            messages = listOf(INITIAL_WELCOME_MESSAGE),
            inputText = "",
            isGenerating = false
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        speechHelper.destroy()
        llmHelper.close()
    }
}

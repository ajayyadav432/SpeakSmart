package com.crispr.ai.ui.chat

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crispr.ai.data.AiAgentPersona
import com.crispr.ai.data.ChatMessage
import com.crispr.ai.data.MessageSender
import com.crispr.ai.llm.LlmInferenceHelper
import com.crispr.ai.speech.SpeechRecognitionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ChatUiState(
    val selectedPersona: AiAgentPersona? = null,
    val messagesPerPersona: Map<String, List<ChatMessage>> = emptyMap(),
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val isListening: Boolean = false,
    val isModelLoaded: Boolean = false,
    val errorMessage: String? = null,
) {
    val currentMessages: List<ChatMessage>
        get() = selectedPersona?.let { persona ->
            messagesPerPersona[persona.id] ?: listOf(
                ChatMessage(text = persona.welcomeMessage, sender = MessageSender.AI)
            )
        } ?: emptyList()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val llmHelper = LlmInferenceHelper(application)
    private val speechHelper = SpeechRecognitionHelper(application)

    private val _uiState = MutableStateFlow(ChatUiState())
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

    fun selectPersona(persona: AiAgentPersona) {
        val currentMap = _uiState.value.messagesPerPersona.toMutableMap()
        if (!currentMap.containsKey(persona.id)) {
            currentMap[persona.id] = listOf(
                ChatMessage(text = persona.welcomeMessage, sender = MessageSender.AI)
            )
        }
        _uiState.value = _uiState.value.copy(
            selectedPersona = persona,
            messagesPerPersona = currentMap,
            inputText = ""
        )
    }

    fun switchPersona() {
        _uiState.value = _uiState.value.copy(selectedPersona = null, inputText = "")
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage(promptText: String = _uiState.value.inputText) {
        val persona = _uiState.value.selectedPersona ?: return
        val trimmed = promptText.trim()
        if (trimmed.isBlank() || _uiState.value.isGenerating) return

        val userMessage = ChatMessage(text = trimmed, sender = MessageSender.USER)
        val pendingAiMessage = ChatMessage(text = "...", sender = MessageSender.AI, isPending = true)

        val existingMessages = _uiState.value.currentMessages
        val updatedMessages = existingMessages + userMessage + pendingAiMessage

        val updatedMap = _uiState.value.messagesPerPersona.toMutableMap()
        updatedMap[persona.id] = updatedMessages

        _uiState.value = _uiState.value.copy(
            messagesPerPersona = updatedMap,
            inputText = "",
            isGenerating = true
        )

        viewModelScope.launch {
            try {
                val history = existingMessages
                    .filter { !it.isPending }
                    .chunked(2)
                    .mapNotNull { pair ->
                        if (pair.size == 2 && pair[0].sender == MessageSender.USER && pair[1].sender == MessageSender.AI) {
                            Pair(pair[0].text, pair[1].text)
                        } else null
                    }

                val aiResponseText = llmHelper.generateChatResponse(trimmed, history, persona)
                val finalAiMessage = ChatMessage(id = pendingAiMessage.id, text = aiResponseText, sender = MessageSender.AI)

                val activeList = _uiState.value.messagesPerPersona[persona.id] ?: emptyList()
                val finalMessages = activeList.map { msg ->
                    if (msg.id == pendingAiMessage.id) finalAiMessage else msg
                }

                val finalMap = _uiState.value.messagesPerPersona.toMutableMap()
                finalMap[persona.id] = finalMessages

                _uiState.value = _uiState.value.copy(
                    messagesPerPersona = finalMap,
                    isGenerating = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Chat response failed: ${e.message}", e)
                val errorMessage = ChatMessage(
                    id = pendingAiMessage.id,
                    text = "Sorry, I encountered an error: ${e.message}",
                    sender = MessageSender.AI
                )
                val activeList = _uiState.value.messagesPerPersona[persona.id] ?: emptyList()
                val finalMessages = activeList.map { msg ->
                    if (msg.id == pendingAiMessage.id) errorMessage else msg
                }
                val finalMap = _uiState.value.messagesPerPersona.toMutableMap()
                finalMap[persona.id] = finalMessages

                _uiState.value = _uiState.value.copy(
                    messagesPerPersona = finalMap,
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
        val persona = _uiState.value.selectedPersona ?: return
        val updatedMap = _uiState.value.messagesPerPersona.toMutableMap()
        updatedMap[persona.id] = listOf(
            ChatMessage(text = persona.welcomeMessage, sender = MessageSender.AI)
        )
        _uiState.value = _uiState.value.copy(
            messagesPerPersona = updatedMap,
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

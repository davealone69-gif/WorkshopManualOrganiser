package com.workshop.manualorganiser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshop.manualorganiser.ai.HermesGatewayClient
import com.workshop.manualorganiser.ai.OllamaClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isThinking: Boolean = false
)

class AIChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow(
        listOf(ChatMessage(text = "Workshop AI is ready. Connect Ollama in Termux to begin.", isUser = false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(text: String, useThinking: Boolean = false) {
        if (text.isBlank() || _isLoading.value) return
        val userMsg = ChatMessage(text = text.trim(), isUser = true)
        _messages.value = _messages.value + userMsg
        val aiId = java.util.UUID.randomUUID().toString()
        _messages.value = _messages.value + ChatMessage(
            id = aiId, text = "", isUser = false, isThinking = true
        )
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val history = _messages.value
                    .dropLast(1)
                    .filter { it.text.isNotBlank() && !it.isThinking }
                    .map { it.isUser to it.text }

                val response = HermesGatewayClient.chat(history, useThinking)
                    .recoverCatching { OllamaClient.chat(history, useThinking) }
                    .getOrThrow()

                _messages.value = _messages.value.map {
                    if (it.id == aiId) it.copy(text = response, isThinking = false) else it
                }
            } catch (e: Exception) {
                _messages.value = _messages.value.map {
                    if (it.id == aiId) it.copy(
                        text = "AI connection failed: ${e.message ?: "unknown error"}. Start Ollama with 'ollama serve' and try again.",
                        isThinking = false
                    ) else it
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}

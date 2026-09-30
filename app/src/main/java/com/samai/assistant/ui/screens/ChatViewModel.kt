package com.samai.assistant.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samai.assistant.ai.ModelRouter
import com.samai.assistant.voice.VoiceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val role: String, val content: String)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val modelRouter: ModelRouter,
    private val voiceManager: VoiceManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("assistant", "Hi! I'm SAM. How can I help you today?"))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val isListening: StateFlow<Boolean> = voiceManager.isListening

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val userMsg = ChatMessage("user", text)
            _messages.value = _messages.value + userMsg
            _isProcessing.value = true

            try {
                val response = modelRouter.processCommand(text)
                val assistantMsg = ChatMessage("assistant", response)
                _messages.value = _messages.value + assistantMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage("assistant", "Sorry, I couldn't process that. ${e.message ?: "Try again."}")
                _messages.value = _messages.value + errorMsg
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun startVoiceInput() {
        voiceManager.startListening()
        viewModelScope.launch {
            voiceManager.transcriptionFlow.collect { text ->
                if (text.isNotBlank() && !_isProcessing.value) {
                    sendMessage(text)
                }
            }
        }
    }
}

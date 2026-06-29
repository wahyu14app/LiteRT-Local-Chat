package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean
)

data class ChatUiState(
    val isModelLoaded: Boolean = false,
    val isLoadingModel: Boolean = false,
    val modelLoadError: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val errorEvent: String? = null
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    fun clearErrorEvent() {
        _uiState.update { it.copy(errorEvent = null) }
    }

    fun loadModel(context: Context, modelPath: String, useGpu: Boolean = true) {
        if (_uiState.value.isLoadingModel) return
        
        if (modelPath.isBlank()) {
            _uiState.update { it.copy(modelLoadError = "Please enter a valid model path.") }
            return
        }
        
        if (!java.io.File(modelPath).exists()) {
            _uiState.update { it.copy(modelLoadError = "File not found: $modelPath. Ensure the model is pushed to the device.") }
            return
        }
        
        _uiState.update { it.copy(isLoadingModel = true, modelLoadError = null) }
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val backend = if (useGpu) Backend.GPU() else Backend.CPU()
                    val config = EngineConfig(
                        modelPath = modelPath,
                        backend = backend,
                        cacheDir = context.cacheDir.path
                    )
                    
                    val newEngine = Engine(config)
                    newEngine.initialize()
                    
                    engine?.close()
                    engine = newEngine
                    conversation = newEngine.createConversation()
                }
                
                _uiState.update { 
                    it.copy(
                        isModelLoaded = true, 
                        isLoadingModel = false,
                        messages = listOf(ChatMessage(text = "Model loaded successfully! You can start chatting.", isUser = false))
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingModel = false, 
                        modelLoadError = e.message ?: "Unknown error occurred"
                    ) 
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(errorEvent = "Message cannot be empty.") }
            return
        }
        if (_uiState.value.isGenerating) {
            _uiState.update { it.copy(errorEvent = "Please wait for the model to finish generating.") }
            return
        }
        if (conversation == null) {
            _uiState.update { it.copy(errorEvent = "Conversation is not initialized. Please load the model first.") }
            return
        }
        
        val userMsg = ChatMessage(text = text, isUser = true)
        val initialModelMsg = ChatMessage(text = "", isUser = false)
        
        _uiState.update { state -> 
            state.copy(
                messages = state.messages + userMsg + initialModelMsg,
                isGenerating = true
            ) 
        }
        
        val currentConversation = conversation ?: return
        
        viewModelScope.launch {
            try {
                val flow = currentConversation.sendMessageAsync(text)
                var fullResponse = ""
                
                flow.catch { e ->
                     _uiState.update { state ->
                         val msgs = state.messages.toMutableList()
                         if (msgs.isNotEmpty()) {
                             val last = msgs.removeLast()
                             msgs.add(last.copy(text = last.text + "\n\n⚠️ Error during generation: ${e.localizedMessage ?: "Unknown error"}"))
                         }
                         state.copy(
                             messages = msgs, 
                             isGenerating = false,
                             errorEvent = "Model generation failed: ${e.localizedMessage}"
                         )
                     }
                }.collect { messageUpdate ->
                    fullResponse += messageUpdate.toString()
                    _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        if (msgs.isNotEmpty()) {
                            val last = msgs.removeLast()
                            msgs.add(last.copy(text = fullResponse))
                        }
                        state.copy(messages = msgs)
                    }
                }
                
                _uiState.update { it.copy(isGenerating = false) }
                
            } catch (e: Exception) {
                _uiState.update { state ->
                     val msgs = state.messages.toMutableList()
                     if (msgs.isNotEmpty()) {
                         val last = msgs.removeLast()
                         msgs.add(last.copy(text = last.text + "\n\n⚠️ Execution error: ${e.localizedMessage ?: "Unknown error"}"))
                     }
                     state.copy(
                         messages = msgs, 
                         isGenerating = false,
                         errorEvent = "An unexpected error occurred: ${e.localizedMessage}"
                     )
                 }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        conversation?.close()
        engine?.close()
    }
}

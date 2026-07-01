package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AiEngineApp
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
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
    val currentModelName: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val errorEvent: String? = null,
    val activeProject: java.io.File? = null,
    val currentSessionId: String = "default"
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AiEngineApp

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val messageQueue = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            loadConversation()
        }
        
        viewModelScope.launch {
            for (text in messageQueue) {
                processMessage(text)
            }
        }
    }

    private fun loadConversation() {
        val targetDir = _uiState.value.activeProject ?: AppConfig.CONVERSATIONS_DIR
        val targetFile = java.io.File(targetDir, "chat_history.json")
        if (targetFile.exists()) {
            try {
                val content = targetFile.readText()
                val parsed = parseJsonMessages(content)
                if (parsed.isNotEmpty()) {
                    _uiState.update { it.copy(messages = parsed) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveConversation() {
        val targetDir = _uiState.value.activeProject ?: AppConfig.CONVERSATIONS_DIR
        val targetFile = java.io.File(targetDir, "chat_history.json")
        try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val jsonArray = _uiState.value.messages.joinToString(
                prefix = "[\n", separator = ",\n", postfix = "\n]"
            ) { msg ->
                val escapedText = msg.text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")
                "  {\"text\":\"${escapedText}\",\"isUser\":${msg.isUser}}"
            }
            targetFile.writeText(jsonArray)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseJsonMessages(json: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        val regex = Regex("\\{\"text\":\"(.*?)\",\"isUser\":(true|false)\\}")
        regex.findAll(json).forEach { match ->
            val text = match.groupValues[1].replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
            val isUser = match.groupValues[2].toBoolean()
            messages.add(ChatMessage(text = text, isUser = isUser))
        }
        return messages
    }

    fun clearErrorEvent() {
        _uiState.update { it.copy(errorEvent = null) }
    }

    fun setActiveProject(project: java.io.File?) {
        _uiState.update { it.copy(activeProject = project) }
        if (engine != null) {
            conversation?.close()
            conversation = engine?.createConversation()
        }
    }

    fun loadModel(context: Context, rawModelPath: String, useGpu: Boolean = true) {
        val modelPath = rawModelPath.trim()
        if (_uiState.value.isLoadingModel) return
        
        if (modelPath.isBlank()) {
            _uiState.update { it.copy(modelLoadError = "Please enter a valid model path.") }
            return
        }
        
        val file = java.io.File(modelPath)
        if (!file.exists()) {
            _uiState.update { it.copy(modelLoadError = "File not found: $modelPath. Ensure the model is pushed to the device and permissions are granted.") }
            return
        }
        if (!file.canRead()) {
            _uiState.update { it.copy(modelLoadError = "Permission denied to read: $modelPath. Please grant All Files Access in Settings.") }
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
                    AiEngineManager.engine = newEngine
                }
                
                _uiState.update { 
                    it.copy(
                        isModelLoaded = true, 
                        isLoadingModel = false,
                        currentModelName = file.name
                    ) 
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: e.message ?: "Unknown error occurred"
                val hint = if (errorMsg.contains("PERMISSION_DENIED", ignoreCase = true) || errorMsg.contains("Permission denied", ignoreCase = true)) {
                    "\nHint: Ensure 'All files access' permission is granted to this app in Android Settings."
                } else ""
                
                _uiState.update { 
                    it.copy(
                        isLoadingModel = false, 
                        modelLoadError = "Error: $errorMsg$hint"
                    ) 
                }
            }
        }
    }

    fun clearConversation() {
        val sessionId = _uiState.value.currentSessionId
        viewModelScope.launch {
            _uiState.update { it.copy(messages = emptyList()) }
            saveConversation()
            conversation?.close()
            conversation = engine?.createConversation()
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) {
            _uiState.update { it.copy(errorEvent = "Message cannot be empty.") }
            return
        }
        if (conversation == null) {
            _uiState.update { it.copy(errorEvent = "Conversation is not initialized. Please load the model first.") }
            return
        }
        
        val userMsg = ChatMessage(text = text, isUser = true)
        _uiState.update { state -> 
            state.copy(messages = state.messages + userMsg)
        }
        saveConversation()
        
        messageQueue.trySend(text)
    }

    private suspend fun processMessage(text: String) {
        val currentConversation = conversation ?: return
        val sessionId = _uiState.value.currentSessionId
        
        val initialModelMsg = ChatMessage(text = "", isUser = false)
        _uiState.update { state -> 
            state.copy(
                messages = state.messages + initialModelMsg,
                isGenerating = true
            ) 
        }
        
        val currentProject = _uiState.value.activeProject
        val promptText = if (currentProject != null) {
            val instructions = java.io.File(currentProject, "instructions.txt").takeIf { it.exists() }?.readText() ?: ""
            "System Instructions:\n$instructions\n\nUser Input:\n$text"
        } else {
            text
        }
        
        try {
            val flow = currentConversation.sendMessageAsync(promptText)
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
                 saveConversation()
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
            saveConversation()
            
            // Process tool calls if in project mode
            if (currentProject != null) {
                processToolCalls(fullResponse, currentProject, sessionId)
            }
            
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
             saveConversation()
        }
    }

    private suspend fun processToolCalls(response: String, projectDir: java.io.File, sessionId: String) {
        var action = ""
        var arg = ""
        var content = ""
        var foundTool = false

        val xmlRegex = Regex("<tool\\s+action=\"([^\"]+)\"\\s+(?:path|query)=\"([^\"]+)\"\\s*>(.*?)</tool>|<tool\\s+action=\"([^\"]+)\"\\s+(?:path|query)=\"([^\"]+)\"\\s*/>", RegexOption.DOT_MATCHES_ALL)
        val xmlMatch = xmlRegex.find(response)
        
        if (xmlMatch != null) {
            action = xmlMatch.groups[1]?.value ?: xmlMatch.groups[4]?.value ?: ""
            arg = xmlMatch.groups[2]?.value ?: xmlMatch.groups[5]?.value ?: ""
            content = xmlMatch.groups[3]?.value ?: ""
            foundTool = true
        } else {
            val jsonRegex = Regex("\\{\\s*\"action\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"(?:path|query)\"\\s*:\\s*\"([^\"]+)\"(?:\\s*,\\s*\"content\"\\s*:\\s*\"(.*?)\")?\\s*\\}", RegexOption.DOT_MATCHES_ALL)
            val jsonMatch = jsonRegex.find(response)
            if (jsonMatch != null) {
                action = jsonMatch.groups[1]?.value ?: ""
                arg = jsonMatch.groups[2]?.value ?: ""
                content = jsonMatch.groups[3]?.value?.replace("\\n", "\n")?.replace("\\\"", "\"")?.replace("\\\\", "\\") ?: ""
                foundTool = true
            }
        }
        
        if (foundTool) {
            val dataDir = java.io.File(projectDir, "data")
            val targetFile = java.io.File(dataDir, arg)
            
            var toolResult = ""
            try {
                when (action) {
                    "write_file" -> {
                        targetFile.parentFile?.mkdirs()
                        targetFile.writeText(content)
                        toolResult = "File $arg written successfully."
                    }
                    "read_file" -> {
                        if (targetFile.exists()) {
                            toolResult = "Content of $arg:\n" + targetFile.readText()
                        } else {
                            toolResult = "Error: File $arg not found."
                        }
                    }
                    "web_search" -> {
                        val result = withContext(Dispatchers.IO) {
                            try {
                                val url = java.net.URL("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${java.net.URLEncoder.encode(arg, "UTF-8")}&utf8=&format=json")
                                val connection = url.openConnection() as java.net.HttpURLConnection
                                connection.requestMethod = "GET"
                                val inputStream = connection.inputStream
                                val responseText = inputStream.bufferedReader().use { it.readText() }
                                
                                val snippetRegex = Regex("\"snippet\":\"(.*?)\"")
                                val snippets = snippetRegex.findAll(responseText).map { it.groupValues[1] }.take(3).toList()
                                
                                if (snippets.isEmpty()) "No results found for $arg"
                                else "Web search results for '$arg':\n- " + snippets.joinToString("\n- ") { it.replace(Regex("<[^>]*>"), "") }
                            } catch (e: Exception) {
                                "Failed to search the web: ${e.message}"
                            }
                        }
                        toolResult = result
                    }
                    else -> {
                        toolResult = "Error: Unknown action $action."
                    }
                }
            } catch (e: Exception) {
                toolResult = "Error executing $action: ${e.message}"
            }
            
            sendMessage("[System: Tool Execution Result]\n$toolResult")
        }
    }

    override fun onCleared() {
        super.onCleared()
        conversation?.close()
        engine?.close()
    }
}

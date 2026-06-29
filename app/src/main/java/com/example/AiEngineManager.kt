package com.example

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

object AiEngineManager {
    var engine: Engine? = null
    private var defaultConversation: Conversation? = null
    
    // session_id to Conversation mapping (LiteRT doesn't easily support multiple concurrent conversations on one engine, wait, does it? `engine.createConversation()`)
    private val conversations = mutableMapOf<String, Conversation>()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    data class PromptRequest(
        val context: Context,
        val prompt: String,
        val sessionId: String,
        val replyAction: String?
    )
    
    private val queue = Channel<PromptRequest>(Channel.UNLIMITED)
    
    init {
        scope.launch {
            for (request in queue) {
                processRequest(request)
            }
        }
    }
    
    fun enqueuePrompt(context: Context, prompt: String, sessionId: String, replyAction: String?) {
        queue.trySend(PromptRequest(context.applicationContext, prompt, sessionId, replyAction))
    }
    
    private suspend fun processRequest(request: PromptRequest) {
        if (engine == null) {
            sendReply(request, "Error: AI Model is not loaded. Please open LiteRT Chat and load a model first.")
            return
        }
        
        val conv = conversations.getOrPut(request.sessionId) {
            engine!!.createConversation()
        }
        
        // Restore history if this is a new conversation object
        val sessionDir = File(AppConfig.CONVERSATIONS_DIR, request.sessionId)
        if (!sessionDir.exists()) sessionDir.mkdirs()
        val historyFile = File(sessionDir, "chat_history.json")
        // Note: we can't easily push history into `Conversation` unless the API supports it, but we can maintain our own JSON log.
        // Actually, LiteRT doesn't have an addMessage for system/history? We can just append to the file.
        
        appendHistory(historyFile, request.prompt, true)
        
        try {
            val flow = conv.sendMessageAsync(request.prompt)
            var fullResponse = ""
            
            flow.catch { e ->
                val errorMsg = "Error during generation: ${e.localizedMessage}"
                appendHistory(historyFile, errorMsg, false)
                sendReply(request, errorMsg)
            }.collect { messageUpdate ->
                fullResponse += messageUpdate.toString()
            }
            
            appendHistory(historyFile, fullResponse, false)
            
            // Check for tool calls (web search etc)
            val finalResponse = processToolCallsIfNeeded(fullResponse, sessionDir, conv)
            if (finalResponse != fullResponse) {
               appendHistory(historyFile, finalResponse, false)
            }
            
            sendReply(request, finalResponse.ifBlank { fullResponse })
            
        } catch (e: Exception) {
            val errorMsg = "Execution error: ${e.localizedMessage}"
            appendHistory(historyFile, errorMsg, false)
            sendReply(request, errorMsg)
        }
    }
    
    private fun sendReply(request: PromptRequest, response: String) {
        if (request.replyAction != null) {
            val intent = Intent(request.replyAction)
            intent.putExtra("response", response)
            intent.putExtra("session_id", request.sessionId)
            intent.setPackage(request.replyAction.substringBefore(".ACTION", "")) // try to restrict
            request.context.sendBroadcast(intent)
        }
    }
    
    private fun appendHistory(file: File, text: String, isUser: Boolean) {
        try {
            val jsonArray = if (file.exists()) {
                JSONArray(file.readText())
            } else {
                JSONArray()
            }
            
            val obj = JSONObject()
            obj.put("text", text)
            obj.put("isUser", isUser)
            jsonArray.put(obj)
            
            file.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            Log.e("AiEngine", "Failed to append history", e)
        }
    }
    
    private suspend fun processToolCallsIfNeeded(response: String, sessionDir: File, conv: Conversation): String {
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
            val dataDir = File(sessionDir, "data")
            if (!dataDir.exists()) dataDir.mkdirs()
            
            if (action == "write_file") {
                try {
                    File(dataDir, arg).writeText(content)
                    return response + "\n\n[System: Successfully wrote to file $arg]"
                } catch (e: Exception) {
                    return response + "\n\n[System: Failed to write file: ${e.localizedMessage}]"
                }
            } else if (action == "read_file") {
                try {
                    val fileContent = File(dataDir, arg).readText()
                    // Send it back to the model
                    val toolPrompt = "Tool result for reading file $arg:\n$fileContent"
                    val flow = conv.sendMessageAsync(toolPrompt)
                    var toolResponse = ""
                    flow.collect { toolResponse += it.toString() }
                    return response + "\n\n" + toolResponse
                } catch (e: Exception) {
                    return response + "\n\n[System: Failed to read file: ${e.localizedMessage}]"
                }
            } else if (action == "web_search") {
                try {
                    val encodedArg = java.net.URLEncoder.encode(arg, "UTF-8")
                    val url = java.net.URL("https://html.duckduckgo.com/html/?q=$encodedArg")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    
                    val snippetRegex = Regex("class=\"result__snippet[^\"]*\">([^<]*)</a>")
                    val snippets = snippetRegex.findAll(responseText).map { it.groupValues[1].replace(Regex("<[^>]*>"), "").trim() }.take(3).toList()
                    
                    val searchResult = if (snippets.isEmpty()) "No results found for $arg" else "Web search results for '$arg':\n- " + snippets.joinToString("\n- ")
                    
                    // Feed back to model
                    val toolPrompt = "Tool result for web search '$arg':\n$searchResult"
                    val flow = conv.sendMessageAsync(toolPrompt)
                    var toolResponse = ""
                    flow.collect { toolResponse += it.toString() }
                    return response + "\n\n" + toolResponse
                } catch (e: Exception) {
                    return response + "\n\n[System: Web search failed: ${e.localizedMessage}]"
                }
            }
        }
        
        return response
    }
}

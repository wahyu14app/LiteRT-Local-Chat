package com.example

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import java.io.File
import com.example.core.AiEngineApp
import com.example.database.entity.ChatMessageEntity
import com.example.database.entity.ChatSessionEntity
import com.example.queue.PromptQueueManager
import com.example.queue.PromptRequest
import kotlinx.coroutines.flow.first

object AiEngineManager {
    var engine: Engine? = null
    
    // session_id to Conversation mapping
    private val conversations = mutableMapOf<String, Conversation>()
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private lateinit var app: AiEngineApp
    
    val queueManager by lazy {
        PromptQueueManager(scope) { request ->
            processRequest(request)
        }
    }
    
    fun init(application: AiEngineApp) {
        this.app = application
    }
    
    fun enqueuePrompt(context: Context, prompt: String, sessionId: String, replyAction: String?) {
        queueManager.enqueue(PromptRequest(sessionId, prompt, replyAction) { response ->
            sendReply(context.applicationContext, replyAction, sessionId, response)
        })
    }
    
    private suspend fun processRequest(request: PromptRequest) {
        val chatDao = app.database.chatDao()
        val settingsRepo = app.settingsRepository
        
        // Ensure session exists
        chatDao.insertSession(ChatSessionEntity(sessionId = request.sessionId, title = "Chat ${request.sessionId.take(4)}"))
        
        // Save User Message
        chatDao.insertMessage(ChatMessageEntity(sessionId = request.sessionId, role = "user", content = request.prompt))
        
        if (engine == null) {
            val errorMsg = "Error: AI Model is not loaded. Please open Local AI Engine and load a model first."
            chatDao.insertMessage(ChatMessageEntity(sessionId = request.sessionId, role = "system", content = errorMsg))
            request.callback?.invoke(errorMsg)
            return
        }
        
        val conv = conversations.getOrPut(request.sessionId) {
            engine!!.createConversation()
        }
        
        try {
            val flow = conv.sendMessageAsync(request.prompt)
            var fullResponse = ""
            
            flow.catch { e ->
                val errorMsg = "Error during generation: ${e.localizedMessage}"
                chatDao.insertMessage(ChatMessageEntity(sessionId = request.sessionId, role = "system", content = errorMsg))
                request.callback?.invoke(errorMsg)
            }.collect { messageUpdate ->
                fullResponse += messageUpdate.toString()
            }
            
            chatDao.insertMessage(ChatMessageEntity(sessionId = request.sessionId, role = "model", content = fullResponse))
            
            request.callback?.invoke(fullResponse)
            
        } catch (e: Exception) {
            val errorMsg = "Execution error: ${e.localizedMessage}"
            chatDao.insertMessage(ChatMessageEntity(sessionId = request.sessionId, role = "system", content = errorMsg))
            request.callback?.invoke(errorMsg)
        }
    }
    
    private fun sendReply(context: Context, replyAction: String?, sessionId: String, response: String) {
        if (replyAction != null) {
            val intent = Intent(replyAction)
            intent.putExtra("response", response)
            intent.putExtra("session_id", sessionId)
            intent.setPackage(replyAction.substringBefore(".ACTION", ""))
            context.sendBroadcast(intent)
        }
    }
}

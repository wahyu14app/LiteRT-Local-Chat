package com.example.queue

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

data class PromptRequest(
    val sessionId: String,
    val prompt: String,
    val replyAction: String? = null,
    val callback: ((String) -> Unit)? = null
)

class PromptQueueManager(
    private val scope: CoroutineScope,
    private val processRequest: suspend (PromptRequest) -> Unit
) {
    private val queue = Channel<PromptRequest>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (request in queue) {
                try {
                    processRequest(request)
                } catch (e: Exception) {
                    Log.e("PromptQueueManager", "Error processing request for session ${request.sessionId}", e)
                    request.callback?.invoke("Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun enqueue(request: PromptRequest) {
        queue.trySend(request)
    }
}

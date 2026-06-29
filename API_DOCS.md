# Background Engine API

LiteRT Chat now acts as a background AI engine for other applications. 

## How it works:
1. Ensure LiteRT Chat is opened and a model is successfully loaded in the UI. 
2. The EngineService runs in the background.
3. From another application, you can send a broadcast intent to LiteRT Chat.
4. LiteRT Chat receives it, processes it through a queue, supports web searching if needed, saves history to `/.lite-rt-local-chat/conversations/{session_id}/chat_history.json`, and broadcasts the response back.

## Intent Specification

### 1. Send Prompt to LiteRT Chat
**Action**: `com.example.litert.ACTION_PROMPT`

**Extras (Key-Value):**
- `prompt` (String): Your text prompt.
- `session_id` (String): Optional. Used to group chat history in a specific folder.
- `reply_action` (String): Optional. The intent action that LiteRT Chat should use when broadcasting the response back to your app.

**Example Code (Kotlin in App1):**
```kotlin
val intent = Intent("com.example.litert.ACTION_PROMPT")
intent.putExtra("prompt", "Halo, apa kabar?")
intent.putExtra("session_id", "app1_session")
intent.putExtra("reply_action", "com.app1.ACTION_RECEIVE_RESPONSE")
sendBroadcast(intent)
```

### 2. Receive Response in App1
Register a `BroadcastReceiver` in your app listening to your `reply_action` (e.g. `com.app1.ACTION_RECEIVE_RESPONSE`).

**Extras received in Broadcast:**
- `response` (String): The generated AI response.
- `session_id` (String): The session ID that was passed in.

**Example Code (Kotlin in App1):**
```kotlin
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.app1.ACTION_RECEIVE_RESPONSE") {
            val response = intent.getStringExtra("response")
            val sessionId = intent.getStringExtra("session_id")
            Log.d("App1", "Received from LiteRT: $response")
        }
    }
}
val filter = IntentFilter("com.app1.ACTION_RECEIVE_RESPONSE")
registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
```

package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class EngineService : Service() {
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.litert.ACTION_PROMPT") {
                val prompt = intent.getStringExtra("prompt") ?: return
                val sessionId = intent.getStringExtra("session_id") ?: "default"
                val replyAction = intent.getStringExtra("reply_action")
                
                AiEngineManager.enqueuePrompt(context, prompt, sessionId, replyAction)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.example.AiEngineManager.init(application as com.example.core.AiEngineApp)
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "EngineChannel")
            .setContentTitle("LiteRT Engine")
            .setContentText("Running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)
        
        val filter = IntentFilter("com.example.litert.ACTION_PROMPT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("EngineChannel", "Engine Service", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

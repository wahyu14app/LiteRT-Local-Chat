package com.example

import android.os.Environment
import java.io.File

object AppConfig {
    val BASE_DIR = File(Environment.getExternalStorageDirectory(), ".lite-rt-local-chat")
    
    val MODELS_DIR = File(BASE_DIR, "models")
    val MODELS_CPU_DIR = File(MODELS_DIR, "cpu")
    val MODELS_GPU_DIR = File(MODELS_DIR, "gpu")
    
    val PROJECTS_DIR = File(BASE_DIR, "projects")
    val CONVERSATIONS_DIR = File(BASE_DIR, "conversations")
    val SETTINGS_DIR = File(BASE_DIR, "settings")

    fun ensureDirectoriesExist() {
        val dirs = listOf(
            BASE_DIR,
            MODELS_DIR,
            MODELS_CPU_DIR,
            MODELS_GPU_DIR,
            PROJECTS_DIR,
            CONVERSATIONS_DIR,
            SETTINGS_DIR
        )
        for (dir in dirs) {
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }
}

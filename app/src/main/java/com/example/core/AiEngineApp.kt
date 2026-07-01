package com.example.core

import android.app.Application
import com.example.settings.SettingsRepository
import com.example.settings.dataStore

class AiEngineApp : Application() {
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(dataStore)
    }
}

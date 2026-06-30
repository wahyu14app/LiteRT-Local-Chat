package com.example.core

import android.app.Application
import com.example.database.AppDatabase
import com.example.settings.SettingsRepository
import com.example.settings.dataStore

class AiEngineApp : Application() {
    lateinit var database: AppDatabase
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(dataStore)
    }
}

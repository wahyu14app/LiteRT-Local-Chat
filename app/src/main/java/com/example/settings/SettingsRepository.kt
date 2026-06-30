package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    companion object {
        val TEMPERATURE = floatPreferencesKey("temperature")
        val TOP_K = intPreferencesKey("top_k")
        val TOP_P = floatPreferencesKey("top_p")
        val SEED = intPreferencesKey("seed")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val THINKING_ENABLED = booleanPreferencesKey("thinking_enabled")
        val MAX_TOKENS = intPreferencesKey("max_tokens")
    }

    val temperature: Flow<Float> = dataStore.data.map { it[TEMPERATURE] ?: 0.8f }
    val topK: Flow<Int> = dataStore.data.map { it[TOP_K] ?: 40 }
    val topP: Flow<Float> = dataStore.data.map { it[TOP_P] ?: 0.9f }
    val systemPrompt: Flow<String> = dataStore.data.map { it[SYSTEM_PROMPT] ?: "You are a helpful AI assistant." }
    val thinkingEnabled: Flow<Boolean> = dataStore.data.map { it[THINKING_ENABLED] ?: false }
    val maxTokens: Flow<Int> = dataStore.data.map { it[MAX_TOKENS] ?: 1024 }

    suspend fun setTemperature(value: Float) { dataStore.edit { it[TEMPERATURE] = value } }
    suspend fun setSystemPrompt(value: String) { dataStore.edit { it[SYSTEM_PROMPT] = value } }
    suspend fun setThinkingEnabled(value: Boolean) { dataStore.edit { it[THINKING_ENABLED] = value } }
    suspend fun setTopK(value: Int) { dataStore.edit { it[TOP_K] = value } }
    suspend fun setMaxTokens(value: Int) { dataStore.edit { it[MAX_TOKENS] = value } }
}

package com.photocleaner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL = stringPreferencesKey("model_name")
        private val SELECTED_DIRS = stringPreferencesKey("selected_directories")
        private val BATCH_SIZE = stringPreferencesKey("batch_size")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY] ?: ""
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BASE_URL] ?: "https://api.openai.com/"
    }

    val model: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL] ?: "mimo-v2.5"
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[BASE_URL] = url
        }
    }

    suspend fun setModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[MODEL] = model
        }
    }

    suspend fun getApiKeySync(): String {
        return context.dataStore.data.first()[API_KEY] ?: ""
    }

    suspend fun getBaseUrlSync(): String {
        return context.dataStore.data.first()[BASE_URL] ?: "https://api.openai.com/"
    }

    suspend fun getModelSync(): String {
        return context.dataStore.data.first()[MODEL] ?: "mimo-v2.5"
    }

    val selectedDirectories: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val stored = preferences[SELECTED_DIRS]
        if (stored.isNullOrBlank()) {
            emptySet()
        } else {
            stored.split(",").filter { it.isNotBlank() }.toSet()
        }
    }

    val batchSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BATCH_SIZE]?.toIntOrNull() ?: 100
    }

    suspend fun setBatchSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[BATCH_SIZE] = size.toString()
        }
    }

    suspend fun getBatchSizeSync(): Int {
        return context.dataStore.data.first()[BATCH_SIZE]?.toIntOrNull() ?: 100
    }

    suspend fun setSelectedDirectories(dirs: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_DIRS] = dirs.joinToString(",")
        }
    }

    suspend fun getSelectedDirectoriesSync(): Set<String> {
        val stored = context.dataStore.data.first()[SELECTED_DIRS]
        return if (stored.isNullOrBlank()) {
            emptySet()
        } else {
            stored.split(",").filter { it.isNotBlank() }.toSet()
        }
    }
}

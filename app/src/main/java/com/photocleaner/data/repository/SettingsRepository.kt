package com.photocleaner.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secret_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL = stringPreferencesKey("model_name")
        private val SELECTED_DIRS = stringPreferencesKey("selected_directories")
        private val BATCH_SIZE = stringPreferencesKey("batch_size")
    }

    @Volatile
    private var cachedApiKey: String = securePrefs.getString("api_key", "") ?: ""

    @Volatile
    private var cachedBaseUrl: String = "https://api.openai.com/"

    init {
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.data.collect { preferences ->
                cachedBaseUrl = preferences[BASE_URL] ?: "https://api.openai.com/"
            }
        }
    }

    private val _apiKeyFlow = MutableStateFlow(cachedApiKey)
    val apiKey: Flow<String> = _apiKeyFlow.asStateFlow()

    val baseUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[BASE_URL] ?: "https://api.openai.com/"
    }

    val model: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[MODEL] ?: "mimo-v2.5"
    }

    suspend fun setApiKey(key: String) {
        securePrefs.edit().putString("api_key", key).apply()
        cachedApiKey = key
        _apiKeyFlow.value = key
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[BASE_URL] = url
        }
        cachedBaseUrl = url
    }

    suspend fun setModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[MODEL] = model
        }
    }

    suspend fun getApiKeySync(): String {
        return cachedApiKey
    }

    fun getApiKeySyncMemory(): String = cachedApiKey

    fun getBaseUrlSyncMemory(): String = cachedBaseUrl

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

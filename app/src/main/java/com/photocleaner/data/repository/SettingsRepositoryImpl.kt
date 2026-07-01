package com.photocleaner.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.photocleaner.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    companion object {
        private val SELECTED_DIRS = stringSetPreferencesKey("selected_directories_v2")
        private val LEGACY_SELECTED_DIRS = stringPreferencesKey("selected_directories")
        private val BATCH_SIZE = stringPreferencesKey("batch_size")
        private val RESCAN_EXISTING = booleanPreferencesKey("rescan_existing_photos")
    }

    override val selectedDirectories: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_DIRS]
            ?: preferences[LEGACY_SELECTED_DIRS]?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()
    }

    override val batchSize: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[BATCH_SIZE]?.toIntOrNull() ?: 2000
    }

    override val rescanExistingPhotos: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[RESCAN_EXISTING] ?: false
    }

    override suspend fun setBatchSize(size: Int) {
        context.dataStore.edit { preferences -> preferences[BATCH_SIZE] = size.toString() }
    }

    override suspend fun getBatchSizeSync(): Int =
        context.dataStore.data.first()[BATCH_SIZE]?.toIntOrNull() ?: 2000

    override suspend fun setRescanExistingPhotos(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[RESCAN_EXISTING] = enabled }
    }

    override suspend fun getRescanExistingPhotosSync(): Boolean =
        context.dataStore.data.first()[RESCAN_EXISTING] ?: false

    override suspend fun setSelectedDirectories(dirs: Set<String>) {
        context.dataStore.edit { preferences -> preferences[SELECTED_DIRS] = dirs }
    }

    override suspend fun getSelectedDirectoriesSync(): Set<String> {
        val preferences = context.dataStore.data.first()
        return preferences[SELECTED_DIRS]
            ?: preferences[LEGACY_SELECTED_DIRS]?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()
    }
}

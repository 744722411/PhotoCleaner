package com.photocleaner.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Abstract settings access so ViewModels depend on a testable contract rather
 * than a concrete DataStore implementation.
 */
interface SettingsRepository {
    val selectedDirectories: Flow<Set<String>>
    val batchSize: Flow<Int>
    val rescanExistingPhotos: Flow<Boolean>
    suspend fun setBatchSize(size: Int)
    suspend fun getBatchSizeSync(): Int
    suspend fun setRescanExistingPhotos(enabled: Boolean)
    suspend fun getRescanExistingPhotosSync(): Boolean
    suspend fun setSelectedDirectories(dirs: Set<String>)
    suspend fun getSelectedDirectoriesSync(): Set<String>
}

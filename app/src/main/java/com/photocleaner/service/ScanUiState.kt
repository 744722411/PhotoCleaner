package com.photocleaner.service

import com.photocleaner.domain.model.DirectoryInfo

/**
 * Scan progress state owned by [ScanStateHolder]. Lives in the service layer so
 * the service does not depend on the UI layer.
 */
data class ScanUiState(
    val isScanning: Boolean = false,
    val isPaused: Boolean = false,
    val scannedCount: Int = 0,
    val totalToScan: Int = 0,
    val processedCount: Int = 0,
    val totalToProcess: Int = 0,
    val uselessFound: Int = 0,
    val scanComplete: Boolean = false,
    val error: String? = null,
    val scanLogs: List<ScanLogEntry> = emptyList(),
    val selectedDirectories: Set<String> = emptySet(),
    val showDirectoryPicker: Boolean = false,
    val discoveredDirectories: List<DirectoryInfo> = emptyList(),
    val isDiscoveringDirs: Boolean = false,
    val batchSize: Int = 2000,
    val rescanExistingPhotos: Boolean = false
)

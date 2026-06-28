package com.photocleaner.service

import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.domain.model.Photo

/**
 * Scan progress state owned by [ScanStateHolder]. Lives in the service layer so
 * the service does not depend on the UI layer.
 */
data class ScanUiState(
    val isScanning: Boolean = false,
    val isPaused: Boolean = false,
    val isProcessing: Boolean = false,
    val isProcessingPaused: Boolean = false,
    val scannedCount: Int = 0,
    val totalToScan: Int = 0,
    val processedCount: Int = 0,
    val totalToProcess: Int = 0,
    val uselessFound: Int = 0,
    val scanComplete: Boolean = false,
    val processingComplete: Boolean = false,
    val error: String? = null,
    val photos: List<Photo> = emptyList(),
    val scanLogs: List<ScanLogEntry> = emptyList(),
    val selectedDirectories: Set<String> = emptySet(),
    val showDirectoryPicker: Boolean = false,
    val discoveredDirectories: List<DirectoryInfo> = emptyList(),
    val isDiscoveringDirs: Boolean = false,
    val batchSize: Int = 100
)

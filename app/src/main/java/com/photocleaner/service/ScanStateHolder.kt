package com.photocleaner.service

import com.photocleaner.domain.model.Photo
import com.photocleaner.ui.scan.ScanLogEntry
import com.photocleaner.ui.scan.ScanUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that holds scan/classify state.
 * Both ScanService and ScanViewModel access this, so progress survives Activity destruction.
 */
@Singleton
class ScanStateHolder @Inject constructor() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun updateState(transform: (ScanUiState) -> ScanUiState) {
        _uiState.update(transform)
    }

    fun addLog(entry: ScanLogEntry) {
        _uiState.update { state ->
            state.copy(scanLogs = state.scanLogs + entry)
        }
    }

    fun reset() {
        _uiState.value = ScanUiState()
    }
}

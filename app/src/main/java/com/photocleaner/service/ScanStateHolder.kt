package com.photocleaner.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that holds scan state so progress survives Activity destruction.
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
            val newLogs = if (state.scanLogs.size >= MAX_LOGS) {
                // Drop oldest without reallocating the whole list every append.
                state.scanLogs.drop(state.scanLogs.size - MAX_LOGS + 1) + entry
            } else {
                state.scanLogs + entry
            }
            state.copy(scanLogs = newLogs)
        }
    }

    fun reset() {
        _uiState.value = ScanUiState()
    }

    private companion object {
        const val MAX_LOGS = 1000
    }
}

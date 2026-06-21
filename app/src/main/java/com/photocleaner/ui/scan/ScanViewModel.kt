package com.photocleaner.ui.scan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.data.repository.SettingsRepository
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.usecase.ScanPhotosUseCase
import com.photocleaner.domain.usecase.ClassifyPhotosUseCase
import com.photocleaner.service.ScanService
import com.photocleaner.service.ScanStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScanUiState(
    val isScanning: Boolean = false,
    val isPaused: Boolean = false,
    val isClassifying: Boolean = false,
    val isClassifyPaused: Boolean = false,
    val scannedCount: Int = 0,
    val totalToScan: Int = 0,
    val classifiedCount: Int = 0,
    val totalToClassify: Int = 0,
    val uselessFound: Int = 0,
    val scanComplete: Boolean = false,
    val classifyComplete: Boolean = false,
    val error: String? = null,
    val photos: List<Photo> = emptyList(),
    val scanLogs: List<ScanLogEntry> = emptyList(),
    val selectedDirectories: Set<String> = emptySet(),
    val showDirectoryPicker: Boolean = false,
    val discoveredDirectories: List<DirectoryInfo> = emptyList(),
    val isDiscoveringDirs: Boolean = false,
    val batchSize: Int = 100
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val scanPhotosUseCase: ScanPhotosUseCase,
    private val classifyPhotosUseCase: ClassifyPhotosUseCase,
    private val settingsRepository: SettingsRepository,
    private val photoRepository: PhotoRepository,
    private val scanStateHolder: ScanStateHolder
) : ViewModel() {

    // Local UI state (directory picker, etc.) merged with service state
    private val _localState = MutableStateFlow(
        ScanUiState(selectedDirectories = emptySet(), showDirectoryPicker = false)
    )

    // Expose merged state: service scan progress + local UI state
    val uiState: StateFlow<ScanUiState> = combine(
        scanStateHolder.uiState,
        _localState,
        settingsRepository.selectedDirectories,
        settingsRepository.batchSize
    ) { serviceState, localState, selectedDirs, batchSize ->
        serviceState.copy(
            selectedDirectories = if (localState.selectedDirectories.isNotEmpty()) localState.selectedDirectories else selectedDirs,
            showDirectoryPicker = localState.showDirectoryPicker,
            discoveredDirectories = localState.discoveredDirectories,
            isDiscoveringDirs = localState.isDiscoveringDirs,
            batchSize = batchSize
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScanUiState()
    )

    fun showDirectoryPicker() {
        _localState.update { it.copy(showDirectoryPicker = true) }
        if (_localState.value.discoveredDirectories.isEmpty() && !_localState.value.isDiscoveringDirs) {
            discoverDirectories()
        }
    }

    fun hideDirectoryPicker() {
        _localState.update { it.copy(showDirectoryPicker = false) }
    }

    fun discoverDirectories() {
        viewModelScope.launch {
            _localState.update { it.copy(isDiscoveringDirs = true) }
            try {
                val dirs = photoRepository.discoverDirectories()
                _localState.update { state ->
                    val currentDirs = scanStateHolder.uiState.value.selectedDirectories.ifEmpty {
                        settingsRepository.getSelectedDirectoriesSync()
                    }
                    // If no dirs were previously selected (first run), select all discovered dirs
                    val selected = if (currentDirs.isEmpty() && dirs.isNotEmpty()) {
                        dirs.map { it.relativePath }.toSet()
                    } else if (state.selectedDirectories.isNotEmpty()) {
                        state.selectedDirectories
                    } else {
                        currentDirs
                    }
                    state.copy(
                        discoveredDirectories = dirs,
                        isDiscoveringDirs = false,
                        selectedDirectories = selected
                    )
                }
            } catch (e: Exception) {
                _localState.update { it.copy(isDiscoveringDirs = false) }
                scanStateHolder.updateState { it.copy(error = "目录扫描失败: ${e.message}") }
            }
        }
    }

    fun toggleDirectory(dir: String) {
        _localState.update { state ->
            val current = state.selectedDirectories.toMutableSet()
            if (current.contains(dir)) {
                current.remove(dir)
            } else {
                current.add(dir)
            }
            state.copy(selectedDirectories = current)
        }
    }

    fun selectAllDirectories() {
        val allDirs = _localState.value.discoveredDirectories.map { it.relativePath }.toSet()
        _localState.update { it.copy(selectedDirectories = allDirs) }
    }

    fun deselectAllDirectories() {
        _localState.update { it.copy(selectedDirectories = emptySet()) }
    }

    fun setBatchSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setBatchSize(size)
        }
    }

    fun saveDirectories() {
        viewModelScope.launch {
            settingsRepository.setSelectedDirectories(_localState.value.selectedDirectories)
            scanStateHolder.updateState { it.copy(selectedDirectories = _localState.value.selectedDirectories) }
        }
    }

    fun startScan() {
        val currentState = scanStateHolder.uiState.value
        if (currentState.isScanning || currentState.isClassifying) return

        // Save directories first
        viewModelScope.launch {
            settingsRepository.setSelectedDirectories(_localState.value.selectedDirectories)
        }

        // Start the foreground service
        ScanService.startScan(appContext)
    }

    fun pauseScan() {
        scanStateHolder.updateState { it.copy(isPaused = true) }
        scanStateHolder.addLog(ScanLogEntry(message = "⏸️ 扫描已暂停", status = LogStatus.INFO))
    }

    fun resumeScan() {
        scanStateHolder.updateState { it.copy(isPaused = false) }
        scanStateHolder.addLog(ScanLogEntry(message = "▶️ 扫描已继续", status = LogStatus.INFO))
    }

    fun stopScan() {
        // Tell the service to stop
        ScanService.stop(appContext)
    }

    fun pauseClassify() {
        scanStateHolder.updateState { it.copy(isClassifyPaused = true) }
        scanStateHolder.addLog(ScanLogEntry(message = "⏸️ AI分类已暂停", status = LogStatus.INFO))
    }

    fun resumeClassify() {
        scanStateHolder.updateState { it.copy(isClassifyPaused = false) }
        scanStateHolder.addLog(ScanLogEntry(message = "▶️ AI分类已继续", status = LogStatus.INFO))
    }

    fun reset() {
        ScanService.stop(appContext)
        scanStateHolder.reset()
        _localState.update { ScanUiState() }
    }
}

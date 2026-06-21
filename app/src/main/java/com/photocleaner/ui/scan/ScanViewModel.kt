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
import com.photocleaner.domain.usecase.ScanLogStatus
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

    private var scanJob: Job? = null

    fun startScan() {
        val currentState = scanStateHolder.uiState.value
        if (currentState.isScanning || currentState.isClassifying) return

        viewModelScope.launch {
            settingsRepository.setSelectedDirectories(_localState.value.selectedDirectories)
        }

        scanStateHolder.updateState {
            it.copy(
                isScanning = true,
                isPaused = false,
                error = null,
                scanComplete = false,
                classifyComplete = false,
                scanLogs = emptyList()
            )
        }
        scanStateHolder.addLog(ScanLogEntry(message = "🔍 开始扫描照片...", status = LogStatus.INFO))

        scanJob = viewModelScope.launch {
            try {
                val batchSize = settingsRepository.getBatchSizeSync()
                val selectedDirectories = settingsRepository.getSelectedDirectoriesSync()

                if (batchSize > 0) {
                    scanStateHolder.addLog(ScanLogEntry(message = "📦 每次处理数量: $batchSize", status = LogStatus.INFO))
                }

                val isPaused = { scanStateHolder.uiState.value.isPaused }

                val photos = scanPhotosUseCase(
                    selectedDirectories = selectedDirectories,
                    batchSize = batchSize,
                    isPaused = isPaused,
                    onProgress = { scanned, total ->
                        scanStateHolder.updateState { state ->
                            state.copy(scannedCount = scanned, totalToScan = total)
                        }
                    },
                    onLog = { log ->
                        scanStateHolder.addLog(ScanLogEntry(
                            photoName = log.photoName,
                            status = when (log.status) {
                                com.photocleaner.domain.usecase.ScanLogStatus.PROCESSING -> LogStatus.PROCESSING
                                com.photocleaner.domain.usecase.ScanLogStatus.LOCAL_HIT -> LogStatus.LOCAL_HIT
                                com.photocleaner.domain.usecase.ScanLogStatus.SUCCESS -> LogStatus.SUCCESS
                                com.photocleaner.domain.usecase.ScanLogStatus.ERROR -> LogStatus.ERROR
                                com.photocleaner.domain.usecase.ScanLogStatus.INFO -> LogStatus.INFO
                            },
                            message = log.message
                        ))
                    }
                )

                val uselessCount = photos.count { it.classification == Classification.USELESS }
                scanStateHolder.updateState {
                    it.copy(
                        isScanning = false,
                        scanComplete = true,
                        classifyComplete = true, // Directly complete since cloud AI is removed
                        photos = photos,
                        uselessFound = uselessCount
                    )
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    scanStateHolder.updateState {
                        it.copy(isScanning = false, error = e.message ?: "扫描失败")
                    }
                    scanStateHolder.addLog(ScanLogEntry(message = "❌ 扫描失败: ${e.message}", status = LogStatus.ERROR))
                }
            }
        }
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
        scanJob?.cancel()
        scanStateHolder.updateState {
            it.copy(
                isScanning = false,
                isPaused = false,
                isClassifying = false,
                isClassifyPaused = false
            )
        }
        scanStateHolder.addLog(ScanLogEntry(message = "⏹️ 已停止", status = LogStatus.INFO))
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
        scanJob?.cancel()
        scanStateHolder.reset()
        _localState.update { ScanUiState() }
    }
}

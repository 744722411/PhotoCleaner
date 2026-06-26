package com.photocleaner.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.data.repository.SettingsRepository
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.DirectoryInfo
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.usecase.ScanLogStatus
import com.photocleaner.domain.usecase.ScanPhotosUseCase
import com.photocleaner.service.ScanStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val scanPhotosUseCase: ScanPhotosUseCase,
    private val settingsRepository: SettingsRepository,
    private val photoRepository: PhotoRepository,
    private val scanStateHolder: ScanStateHolder
) : ViewModel() {

    private val localState = MutableStateFlow(
        ScanUiState(selectedDirectories = emptySet(), showDirectoryPicker = false)
    )
    private var hasDirectorySelectionDraft = false

    val uiState: StateFlow<ScanUiState> = combine(
        scanStateHolder.uiState,
        localState,
        settingsRepository.selectedDirectories,
        settingsRepository.batchSize
    ) { serviceState, localUiState, persistedSelectedDirs, batchSize ->
        serviceState.copy(
            selectedDirectories = if (
                hasDirectorySelectionDraft ||
                localUiState.discoveredDirectories.isNotEmpty() ||
                localUiState.isDiscoveringDirs
            ) {
                localUiState.selectedDirectories
            } else {
                persistedSelectedDirs
            },
            showDirectoryPicker = localUiState.showDirectoryPicker,
            discoveredDirectories = localUiState.discoveredDirectories,
            isDiscoveringDirs = localUiState.isDiscoveringDirs,
            batchSize = batchSize
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScanUiState()
    )

    private var scanJob: Job? = null

    fun showDirectoryPicker() {
        localState.update { it.copy(showDirectoryPicker = true) }
        if (localState.value.discoveredDirectories.isEmpty() && !localState.value.isDiscoveringDirs) {
            discoverDirectories()
        }
    }

    fun hideDirectoryPicker() {
        localState.update { it.copy(showDirectoryPicker = false) }
    }

    fun discoverDirectories() {
        viewModelScope.launch {
            localState.update { it.copy(isDiscoveringDirs = true) }
            try {
                val dirs = photoRepository.discoverDirectories()
                localState.update { state ->
                    val persisted = settingsRepository.getSelectedDirectoriesSync()
                    val selected = when {
                        hasDirectorySelectionDraft -> state.selectedDirectories
                        persisted.isNotEmpty() -> persisted
                        dirs.isNotEmpty() -> dirs.map { it.relativePath }.toSet()
                        else -> emptySet()
                    }
                    state.copy(
                        discoveredDirectories = dirs,
                        isDiscoveringDirs = false,
                        selectedDirectories = selected
                    )
                }
            } catch (e: Exception) {
                localState.update { it.copy(isDiscoveringDirs = false) }
                scanStateHolder.updateState {
                    it.copy(error = "目录扫描失败: ${e.message}")
                }
            }
        }
    }

    fun toggleDirectory(dir: String) {
        hasDirectorySelectionDraft = true
        localState.update { state ->
            val current = state.selectedDirectories.toMutableSet()
            if (!current.add(dir)) {
                current.remove(dir)
            }
            state.copy(selectedDirectories = current)
        }
    }

    fun selectAllDirectories() {
        hasDirectorySelectionDraft = true
        val allDirs = localState.value.discoveredDirectories.map { it.relativePath }.toSet()
        localState.update { it.copy(selectedDirectories = allDirs) }
    }

    fun deselectAllDirectories() {
        hasDirectorySelectionDraft = true
        localState.update { it.copy(selectedDirectories = emptySet()) }
    }

    fun setBatchSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setBatchSize(size)
        }
    }

    suspend fun persistSelectedDirectories() {
        val selected = getEffectiveSelectedDirectories()
        settingsRepository.setSelectedDirectories(selected)
        scanStateHolder.updateState { it.copy(selectedDirectories = selected) }
    }

    private suspend fun getEffectiveSelectedDirectories(): Set<String> {
        val local = localState.value
        return if (
            hasDirectorySelectionDraft ||
            local.discoveredDirectories.isNotEmpty() ||
            local.isDiscoveringDirs
        ) {
            local.selectedDirectories
        } else {
            settingsRepository.getSelectedDirectoriesSync()
        }
    }

    fun saveDirectories() {
        viewModelScope.launch {
            persistSelectedDirectories()
        }
    }

    fun startScan() {
        val currentState = scanStateHolder.uiState.value
        if (currentState.isScanning || currentState.isClassifying) {
            return
        }

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                persistSelectedDirectories()

                scanStateHolder.updateState {
                    it.copy(
                        isScanning = true,
                        isPaused = false,
                        isClassifying = false,
                        isClassifyPaused = false,
                        error = null,
                        scanComplete = false,
                        classifyComplete = false,
                        scanLogs = emptyList(),
                        scannedCount = 0,
                        totalToScan = 0,
                        classifiedCount = 0,
                        totalToClassify = 0,
                        photos = emptyList(),
                        uselessFound = 0
                    )
                }
                scanStateHolder.addLog(
                    ScanLogEntry(message = "开始扫描照片...", status = LogStatus.INFO)
                )

                val batchSize = settingsRepository.getBatchSizeSync()
                val selectedDirectories = settingsRepository.getSelectedDirectoriesSync()

                if (batchSize > 0) {
                    scanStateHolder.addLog(
                        ScanLogEntry(
                            message = "每次处理数量: $batchSize",
                            status = LogStatus.INFO
                        )
                    )
                }

                val scannedPhotos = scanPhotosUseCase(
                    selectedDirectories = selectedDirectories,
                    batchSize = batchSize,
                    isPaused = { scanStateHolder.uiState.value.isPaused },
                    onProgress = { scanned, total ->
                        scanStateHolder.updateState {
                            it.copy(scannedCount = scanned, totalToScan = total)
                        }
                    },
                    onLog = { log ->
                        scanStateHolder.addLog(
                            ScanLogEntry(
                                photoName = log.photoName,
                                status = when (log.status) {
                                    ScanLogStatus.PROCESSING -> LogStatus.PROCESSING
                                    ScanLogStatus.LOCAL_HIT -> LogStatus.LOCAL_HIT
                                    ScanLogStatus.SUCCESS -> LogStatus.SUCCESS
                                    ScanLogStatus.ERROR -> LogStatus.ERROR
                                    ScanLogStatus.INFO -> LogStatus.INFO
                                },
                                message = log.message
                            )
                        )
                    }
                )

                val allPhotos = photoRepository.getAllPhotos().first()
                val uselessCount = allPhotos.count {
                    it.classification == Classification.USELESS && !it.isInTrash
                }

                scanStateHolder.updateState {
                    it.copy(
                        isScanning = false,
                        isClassifying = false,
                        isClassifyPaused = false,
                        scanComplete = true,
                        classifyComplete = true,
                        photos = allPhotos,
                        uselessFound = uselessCount,
                        classifiedCount = scannedPhotos.size,
                        totalToClassify = scannedPhotos.size
                    )
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    scanStateHolder.updateState {
                        it.copy(
                            isScanning = false,
                            isClassifying = false,
                            error = e.message ?: "扫描失败"
                        )
                    }
                    scanStateHolder.addLog(
                        ScanLogEntry(
                            message = "扫描失败: ${e.message}",
                            status = LogStatus.ERROR
                        )
                    )
                }
            }
        }
    }

    fun pauseScan() {
        scanStateHolder.updateState { it.copy(isPaused = true) }
        scanStateHolder.addLog(ScanLogEntry(message = "扫描已暂停", status = LogStatus.INFO))
    }

    fun resumeScan() {
        scanStateHolder.updateState { it.copy(isPaused = false) }
        scanStateHolder.addLog(ScanLogEntry(message = "扫描已继续", status = LogStatus.INFO))
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
        scanStateHolder.addLog(ScanLogEntry(message = "已停止", status = LogStatus.INFO))
    }

    fun reset() {
        scanJob?.cancel()
        scanStateHolder.reset()
        hasDirectorySelectionDraft = false
        localState.update { ScanUiState() }
    }
}

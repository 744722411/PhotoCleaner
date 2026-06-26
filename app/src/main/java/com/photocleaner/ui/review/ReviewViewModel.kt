package com.photocleaner.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.usecase.DeletePhotosUseCase
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

enum class FilterType(val displayName: String) {
    ALL("全部"),
    SIMILAR("相似"),
    LARGE("超大媒体"),
    USELESS("建议清理"),
    UNCERTAIN("人工审查"),
    KEEP("保留")
}

sealed class ReviewEvent {
    data class LaunchTrashIntent(val pendingIntent: android.app.PendingIntent) : ReviewEvent()
}

data class ReviewUiState(
    val photos: List<Photo> = emptyList(),
    val similarGroups: List<List<Photo>> = emptyList(),
    val selectedPhotos: Set<Long> = emptySet(),
    val filter: FilterType = FilterType.ALL,
    val isBatchMode: Boolean = false,
    val isLoading: Boolean = false,
    val deletedCount: Int = 0,
    val showUndo: Boolean = false,
    val lastDeletedPhotos: List<Photo> = emptyList(),
    val detailPhoto: Photo? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: PhotoRepository,
    private val deletePhotosUseCase: DeletePhotosUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(FilterType.ALL)
    
    private val _event = MutableSharedFlow<ReviewEvent>()
    val event = _event.asSharedFlow()

    private val pendingDeletePhotosList = mutableListOf<Photo>()
    private var deleteJob: Job? = null

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                repository.getAllPhotos(),
                _filter
            ) { photos, filter ->
                val filtered = when (filter) {
                    FilterType.ALL -> photos
                    FilterType.SIMILAR -> photos
                    FilterType.LARGE -> photos.sortedByDescending { it.size }
                    FilterType.USELESS -> photos.filter { it.classification == Classification.USELESS }
                    FilterType.UNCERTAIN -> photos.filter { it.classification == Classification.UNCERTAIN }
                    FilterType.KEEP -> photos.filter { it.classification == Classification.KEEP }
                }
                
                val similarGroups = if (filter == FilterType.SIMILAR) {
                    groupSimilarPhotos(photos)
                } else {
                    emptyList()
                }

                Pair(filtered, similarGroups)
            }.collect { (filtered, similarGroups) ->
                _uiState.update { state ->
                    state.copy(
                        photos = filtered,
                        similarGroups = similarGroups,
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun groupSimilarPhotos(photos: List<Photo>): List<List<Photo>> = kotlinx.coroutines.withContext(Dispatchers.Default) {
        val activePhotos = photos.filter { !it.isInTrash && it.dHash != 0L }
        if (activePhotos.isEmpty()) return@withContext emptyList()

        val groups = mutableListOf<MutableList<Photo>>()
        val visited = mutableSetOf<Long>()
        val sorted = activePhotos.sortedByDescending { it.dateAdded }

        for (i in sorted.indices) {
            val p1 = sorted[i]
            if (p1.id in visited) continue

            val currentGroup = mutableListOf<Photo>()
            currentGroup.add(p1)
            visited.add(p1.id)

            for (j in i + 1 until sorted.size) {
                val p2 = sorted[j]
                if (p2.id in visited) continue

                val timeDiff = abs(p1.dateAdded - p2.dateAdded)
                val distance = ImageUtils.hammingDistance(p1.dHash, p2.dHash)

                // 10 minutes (600s) and Hamming distance <= 5
                if (timeDiff <= 600 && distance <= 5) {
                    currentGroup.add(p2)
                    visited.add(p2.id)
                }
            }

            if (currentGroup.size > 1) {
                groups.add(currentGroup)
            }
        }
        groups
    }

    fun setFilter(filter: FilterType) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter) }
    }

    fun toggleBatchMode() {
        _uiState.update { it.copy(isBatchMode = !it.isBatchMode, selectedPhotos = emptySet()) }
    }

    fun toggleSelection(photoId: Long) {
        _uiState.update { state ->
            val newSelected = if (photoId in state.selectedPhotos) {
                state.selectedPhotos - photoId
            } else {
                state.selectedPhotos + photoId
            }
            state.copy(selectedPhotos = newSelected)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedPhotos = state.photos.map { it.id }.toSet())
        }
    }

    fun deselectAll() {
        _uiState.update { it.copy(selectedPhotos = emptySet()) }
    }

    fun deleteSelected() {
        val selected = _uiState.value.selectedPhotos
        if (selected.isEmpty()) return

        val photosToDelete = _uiState.value.photos.filter { it.id in selected }
        deletePhotos(photosToDelete)
        _uiState.update { it.copy(isBatchMode = false, selectedPhotos = emptySet()) }
    }

    fun deletePhoto(photo: Photo) {
        deletePhotos(listOf(photo))
    }

    fun keepBestInGroup(group: List<Photo>) {
        if (group.size <= 1) return
        val bestPhoto = group.minWithOrNull { p1, p2 ->
            val p1Blur = p1.localReason.contains("模糊")
            val p2Blur = p2.localReason.contains("模糊")
            if (p1Blur != p2Blur) {
                return@minWithOrNull if (p1Blur) 1 else -1
            }

            val p1Area = p1.width * p1.height
            val p2Area = p2.width * p2.height
            if (p1Area != p2Area) {
                return@minWithOrNull p2Area.compareTo(p1Area)
            }

            if (p1.size != p2.size) {
                return@minWithOrNull p2.size.compareTo(p1.size)
            }

            p1.dateAdded.compareTo(p2.dateAdded)
        } ?: group.first()

        val toDelete = group.filter { it.id != bestPhoto.id }
        deletePhotos(toDelete)
    }

    private fun deletePhotos(photos: List<Photo>) {
        if (photos.isEmpty()) return

        deleteJob?.cancel()
        val existingIds = pendingDeletePhotosList.mapTo(mutableSetOf()) { it.id }
        photos.forEach { photo ->
            if (existingIds.add(photo.id)) {
                pendingDeletePhotosList.add(photo)
            }
        }

        viewModelScope.launch {
            try {
                // Logically delete in Room (isInTrash = true)
                deletePhotosUseCase(photos)
            } catch (_: Exception) {}
        }

        _uiState.update { state ->
            state.copy(
                deletedCount = pendingDeletePhotosList.size,
                showUndo = true,
                lastDeletedPhotos = pendingDeletePhotosList.toList()
            )
        }

        deleteJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            _uiState.update { it.copy(showUndo = false) }
        }
    }

    fun undoDelete() {
        deleteJob?.cancel()
        val photos = _uiState.value.lastDeletedPhotos
        if (photos.isEmpty()) return

        viewModelScope.launch {
            try {
                // Restore logic state in Room (isInTrash = false)
                deletePhotosUseCase.restore(photos)
                pendingDeletePhotosList.clear()
                _uiState.update { state ->
                    state.copy(
                        showUndo = false,
                        lastDeletedPhotos = emptyList(),
                        deletedCount = maxOf(0, state.deletedCount - photos.size)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun commitPendingDeletes() {
        val photos = pendingDeletePhotosList.toList()
        if (photos.isEmpty()) return

        viewModelScope.launch {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val pendingIntent = repository.createTrashPendingIntent(photos)
                if (pendingIntent != null) {
                    _uiState.update { it.copy(showUndo = false) }
                    _event.emit(ReviewEvent.LaunchTrashIntent(pendingIntent))
                } else {
                    try {
                        deletePhotosUseCase.restore(photos)
                    } catch (_: Exception) {}
                    pendingDeletePhotosList.clear()
                    _uiState.update { state ->
                        state.copy(
                            showUndo = false,
                            lastDeletedPhotos = emptyList(),
                            deletedCount = maxOf(0, state.deletedCount - photos.size)
                        )
                    }
                }
            } else {
                pendingDeletePhotosList.clear()
                _uiState.update {
                    it.copy(
                        showUndo = false,
                        lastDeletedPhotos = emptyList(),
                        deletedCount = 0
                    )
                }
            }
        }
    }

    fun onTrashConfirmed() {
        pendingDeletePhotosList.clear()
        _uiState.update { it.copy(showUndo = false, lastDeletedPhotos = emptyList()) }
    }

    fun onTrashCanceled() {
        val photos = pendingDeletePhotosList.toList()
        pendingDeletePhotosList.clear()
        viewModelScope.launch {
            try {
                deletePhotosUseCase.restore(photos)
            } catch (_: Exception) {}
            _uiState.update { state ->
                state.copy(
                    showUndo = false,
                    lastDeletedPhotos = emptyList(),
                    deletedCount = maxOf(0, state.deletedCount - photos.size)
                )
            }
        }
    }

    fun showDetail(photo: Photo) {
        _uiState.update { it.copy(detailPhoto = photo) }
    }

    fun hideDetail() {
        _uiState.update { it.copy(detailPhoto = null) }
    }

    fun keepPhoto(photo: Photo) {
        viewModelScope.launch {
            repository.updateClassification(photo.id, Classification.KEEP, 1.0f, "user_kept")
        }
    }
}

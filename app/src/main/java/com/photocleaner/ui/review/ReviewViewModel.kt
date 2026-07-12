package com.photocleaner.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.R
import com.photocleaner.data.service.TrashService
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.usecase.DeletePhotosUseCase
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FilterType(val displayRes: Int) {
    ALL(R.string.filter_all),
    SIMILAR(R.string.filter_similar),
    LARGE(R.string.filter_large),
    USELESS(R.string.filter_useless),
    UNCERTAIN(R.string.filter_uncertain),
    KEEP(R.string.filter_keep)
}

sealed class ReviewEvent {
    data class LaunchTrashIntent(val pendingIntent: android.app.PendingIntent) : ReviewEvent()
}

data class ReviewUiState(
    val photos: List<Photo> = emptyList(),
    val similarGroups: List<List<Photo>> = emptyList(),
    val selectedPhotos: Set<Long> = emptySet(),
    val filter: FilterType = FilterType.USELESS,
    val isBatchMode: Boolean = false,
    val isLoading: Boolean = false,
    val isDeleteRequestInFlight: Boolean = false,
    val detailPhoto: Photo? = null,
    val error: String? = null
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: PhotoRepository,
    private val deletePhotosUseCase: DeletePhotosUseCase,
    private val trashService: TrashService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(FilterType.USELESS)
    private val _pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _event = MutableSharedFlow<ReviewEvent>()
    val event = _event

    private val pendingDeletePhotosList = mutableListOf<Photo>()

    init { loadPhotos() }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(_filter, repository.getAllPhotos(), _pendingDeleteIds) { filter, photos, pendingIds ->
                Triple(filter, photos.filterNot { it.id in pendingIds }, pendingIds)
            }.collectLatest { (filter, photos, _) ->
                val filtered = when (filter) {
                    FilterType.ALL -> photos
                    FilterType.SIMILAR -> photos
                    FilterType.LARGE -> photos
                        .filter { it.size >= LARGE_MEDIA_THRESHOLD_BYTES }
                        .sortedByDescending { it.size }
                    FilterType.USELESS -> photos.filter { it.classification == Classification.USELESS }
                    FilterType.UNCERTAIN -> photos.filter { it.classification == Classification.UNCERTAIN }
                    FilterType.KEEP -> photos.filter { it.classification == Classification.KEEP }
                }
                val similarGroups = if (filter == FilterType.SIMILAR) groupSimilarPhotos(photos) else emptyList()
                _uiState.update { state -> state.copy(photos = filtered, similarGroups = similarGroups, isLoading = false) }
            }
        }
    }

    private suspend fun groupSimilarPhotos(photos: List<Photo>) = kotlinx.coroutines.withContext(Dispatchers.Default) {
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
                if (p1.dateAdded - p2.dateAdded > 600) break
                if (p2.id in visited) continue
                if (ImageUtils.hammingDistance(p1.dHash, p2.dHash) <= 5) {
                    currentGroup.add(p2)
                    visited.add(p2.id)
                }
            }

            if (currentGroup.size > 1) groups.add(currentGroup)
        }
        groups
    }

    fun setFilter(filter: FilterType) { _filter.value = filter; _uiState.update { it.copy(filter = filter) } }
    fun toggleBatchMode() { _uiState.update { it.copy(isBatchMode = !it.isBatchMode, selectedPhotos = emptySet()) } }
    fun toggleSelection(photoId: Long) { _uiState.update { state -> state.copy(selectedPhotos = if (photoId in state.selectedPhotos) state.selectedPhotos - photoId else state.selectedPhotos + photoId) } }
    fun selectAll() { _uiState.update { state -> state.copy(selectedPhotos = state.photos.map { it.id }.toSet()) } }
    fun deselectAll() { _uiState.update { it.copy(selectedPhotos = emptySet()) } }

    fun deleteSelected() {
        val selected = _uiState.value.selectedPhotos
        if (selected.isEmpty()) return
        requestDelete(_uiState.value.photos.filter { it.id in selected })
        _uiState.update { it.copy(isBatchMode = false, selectedPhotos = emptySet()) }
    }

    fun deletePhoto(photo: Photo) = requestDelete(listOf(photo))

    fun deletePhotos(photos: List<Photo>) = requestDelete(photos)

    fun keepBestInGroup(group: List<Photo>) {
        if (group.size <= 1) return
        val bestPhoto = group.minWithOrNull { p1, p2 ->
            val p1Blur = p1.localReason.contains("模糊")
            val p2Blur = p2.localReason.contains("模糊")
            if (p1Blur != p2Blur) return@minWithOrNull if (p1Blur) 1 else -1
            val p1Area = p1.width * p1.height
            val p2Area = p2.width * p2.height
            if (p1Area != p2Area) return@minWithOrNull p2Area.compareTo(p1Area)
            if (p1.size != p2.size) return@minWithOrNull p2.size.compareTo(p1.size)
            p1.dateAdded.compareTo(p2.dateAdded)
        } ?: group.first()
        requestDelete(group.filter { it.id != bestPhoto.id })
    }

    private fun requestDelete(photos: List<Photo>) {
        if (photos.isEmpty() || _uiState.value.isDeleteRequestInFlight) return
        val existingIds = pendingDeletePhotosList.mapTo(mutableSetOf()) { it.id }
        photos.forEach { photo -> if (existingIds.add(photo.id)) pendingDeletePhotosList.add(photo) }
        _pendingDeleteIds.value = existingIds
        _uiState.update { it.copy(isDeleteRequestInFlight = true) }
        commitPendingDeletes()
    }

    fun commitPendingDeletes() {
        val photos = pendingDeletePhotosList.toList()
        if (photos.isEmpty()) return
        viewModelScope.launch {
            val pendingIntent = trashService.createDeletePendingIntent(photos)
            if (pendingIntent != null) {
                _event.emit(ReviewEvent.LaunchTrashIntent(pendingIntent))
            } else {
                _uiState.update { it.copy(error = "无法创建系统永久删除请求") }
                clearPendingDeletes()
            }
        }
    }

    fun onTrashConfirmed() {
        val photos = pendingDeletePhotosList.toList()
        if (photos.isEmpty()) return
        viewModelScope.launch {
            try {
                val deletedIds = repository.findDeletedPhotoIds(photos)
                val deletedPhotos = photos.filter { it.id in deletedIds }
                if (deletedPhotos.isNotEmpty()) deletePhotosUseCase(deletedPhotos)
                clearPendingDeletes()
                val failedCount = photos.size - deletedPhotos.size
                if (failedCount > 0) {
                    _uiState.update { it.copy(error = "$failedCount 张照片未能永久删除，请重试") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                clearPendingDeletes()
                _uiState.update { it.copy(error = e.message ?: "删除状态更新失败") }
            }
        }
    }

    fun onTrashCanceled() {
        clearPendingDeletes()
    }

    private fun clearPendingDeletes() {
        pendingDeletePhotosList.clear()
        _pendingDeleteIds.value = emptySet()
        _uiState.update { it.copy(isDeleteRequestInFlight = false) }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun selectPhotoForDetail(photo: Photo) { _uiState.update { it.copy(detailPhoto = photo) } }
    fun clearDetailPhoto() { _uiState.update { it.copy(detailPhoto = null) } }

    fun keepPhoto(photo: Photo) {
        viewModelScope.launch {
            try {
                repository.updateClassification(photo.id, Classification.KEEP, 1.0f, "user_kept")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "保留操作失败") }
            }
        }
    }

    private companion object {
        const val LARGE_MEDIA_THRESHOLD_BYTES = 20L * 1024L * 1024L
    }
}

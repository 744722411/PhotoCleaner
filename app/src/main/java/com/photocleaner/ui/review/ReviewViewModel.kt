package com.photocleaner.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.domain.usecase.DeletePhotosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FilterType(val displayName: String) {
    ALL("\u5168\u90e8"),
    USELESS("\u65e0\u7528"),
    UNCERTAIN("\u5f85\u5b9a"),
    KEEP("\u4fdd\u7559")
}

data class ReviewUiState(
    val photos: List<Photo> = emptyList(),
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

    init {
        loadPhotos()
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Combine photos and filter into a single reactive stream
            combine(
                repository.getAllPhotos(),
                _filter
            ) { photos, filter ->
                val filtered = when (filter) {
                    FilterType.ALL -> photos
                    FilterType.USELESS -> photos.filter { it.classification == Classification.USELESS }
                    FilterType.UNCERTAIN -> photos.filter { it.classification == Classification.UNCERTAIN }
                    FilterType.KEEP -> photos.filter { it.classification == Classification.KEEP }
                }
                filtered
            }.collect { filtered ->
                _uiState.update { it.copy(photos = filtered, isLoading = false) }
            }
        }
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

    private fun deletePhotos(photos: List<Photo>) {
        viewModelScope.launch {
            try {
                deletePhotosUseCase(photos)
                _uiState.update { state ->
                    state.copy(
                        deletedCount = state.deletedCount + photos.size,
                        showUndo = true,
                        lastDeletedPhotos = photos
                    )
                }
                // Auto-hide undo after 5 seconds
                kotlinx.coroutines.delay(5000)
                _uiState.update { it.copy(showUndo = false) }
            } catch (_: Exception) { }
        }
    }

    fun undoDelete() {
        val photos = _uiState.value.lastDeletedPhotos
        if (photos.isEmpty()) return

        viewModelScope.launch {
            try {
                deletePhotosUseCase.restore(photos)
                _uiState.update { it.copy(showUndo = false, lastDeletedPhotos = emptyList()) }
            } catch (_: Exception) { }
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

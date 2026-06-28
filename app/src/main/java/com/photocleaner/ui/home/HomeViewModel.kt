package com.photocleaner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val totalPhotos: Int = 0,
    val processedPhotos: Int = 0,
    val uselessPhotos: Int = 0,
    val spaceSaved: String = "0B",
    val processingCoverage: Int = 0,
    val recentPhotos: List<Photo> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PhotoRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.getTotalCount(),
        repository.getClassifiedCount(),
        repository.getUselessCount(),
        repository.getUselessSize(),
        repository.getAllPhotos()
    ) { total, classified, useless, size, photos ->
        HomeUiState(
            totalPhotos = total,
            processedPhotos = classified,
            uselessPhotos = useless,
            spaceSaved = ImageUtils.formatFileSize(size),
            processingCoverage = if (total > 0) {
                ((classified.toFloat() / total.toFloat()) * 100).toInt()
            } else 0,
            recentPhotos = photos.take(6)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}


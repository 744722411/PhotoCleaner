package com.photocleaner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalPhotos: Int = 0,
    val classifiedPhotos: Int = 0,
    val uselessPhotos: Int = 0,
    val spaceSaved: String = "0B",
    val classificationCoverage: Int = 0,
    val recentPhotos: List<Photo> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getTotalCount(),
                repository.getClassifiedCount(),
                repository.getUselessCount(),
                repository.getUselessSize()
            ) { total, classified, useless, size ->
                Quadruple(total, classified, useless, size)
            }.collect { (total, classified, useless, size) ->
                _uiState.update {
                    it.copy(
                        totalPhotos = total,
                        classifiedPhotos = classified,
                        uselessPhotos = useless,
                        spaceSaved = ImageUtils.formatFileSize(size),
                        classificationCoverage = if (total > 0) {
                            ((classified.toFloat() / total.toFloat()) * 100).toInt()
                        } else {
                            0
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getAllPhotos().collect { photos ->
                _uiState.update { it.copy(recentPhotos = photos.take(6)) }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

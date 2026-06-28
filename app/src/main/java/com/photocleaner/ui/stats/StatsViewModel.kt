package com.photocleaner.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CategoryStat(
    val classification: Classification,
    val count: Int,
    val totalSize: Long
)

data class StatsUiState(
    val totalPhotos: Int = 0,
    val processedPhotos: Int = 0,
    val uselessPhotos: Int = 0,
    val uselessSize: Long = 0L,
    val spaceSaved: String = "0B",
    val categoryStats: List<CategoryStat> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: PhotoRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.getTotalCount(),
        repository.getClassifiedCount(),
        repository.getUselessCount(),
        repository.getUselessSize(),
        repository.getAllPhotos()
    ) { total, classified, useless, size, photos ->
        StatsUiState(
            totalPhotos = total,
            processedPhotos = classified,
            uselessPhotos = useless,
            uselessSize = size,
            spaceSaved = ImageUtils.formatFileSize(size),
            categoryStats = buildCategoryStats(photos),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    private fun buildCategoryStats(photos: List<Photo>): List<CategoryStat> {
        val grouped = photos.groupBy { it.classification }
        return Classification.entries.map { classification ->
            val group = grouped[classification] ?: emptyList()
            CategoryStat(
                classification = classification,
                count = group.size,
                totalSize = group.sumOf { it.size }
            )
        }.filter { it.count > 0 }
    }
}


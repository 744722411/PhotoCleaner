package com.photocleaner.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.model.Classification
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryStat(
    val classification: Classification,
    val count: Int,
    val totalSize: Long
)

data class StatsUiState(
    val totalPhotos: Int = 0,
    val classifiedPhotos: Int = 0,
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

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(
                repository.getTotalCount(),
                repository.getClassifiedCount(),
                repository.getUselessCount(),
                repository.getUselessSize()
            ) { total, classified, useless, size ->
                // Return the values from combine, don't update state here
                total to Triple(classified, useless, size)
            }.collect { (total, triple) ->
                val (classified, useless, size) = triple
                _uiState.update {
                    it.copy(
                        totalPhotos = total,
                        classifiedPhotos = classified,
                        uselessPhotos = useless,
                        uselessSize = size,
                        spaceSaved = ImageUtils.formatFileSize(size)
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getAllPhotos().collect { photos ->
                val categoryStats = buildCategoryStats(photos)
                _uiState.update {
                    it.copy(categoryStats = categoryStats, isLoading = false)
                }
            }
        }
    }

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

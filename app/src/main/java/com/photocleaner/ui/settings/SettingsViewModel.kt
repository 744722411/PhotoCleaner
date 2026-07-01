package com.photocleaner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val batchSize: Int = 2000,
    val rescanExistingPhotos: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.batchSize,
        settingsRepository.rescanExistingPhotos
    ) { batchSize, rescanExistingPhotos ->
        SettingsUiState(
            batchSize = batchSize,
            rescanExistingPhotos = rescanExistingPhotos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setBatchSize(size: Int) {
        viewModelScope.launch { settingsRepository.setBatchSize(size.coerceAtLeast(0)) }
    }

    fun setRescanExistingPhotos(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRescanExistingPhotos(enabled) }
    }
}

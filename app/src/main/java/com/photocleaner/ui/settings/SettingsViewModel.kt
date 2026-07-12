package com.photocleaner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val rescanExistingPhotos: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.rescanExistingPhotos
        .map { rescanExistingPhotos ->
            SettingsUiState(rescanExistingPhotos = rescanExistingPhotos)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun setRescanExistingPhotos(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRescanExistingPhotos(enabled) }
    }
}

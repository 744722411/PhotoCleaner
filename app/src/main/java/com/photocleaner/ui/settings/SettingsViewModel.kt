package com.photocleaner.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.data.remote.OpenAIService
import com.photocleaner.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/",
    val model: String = "mimo-v2.5",
    val isTestingConnection: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val openAIService: OpenAIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.apiKey.collect { key ->
                _uiState.update { it.copy(apiKey = key) }
            }
        }
        viewModelScope.launch {
            settingsRepository.baseUrl.collect { url ->
                _uiState.update { it.copy(baseUrl = url) }
            }
        }
        viewModelScope.launch {
            settingsRepository.model.collect { model ->
                _uiState.update { it.copy(model = model) }
            }
        }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setApiKey(key)
        }
    }

    fun setBaseUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setBaseUrl(url)
        }
    }

    fun setModel(model: String) {
        viewModelScope.launch {
            settingsRepository.setModel(model)
        }
    }

    fun testConnection(baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testResult = null) }
            try {
                settingsRepository.setBaseUrl(baseUrl.trim())
                settingsRepository.setApiKey(apiKey.trim())
                settingsRepository.setModel(model.trim())
                openAIService.testConnection(baseUrl.trim(), apiKey.trim(), model.trim())
                _uiState.update {
                    it.copy(isTestingConnection = false, testResult = "连接成功", testSuccess = true)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isTestingConnection = false, testResult = "连接失败: ${e.message}", testSuccess = false)
                }
            }
        }
    }
}

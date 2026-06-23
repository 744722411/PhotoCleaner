package com.photocleaner.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.data.remote.OpenAIService
import com.photocleaner.data.repository.SettingsRepository
import com.photocleaner.domain.model.Photo
import com.photocleaner.domain.repository.PhotoRepository
import com.photocleaner.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val totalPhotos: Int = 0,
    val classifiedPhotos: Int = 0,
    val uselessPhotos: Int = 0,
    val spaceSaved: String = "0B",
    val recentPhotos: List<Photo> = emptyList(),
    val isScanning: Boolean = false,
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/",
    val model: String = "mimo-v2.5",
    val isTestingConnection: Boolean = false,
    val testResult: String? = null,
    val testSuccess: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PhotoRepository,
    private val settingsRepository: SettingsRepository,
    private val openAIService: OpenAIService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
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
                        spaceSaved = ImageUtils.formatFileSize(size)
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getAllPhotos().collect { photos ->
                _uiState.update { it.copy(recentPhotos = photos.take(10)) }
            }
        }

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
        _uiState.update { it.copy(apiKey = key) }
        viewModelScope.launch {
            settingsRepository.setApiKey(key)
        }
    }

    fun setBaseUrl(url: String) {
        _uiState.update { it.copy(baseUrl = url) }
        viewModelScope.launch {
            settingsRepository.setBaseUrl(url)
        }
    }

    fun setModel(model: String) {
        _uiState.update { it.copy(model = model) }
        viewModelScope.launch {
            settingsRepository.setModel(model)
        }
    }

    fun testConnection(baseUrl: String, apiKey: String, model: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testResult = null) }
            try {
                // Save the settings first so the baseUrl is used by the API
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

    fun clearTestResult() {
        _uiState.update { it.copy(testResult = null) }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

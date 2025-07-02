package com.vani.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SetupUiState(
    val apiKey: String = "",
    val selectedLanguage: String = "en-US",
    val silenceTimeout: String = "60", // Default timeout of 10 seconds
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SetupViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val prefs = context.getSharedPreferences("vani_prefs", Context.MODE_PRIVATE)

    init {
        // Load existing settings if available
        val loadedApiKey = prefs.getString("gemini_api_key", "") ?: ""
        val loadedLanguage = prefs.getString("transcription_language", "en-US") ?: "en-US"
        val loadedSilenceTimeout = prefs.getInt("silence_timeout_seconds", 60).toString()
        _uiState.value = SetupUiState(
            apiKey = loadedApiKey,
            selectedLanguage = loadedLanguage,
            silenceTimeout = loadedSilenceTimeout
        )
    }

    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(apiKey = apiKey, errorMessage = null)
    }

    fun updateLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun updateSilenceTimeout(timeout: String) {
        // Allow only numeric input
        if (timeout.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(silenceTimeout = timeout, errorMessage = null)
        }
    }

    fun saveSettings(): Boolean {
        val currentState = _uiState.value
        val timeoutValue = currentState.silenceTimeout.toIntOrNull()

        if (currentState.apiKey.isBlank()) {
            _uiState.value = currentState.copy(errorMessage = "API Key is required")
            return false
        }
        if (timeoutValue == null || timeoutValue <= 0) {
            _uiState.value = currentState.copy(errorMessage = "Silence timeout must be a positive number")
            return false
        }

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

        try {
            prefs.edit()
                .putString("gemini_api_key", currentState.apiKey)
                .putString("transcription_language", currentState.selectedLanguage)
                .putInt("silence_timeout_seconds", timeoutValue)
                .putBoolean("setup_completed", true)
                .apply()
            
            _uiState.value = _uiState.value.copy(isLoading = false)
            return true
        } catch (e: Exception) {
            _uiState.value = currentState.copy(
                isLoading = false,
                errorMessage = "Failed to save settings: ${e.message}"
            )
            return false
        }
    }

    fun isSetupCompleted(): Boolean {
        return prefs.getBoolean("setup_completed", false)
    }
}

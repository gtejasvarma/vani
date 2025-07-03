package com.vani.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vani.util.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME,
    API_KEY_SETUP,
    LANGUAGE_SELECTION,
    PERMISSIONS,
    SETUP_COMPLETE
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val apiKey: String = "",
    val selectedLanguage: String = "te-IN", // Telugu pre-selected
    val silenceTimeout: String = "60",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasPermission: Boolean = false,
    val showScreenPinningReminder: Boolean = false,
    val canGoNext: Boolean = false,
    val setupProgress: Float = 0f
)

class OnboardingViewModel(private val context: Context) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val prefs = context.getSharedPreferences("vani_prefs", Context.MODE_PRIVATE)

    init {
        // Load existing settings if available
        val loadedApiKey = prefs.getString("gemini_api_key", "") ?: ""
        val loadedLanguage = prefs.getString("transcription_language", "te-IN") ?: "te-IN"
        val loadedSilenceTimeout = prefs.getInt("silence_timeout_seconds", 60).toString()
        val hasPermission = PermissionUtils.hasRecordAudioPermission(context)
        
        _uiState.value = OnboardingUiState(
            apiKey = loadedApiKey,
            selectedLanguage = loadedLanguage,
            silenceTimeout = loadedSilenceTimeout,
            hasPermission = hasPermission,
            canGoNext = true, // Welcome screen can always proceed
            setupProgress = 0.2f
        )
    }

    fun nextStep() {
        val currentState = _uiState.value
        val nextStep = when (currentState.currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.API_KEY_SETUP
            OnboardingStep.API_KEY_SETUP -> OnboardingStep.LANGUAGE_SELECTION
            OnboardingStep.LANGUAGE_SELECTION -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> OnboardingStep.SETUP_COMPLETE
            OnboardingStep.SETUP_COMPLETE -> return // Final step
        }
        
        val progress = when (nextStep) {
            OnboardingStep.WELCOME -> 0.25f
            OnboardingStep.API_KEY_SETUP -> 0.5f
            OnboardingStep.LANGUAGE_SELECTION -> 0.75f
            OnboardingStep.PERMISSIONS -> 1.0f
            OnboardingStep.SETUP_COMPLETE -> 1.0f
        }
        
        _uiState.value = currentState.copy(
            currentStep = nextStep,
            setupProgress = progress,
            canGoNext = canProceedFromStep(nextStep),
            errorMessage = null
        )
    }

    fun previousStep() {
        val currentState = _uiState.value
        val previousStep = when (currentState.currentStep) {
            OnboardingStep.WELCOME -> return // First step
            OnboardingStep.API_KEY_SETUP -> OnboardingStep.WELCOME
            OnboardingStep.LANGUAGE_SELECTION -> OnboardingStep.API_KEY_SETUP
            OnboardingStep.PERMISSIONS -> OnboardingStep.LANGUAGE_SELECTION
            OnboardingStep.SETUP_COMPLETE -> OnboardingStep.PERMISSIONS
        }
        
        val progress = when (previousStep) {
            OnboardingStep.WELCOME -> 0.25f
            OnboardingStep.API_KEY_SETUP -> 0.5f
            OnboardingStep.LANGUAGE_SELECTION -> 0.75f
            OnboardingStep.PERMISSIONS -> 1.0f
            OnboardingStep.SETUP_COMPLETE -> 1.0f
        }
        
        _uiState.value = currentState.copy(
            currentStep = previousStep,
            setupProgress = progress,
            canGoNext = canProceedFromStep(previousStep),
            errorMessage = null
        )
    }

    fun updateApiKey(apiKey: String) {
        _uiState.value = _uiState.value.copy(
            apiKey = apiKey, 
            errorMessage = null,
            canGoNext = isValidApiKey(apiKey)
        )
    }

    fun updateLanguage(language: String) {
        _uiState.value = _uiState.value.copy(
            selectedLanguage = language,
            canGoNext = true
        )
    }

    fun updateSilenceTimeout(timeout: String) {
        if (timeout.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                silenceTimeout = timeout, 
                errorMessage = null
            )
        }
    }

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            hasPermission = true,
            canGoNext = true,
            errorMessage = null
        )
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            hasPermission = false,
            canGoNext = false,
            errorMessage = "Microphone permission is required for voice transcription"
        )
    }

    fun completeSetupAndFinish() {
        val currentState = _uiState.value
        val timeoutValue = currentState.silenceTimeout.toIntOrNull() ?: 60

        if (!isValidApiKey(currentState.apiKey)) {
            _uiState.value = currentState.copy(errorMessage = "Valid API Key is required")
            return
        }

        if (!currentState.hasPermission) {
            _uiState.value = currentState.copy(errorMessage = "Microphone permission is required")
            return
        }

        // Save settings immediately without loading state or delay
        try {
            prefs.edit()
                .putString("gemini_api_key", currentState.apiKey)
                .putString("transcription_language", currentState.selectedLanguage)
                .putInt("silence_timeout_seconds", timeoutValue)
                .putBoolean("setup_completed", true)
                .apply()
            
        } catch (e: Exception) {
            _uiState.value = currentState.copy(
                errorMessage = "Failed to save settings: ${e.message}"
            )
        }
    }

    fun isSetupCompleted(): Boolean {
        return prefs.getBoolean("setup_completed", false)
    }

    private fun canProceedFromStep(step: OnboardingStep): Boolean {
        val currentState = _uiState.value
        return when (step) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.API_KEY_SETUP -> isValidApiKey(currentState.apiKey)
            OnboardingStep.LANGUAGE_SELECTION -> true
            OnboardingStep.PERMISSIONS -> currentState.hasPermission
            OnboardingStep.SETUP_COMPLETE -> true
        }
    }

    private fun isValidApiKey(apiKey: String): Boolean {
        // Basic validation - check if it looks like a valid API key format
        return apiKey.isNotBlank() && 
               apiKey.length >= 20 && 
               apiKey.matches(Regex("^[A-Za-z0-9_-]+$"))
    }
}
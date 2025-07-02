package com.vani.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vani.data.TranscriptionRepository
import com.vani.data.model.TranscriptLine
import com.vani.speech.SimpleSpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.vani.util.ConnectivityMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MicButtonState {
    IDLE,
    LISTENING
}

data class MainUiState(
    val transcriptLines: List<TranscriptLine> = emptyList(),
    val micButtonState: MicButtonState = MicButtonState.IDLE,
    val isListening: Boolean = false,
    val isConnected: Boolean = true
)

class MainViewModel(
    private val repository: TranscriptionRepository,
    private val context: Context
) : ViewModel() {
    
    private val _micButtonState = MutableStateFlow(MicButtonState.IDLE)
    private val _isListening = MutableStateFlow(false)
    
    private val connectivityMonitor = ConnectivityMonitor(context)
    
    private var speechRecognizer: SimpleSpeechRecognizer? = null
    private val prefs = context.getSharedPreferences("vani_prefs", Context.MODE_PRIVATE)
    
    val uiState: StateFlow<MainUiState> = combine(
        repository.transcriptLines,
        _micButtonState,
        _isListening,
        connectivityMonitor.isConnected
    ) { transcriptLines, micState, isListening, connected ->
        MainUiState(
            transcriptLines = transcriptLines,
            micButtonState = micState,
            isListening = isListening,
            isConnected = connected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )
    
    init {
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SimpleSpeechRecognizer(
            context = context,
            onPartialResult = { text ->
                // Partial results can be used to update the UI in real-time if needed
            },
            onFinalResult = { text ->
                viewModelScope.launch {
                    if (text.isNotBlank() && !text.contains("No speech detected")) {
                        repository.addTranscriptLine(TranscriptLine(text = text))
                    }
                    // If the mic is still supposed to be listening, restart it
                    if (_micButtonState.value == MicButtonState.LISTENING) {
                        startListening()
                    } else {
                        _isListening.value = false
                    }
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    // Silently stop listening on timeout, otherwise show the error
                    if (error.contains("No speech input")) {
                        _micButtonState.value = MicButtonState.IDLE
                    } else {
                        repository.addTranscriptLine(TranscriptLine(text = "❌ Error: $error"))
                        _micButtonState.value = MicButtonState.IDLE
                    }
                    _isListening.value = false
                }
            },
            onReady = {
                _isListening.value = true
            }
        )
    }
    
    fun onMicButtonTap() {
        when (_micButtonState.value) {
            MicButtonState.IDLE -> {
                _micButtonState.value = MicButtonState.LISTENING
                startListening()
            }
            MicButtonState.LISTENING -> {
                _micButtonState.value = MicButtonState.IDLE
                stopListening()
            }
        }
    }
    
    private fun startListening() {
        val language = prefs.getString("transcription_language", "en-US") ?: "en-US"
        val timeout = prefs.getInt("silence_timeout_seconds", 10) * 1000L
        speechRecognizer?.startListening(language, timeout)
    }
    
    private fun stopListening() {
        _isListening.value = false
        speechRecognizer?.stopListening()
    }
    
    fun clearConversation() {
        viewModelScope.launch {
            repository.clearTranscript()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.stopListening()
    }
}
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

enum class MicButtonState {
    IDLE,
    LISTENING
}

data class MainUiState(
    val transcriptLines: List<TranscriptLine> = emptyList(),
    val micButtonState: MicButtonState = MicButtonState.IDLE,
    val isListening: Boolean = false,
    val isConnected: Boolean = true,
    val volumeLevel: Float = 0f // RMS dB level for volume indicator
)

class MainViewModel(
    private val repository: TranscriptionRepository,
    private val context: Context
) : ViewModel() {
    
    private val _micButtonState = MutableStateFlow(MicButtonState.IDLE)
    private val _isListening = MutableStateFlow(false)
    private val _volumeLevel = MutableStateFlow(0f)
    
    private val connectivityMonitor = ConnectivityMonitor(context)
    
    private var speechRecognizer: SimpleSpeechRecognizer? = null
    private val prefs = context.getSharedPreferences("vani_prefs", Context.MODE_PRIVATE)
    
    private var conversationTimerJob: Job? = null
    private val conversationTimeoutMs = 5 * 60 * 1000L // 5 minutes
    
    val uiState: StateFlow<MainUiState> = combine(
        repository.transcriptLines,
        _micButtonState,
        _isListening,
        connectivityMonitor.isConnected,
        _volumeLevel
    ) { transcriptLines, micState, isListening, connected, volumeLevel ->
        MainUiState(
            transcriptLines = transcriptLines,
            micButtonState = micState,
            isListening = isListening,
            isConnected = connected,
            volumeLevel = volumeLevel
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
                    // In conversation mode, automatically restart listening
                    if (_micButtonState.value == MicButtonState.LISTENING) {
                        startConversationTimer() // Start 5-minute timer after speech
                        startListening() // Immediately restart recognizer
                    } else {
                        _isListening.value = false
                        _volumeLevel.value = 0f
                    }
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    // Handle different error types
                    when {
                        error.contains("No speech input") || error.contains("Speech timeout") -> {
                            // Timeout errors - restart if still in conversation mode
                            if (_micButtonState.value == MicButtonState.LISTENING) {
                                startConversationTimer()
                                startListening()
                            } else {
                                stopConversationMode()
                            }
                        }
                        else -> {
                            // Real errors - show to user and stop
                            repository.addTranscriptLine(TranscriptLine(text = "❌ Error: $error"))
                            stopConversationMode()
                        }
                    }
                }
            },
            onReady = {
                _isListening.value = true
            },
            onVolumeChanged = { rmsDb ->
                _volumeLevel.value = rmsDb
            },
            onSpeechStart = {
                // Reset timer when new speech begins
                resetConversationTimer()
            },
            onSpeechEnd = {
                // Start timer when speech ends
                startConversationTimer()
            }
        )
    }
    
    private fun startConversationTimer() {
        // Cancel existing timer
        conversationTimerJob?.cancel()
        
        // Start new 5-minute timer
        conversationTimerJob = viewModelScope.launch {
            delay(conversationTimeoutMs)
            // Timer completed - stop conversation mode
            if (_micButtonState.value == MicButtonState.LISTENING) {
                stopConversationMode()
            }
        }
    }
    
    private fun resetConversationTimer() {
        // Cancel current timer and start fresh
        conversationTimerJob?.cancel()
        // Don't restart timer immediately - let speech end trigger it
    }
    
    private fun stopConversationMode() {
        conversationTimerJob?.cancel()
        _micButtonState.value = MicButtonState.IDLE
        _isListening.value = false
        _volumeLevel.value = 0f
        speechRecognizer?.stopListening()
    }
    
    fun onMicButtonTap() {
        when (_micButtonState.value) {
            MicButtonState.IDLE -> {
                _micButtonState.value = MicButtonState.LISTENING
                startListening()
            }
            MicButtonState.LISTENING -> {
                stopConversationMode()
            }
        }
    }
    
    private fun startListening() {
        val language = prefs.getString("transcription_language", "en-US") ?: "en-US"
        // Use shorter timeout for individual speech segments (30 seconds)
        // The conversation timer handles the overall 5-minute timeout
        val segmentTimeout = 30 * 1000L 
        speechRecognizer?.startListening(language, segmentTimeout)
    }
    
    private fun stopListening() {
        conversationTimerJob?.cancel()
        _isListening.value = false
        speechRecognizer?.stopListening()
    }
    
    fun clearConversation() {
        viewModelScope.launch {
            // Stop conversation mode first
            stopConversationMode()
            // Clear the transcript
            repository.clearTranscript()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        conversationTimerJob?.cancel()
        speechRecognizer?.stopListening()
    }
}
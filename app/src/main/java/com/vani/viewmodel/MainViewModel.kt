package com.vani.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vani.data.TranscriptionRepository
import com.vani.data.model.TranscriptLine
import com.vani.service.SpeechRecognitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.vani.util.ConnectivityMonitor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.Intent
import android.os.IBinder

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
    
    private var speechService: SpeechRecognitionService? = null
    private val prefs = context.getSharedPreferences("vani_prefs", Context.MODE_PRIVATE)
    private var serviceBound = false
    
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
        // Delay service binding to avoid issues during ViewModel creation
        viewModelScope.launch {
            delay(100) // Small delay to ensure context is ready
            bindToSpeechService()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as SpeechRecognitionService.SpeechRecognitionBinder
            speechService = binder.getService()
            serviceBound = true
            setupServiceCallbacks()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            speechService = null
            serviceBound = false
        }
    }
    
    private fun bindToSpeechService() {
        try {
            val intent = Intent(context, SpeechRecognitionService::class.java)
            // Start the service first to ensure it exists
            val startResult = context.startService(intent)
            if (startResult != null) {
                // Only bind if service started successfully
                val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                if (!bound) {
                    serviceBound = false
                }
            } else {
                serviceBound = false
            }
        } catch (e: Exception) {
            // Handle any binding exceptions gracefully  
            serviceBound = false
        }
    }
    
    private fun setupServiceCallbacks() {
        speechService?.apply {
            onPartialResult = { text ->
                // Handle partial results for real-time UI updates
                viewModelScope.launch {
                    // Could add partial result handling here if needed
                }
            }
            
            onFinalResult = { text ->
                viewModelScope.launch {
                    if (text.isNotBlank() && !text.contains("No speech detected")) {
                        repository.addTranscriptLine(TranscriptLine(text = text))
                    }
                }
            }
            
            onError = { error ->
                viewModelScope.launch {
                    repository.addTranscriptLine(TranscriptLine(text = "❌ Error: $error"))
                    _micButtonState.value = MicButtonState.IDLE
                }
            }
            
            onListeningStateChanged = { listening ->
                _isListening.value = listening
            }
            
            onVolumeChanged = { volume ->
                _volumeLevel.value = volume
            }
        }
    }
    
    private fun stopConversationMode() {
        _micButtonState.value = MicButtonState.IDLE
        _isListening.value = false
        _volumeLevel.value = 0f
        speechService?.stopConversationMode()
    }
    
    fun onMicButtonTap() {
        if (!serviceBound || speechService == null) {
            // Service not ready, try to bind again
            bindToSpeechService()
            return
        }
        
        when (_micButtonState.value) {
            MicButtonState.IDLE -> {
                _micButtonState.value = MicButtonState.LISTENING
                val language = prefs.getString("transcription_language", "en-US") ?: "en-US"
                speechService?.startConversationMode(language)
            }
            MicButtonState.LISTENING -> {
                stopConversationMode()
            }
        }
    }
    
    
    fun clearConversation() {
        viewModelScope.launch {
            // Stop conversation mode first
            stopConversationMode()
            // Clear the transcript
            repository.clearTranscript()
        }
    }
    
    private fun ensureServiceBound() {
        if (!serviceBound || speechService == null) {
            bindToSpeechService()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        speechService?.stopConversationMode()
        if (serviceBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // Service may already be unbound
            }
            serviceBound = false
        }
        speechService = null
    }
}
package com.vani.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SimpleSpeechRecognizer(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onReady: () -> Unit,
    private val onVolumeChanged: (Float) -> Unit = {},
    private val onSpeechStart: () -> Unit = {},
    private val onSpeechEnd: () -> Unit = {}
) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    fun startListening(language: String, silenceMillis: Long) {
        if (isListening) {
            stopListening()
        }
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available")
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onReady()
            }
            
            override fun onBeginningOfSpeech() {
                onPartialResult("🗣️ Speech detected!")
                onSpeechStart()
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Convert RMS dB to a normalized 0-1 range for UI display
                // RMS values typically range from 0 to ~10dB, normalize to 0-1
                val normalizedVolume = (rmsdB / 10f).coerceIn(0f, 1f)
                onVolumeChanged(normalizedVolume)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Not used for this implementation
            }
            
            override fun onEndOfSpeech() {
                onPartialResult("🔚 Speech ended, processing...")
                onSpeechEnd()
            }
            
            override fun onError(error: Int) {
                isListening = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech input"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error: $error"
                }
                onError(errorMessage)
            }
            
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches.isNullOrEmpty()) {
                    onFinalResult("❌ No speech detected")
                } else {
                    onFinalResult(matches.first())
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Ignore partial results for simplicity
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used for this implementation
            }
        })
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMillis)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMillis)
        }
        
        speechRecognizer?.startListening(intent)
    }
    
    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
    
    fun isCurrentlyListening(): Boolean = isListening
}
package com.vani.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.vani.MainActivity
import com.vani.R
import com.vani.speech.SimpleSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground service for continuous speech recognition following Google Live Transcribe patterns.
 * Handles session management, audio buffering, and network resilience.
 */
class SpeechRecognitionService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "speech_recognition_channel"
        private const val SESSION_TIMEOUT_MS = 4 * 60 * 1000L // 4 minutes (before 5min API limit)
        private const val SEGMENT_TIMEOUT_MS = 15 * 1000L // 15 seconds for continuous speech
        private const val RESTART_DELAY_MS = 100L // Minimal delay to avoid Android issues
    }
    
    private val binder = SpeechRecognitionBinder()
    // Separate threads: background for audio processing, main for UI updates
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioHandler = Handler(Looper.getMainLooper()) // SpeechRecognizer requires main thread
    
    // Service state
    private val isListening = AtomicBoolean(false)
    private val isInConversationMode = AtomicBoolean(false)
    
    // Speech recognition components
    private var speechRecognizer: SimpleSpeechRecognizer? = null
    private var currentLanguage = "en-US"
    
    // Session management (following Live Transcribe pattern)
    private var sessionTimerJob: Job? = null
    private var conversationTimerJob: Job? = null
    private var audioBuffer = mutableListOf<String>() // Buffer partial results between sessions
    
    // Callbacks for UI communication
    var onPartialResult: ((String) -> Unit)? = null
    var onFinalResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onListeningStateChanged: ((Boolean) -> Unit)? = null
    var onVolumeChanged: ((Float) -> Unit)? = null
    
    inner class SpeechRecognitionBinder : Binder() {
        fun getService(): SpeechRecognitionService = this@SpeechRecognitionService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupSpeechRecognizer()
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return START_STICKY
    }
    
    private fun startForeground() {
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Speech Recognition",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous speech recognition service"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val status = if (isListening.get()) "Listening..." else "Ready"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vani Speech Recognition")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_mic_24) // You'll need to add this icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer = SimpleSpeechRecognizer(
            context = this,
            onPartialResult = { text ->
                // Process partial results on background thread
                serviceScope.launch(Dispatchers.Default) {
                    if (!text.contains("Speech detected") && !text.contains("Speech ended")) {
                        audioBuffer.add(text)
                        // Update UI on main thread
                        mainHandler.post {
                            onPartialResult?.invoke(text)
                        }
                    }
                }
            },
            onFinalResult = { text ->
                handleFinalResult(text)
            },
            onError = { error ->
                handleSpeechError(error)
            },
            onReady = {
                isListening.set(true)
                updateNotification()
                // Update UI on main thread
                mainHandler.post {
                    onListeningStateChanged?.invoke(true)
                }
            },
            onVolumeChanged = { volume ->
                // Update UI on main thread
                mainHandler.post {
                    onVolumeChanged?.invoke(volume)
                }
            },
            onSpeechStart = {
                // Reset session timer when new speech begins
                resetSessionTimer()
            },
            onSpeechEnd = {
                // Start session timer when speech ends
                startSessionTimer()
            }
        )
    }
    
    private fun handleFinalResult(text: String) {
        // Process audio result on background thread
        serviceScope.launch(Dispatchers.Default) {
            if (text.isNotBlank() && !text.contains("No speech detected")) {
                // Clear buffer since we got final result
                audioBuffer.clear()
                
                // Update UI on main thread
                mainHandler.post {
                    onFinalResult?.invoke(text)
                }
                
                // In conversation mode, we MUST restart after final results
                // Android SpeechRecognizer stops after onResults()
                if (isInConversationMode.get()) {
                    // Restart immediately - SpeechRecognizer is dead after final result
                    delay(RESTART_DELAY_MS) // Minimal delay
                    if (isInConversationMode.get()) {
                        startNewSession()
                    }
                } else {
                    stopListening()
                }
            }
        }
    }
    
    private fun handleSpeechError(error: String) {
        // Process errors on background thread
        serviceScope.launch(Dispatchers.Default) {
            when {
                error.contains("No speech input") || error.contains("Speech timeout") -> {
                    // Silence timeout - this is normal in continuous mode
                    if (isInConversationMode.get()) {
                        // Only restart if we've been running close to session limit
                        // Otherwise let SpeechRecognizer continue naturally
                        delay(500) // Brief pause
                        if (isInConversationMode.get()) {
                            startNewSession() // Restart to avoid Android timeout
                        }
                    } else {
                        stopListening()
                    }
                }
                else -> {
                    // Real errors - notify UI on main thread and stop
                    mainHandler.post {
                        onError?.invoke(error)
                    }
                    stopListening()
                }
            }
        }
    }
    
    // Following Live Transcribe session management pattern
    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = serviceScope.launch {
            delay(SESSION_TIMEOUT_MS)
            // Close session before API timeout and restart
            if (isInConversationMode.get()) {
                startNewSession()
            }
        }
    }
    
    private fun resetSessionTimer() {
        sessionTimerJob?.cancel()
    }
    
    private fun startNewSession() {
        // Following Live Transcribe pattern: gracefully restart session
        stopCurrentSession()
        serviceScope.launch(Dispatchers.Default) {
            delay(50) // Minimal pause to avoid Android race conditions
            if (isInConversationMode.get()) {
                startListeningInternal()
            }
        }
    }
    
    private fun stopCurrentSession() {
        isListening.set(false)
        resetSessionTimer()
        updateNotification()
        
        // SpeechRecognizer operations must be on main thread
        mainHandler.post {
            speechRecognizer?.stopListening()
            onListeningStateChanged?.invoke(false)
        }
    }
    
    // Public API for UI communication
    fun startConversationMode(language: String = "en-US") {
        currentLanguage = language
        isInConversationMode.set(true)
        startConversationTimer()
        startListeningInternal()
    }
    
    fun stopConversationMode() {
        isInConversationMode.set(false)
        conversationTimerJob?.cancel()
        stopListening()
    }
    
    fun startSingleRecognition(language: String = "en-US") {
        currentLanguage = language
        isInConversationMode.set(false)
        startListeningInternal()
    }
    
    private fun startListeningInternal() {
        // SpeechRecognizer must be called on main thread (Android requirement)
        audioHandler.post {
            speechRecognizer?.startListening(currentLanguage, SEGMENT_TIMEOUT_MS)
            startSessionTimer()
        }
    }
    
    private fun stopListening() {
        isInConversationMode.set(false)
        conversationTimerJob?.cancel()
        stopCurrentSession()
    }
    
    private fun startConversationTimer() {
        conversationTimerJob?.cancel()
        conversationTimerJob = serviceScope.launch {
            delay(5 * 60 * 1000L) // 5 minutes conversation timeout
            stopConversationMode()
        }
    }
    
    fun isCurrentlyListening(): Boolean = isListening.get()
    fun isInConversation(): Boolean = isInConversationMode.get()
    
    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        speechRecognizer = null
        serviceScope.cancel()
    }
}
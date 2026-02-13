package com.talkar.app.data.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Manages speech recognition with silence detection logic similar to the iOS implementation.
 */
class SpeechRecognitionManager(private val context: Context) {
    private val TAG = "SpeechRecognitionManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main)
    
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private var silenceJob: Job? = null
    private var noSpeechTimeoutJob: Job? = null // Added no-speech timeout job
    private var lastTranscript = ""
    private var onSilenceDetected: (() -> Unit)? = null
    
    // 🔥 State guard to prevent duplicate handles for the same silence event
    private var isHandlingResult = false
    
    // Silence threshold in milliseconds (matches iOS 2.0s)
    private val SILENCE_THRESHOLD_MS = 2000L
    // Timeout if no speech started at all (10s)
    private val NO_SPEECH_TIMEOUT_MS = 10000L

    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            // 🔥 Safety: Ensure any old instance is fully killed before replacing
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
            isHandlingResult = false // Reset gate on init
        } else {
            Log.e(TAG, "Speech recognition not available on this device")
        }
    }

    fun startListening(onSilence: () -> Unit) {
        if (speechRecognizer == null) {
            initialize()
        }
        
        if (speechRecognizer == null) {
            Log.e(TAG, "Cannot start listening: SpeechRecognizer initialization failed")
            return
        }
        
        onSilenceDetected = onSilence
        _transcript.value = ""
        lastTranscript = ""
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
            isHandlingResult = false // Reset gate on start
            startSilenceTimer()
            startNoSpeechTimeoutTimer() // Start the no-speech timeout timer
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _isListening.value = false
            onSilenceDetected = null
        }
    }

    fun stopListening() {
        try {
            silenceJob?.cancel()
            noSpeechTimeoutJob?.cancel() // Cancel no-speech timeout job
            // speechRecognizer?.cancel() // Removed redundant cancel, preferring stopListening to deliver results
            speechRecognizer?.stopListening()
            _isListening.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
        }
    }

    fun destroy() {
        try {
            // Sequence for maximum grace
            silenceJob?.cancel()
            silenceJob = null
            noSpeechTimeoutJob?.cancel() // Cancel no-speech timeout job
            noSpeechTimeoutJob = null
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
            
            onSilenceDetected = null // Clear callback to avoid leaks/stale triggers
            speechRecognizer?.destroy()
            speechRecognizer = null
            
            scope.cancel(null) // Cancel the coroutine scope
            Log.d(TAG, "SpeechRecognitionManager destroyed and cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying recognizer", e)
        }
    }

    private fun startSilenceTimer() {
        silenceJob?.cancel()
        silenceJob = scope.launch {
            delay(SILENCE_THRESHOLD_MS)
            // If we reach here, silence threshold passed
            
            Log.d(TAG, "Silence detected for ${SILENCE_THRESHOLD_MS}ms")
            
            if (isHandlingResult) return@launch
            isHandlingResult = true
            
            stopListening()
            
            // Single-invocation pattern
            val callback = onSilenceDetected
            onSilenceDetected = null
            callback?.invoke()
        }
    }

    private fun startNoSpeechTimeoutTimer() {
        noSpeechTimeoutJob?.cancel()
        noSpeechTimeoutJob = scope.launch {
            delay(NO_SPEECH_TIMEOUT_MS)
            
            Log.d(TAG, "No speech detected for ${NO_SPEECH_TIMEOUT_MS}ms (Timeout)")
            
            if (isHandlingResult) return@launch
            isHandlingResult = true
            
            stopListening()
            
            // Treat as silence/no-match to trigger prompt
            val callback = onSilenceDetected
            onSilenceDetected = null
            callback?.invoke()
        }
    }

    private val recognitionListener = object : RecognitionListener {
        // 🔥 Helper to ignore events from a destroyed recognizer
        private fun isZombie(): Boolean = speechRecognizer == null

        override fun onReadyForSpeech(params: Bundle?) {
            if (isZombie()) return
            Log.d(TAG, "onReadyForSpeech")
            isHandlingResult = false // Confirmed ready, clear any stale gate
        }

        override fun onBeginningOfSpeech() {
            if (isZombie()) return
            Log.d(TAG, "onBeginningOfSpeech")
            _isListening.value = true // Ensure listening state is true
            // Cancel no-speech timeout as user has started speaking
            noSpeechTimeoutJob?.cancel()
            noSpeechTimeoutJob = null
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            if (isZombie()) return
            Log.d(TAG, "onEndOfSpeech")
            startSilenceTimer()
        }

        private var lastErrorTime = 0L
        private var lastErrorCode = -1

        override fun onError(error: Int) {
            if (isZombie()) return // 🤫 SILENCE ZOMBIE ERRORS
            
            val currentTime = System.currentTimeMillis()
            if (error == lastErrorCode && (currentTime - lastErrorTime) < 1000) {
                Log.v(TAG, "Ignoring duplicate onError ($error) within debounce window")
                return
            }
            
            if (isHandlingResult) {
                Log.v(TAG, "Ignoring onError ($error): already handling result")
                return
            }
            
            lastErrorTime = currentTime
            lastErrorCode = error
            
            val errorMessage = getErrorText(error)
            Log.e(TAG, "onError: $errorMessage")
            
            silenceJob?.cancel()
            silenceJob = null
            noSpeechTimeoutJob?.cancel()
            noSpeechTimeoutJob = null
            _isListening.value = false
            
            // Terminal error: mark handled and notify
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                 isHandlingResult = true
                 val callback = onSilenceDetected
                 onSilenceDetected = null
                 callback?.invoke()
            }
        }

        override fun onResults(results: Bundle?) {
            if (isZombie() || isHandlingResult) return
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                isHandlingResult = true
                val text = matches[0]
                Log.d(TAG, "onResults: $text")
                _transcript.value = text
                
                silenceJob?.cancel()
                silenceJob = null
                noSpeechTimeoutJob?.cancel()
                noSpeechTimeoutJob = null
                stopListening()
                
                val callback = onSilenceDetected
                onSilenceDetected = null
                callback?.invoke()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (isZombie()) return
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                Log.d(TAG, "onPartialResults: $text")
                
                if (text != lastTranscript) {
                    _transcript.value = text
                    lastTranscript = text
                    startSilenceTimer()
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
}

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
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private var silenceJob: Job? = null
    private var lastTranscript = ""
    private var onSilenceDetected: (() -> Unit)? = null
    
    // Silence threshold in milliseconds (matches iOS 2.0s)
    private val SILENCE_THRESHOLD_MS = 2000L

    fun initialize() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            // 🔥 Safety: Ensure any old instance is fully killed before replacing
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }
        } else {
            Log.e(TAG, "Speech recognition not available on this device")
        }
    }

    fun startListening(onSilence: () -> Unit) {
        if (speechRecognizer == null) initialize()
        
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
            startSilenceTimer()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            silenceJob?.cancel()
            speechRecognizer?.cancel() // 🔥 Cancel pending requests 
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
            speechRecognizer?.cancel()
            speechRecognizer?.stopListening()
            
            onSilenceDetected = null // Clear callback to avoid leaks/stale triggers
            speechRecognizer?.destroy()
            speechRecognizer = null
            Log.d(TAG, "SpeechRecognizer destroyed and cleaned up")
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
            stopListening()
            onSilenceDetected?.invoke()
        }
    }

    private val recognitionListener = object : RecognitionListener {
        // 🔥 Helper to ignore events from a destroyed recognizer
        private fun isZombie(): Boolean = speechRecognizer == null

        override fun onReadyForSpeech(params: Bundle?) {
            if (isZombie()) return
            Log.d(TAG, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            if (isZombie()) return
            Log.d(TAG, "onBeginningOfSpeech")
            silenceJob?.cancel()
        }

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            if (isZombie()) return
            Log.d(TAG, "onEndOfSpeech")
            startSilenceTimer()
        }

        override fun onError(error: Int) {
            if (isZombie()) return // 🤫 SILENCE ZOMBIE ERRORS
            
            val errorMessage = getErrorText(error)
            Log.e(TAG, "onError: $errorMessage")
            _isListening.value = false
            
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                 onSilenceDetected?.invoke()
            }
        }

        override fun onResults(results: Bundle?) {
            if (isZombie()) return
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val text = matches[0]
                Log.d(TAG, "onResults: $text")
                _transcript.value = text
                
                stopListening()
                onSilenceDetected?.invoke()
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

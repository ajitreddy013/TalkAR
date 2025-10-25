package com.talkar.app.data.services

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Service to handle speech recognition using Android's built-in SpeechRecognizer
 */
class SpeechRecognitionService(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    // State flows for speech recognition results
    private val _speechResult = MutableStateFlow<SpeechResult>(SpeechResult.Idle)
    val speechResult: StateFlow<SpeechResult> = _speechResult
    
    // Initialize speech recognizer
    init {
        initializeSpeechRecognizer()
    }
    
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _speechResult.value = SpeechResult.Listening
                        Log.d(TAG, "Ready for speech")
                    }
                    
                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "Beginning of speech")
                    }
                    
                    override fun onRmsChanged(rmsdB: Float) {
                        // Can be used for visual feedback
                    }
                    
                    override fun onBufferReceived(buffer: ByteArray?) {
                        Log.d(TAG, "Buffer received")
                    }
                    
                    override fun onEndOfSpeech() {
                        _speechResult.value = SpeechResult.Processing
                        Log.d(TAG, "End of speech")
                    }
                    
                    override fun onError(error: Int) {
                        val errorMessage = getErrorMessage(error)
                        _speechResult.value = SpeechResult.Error(errorMessage)
                        isListening = false
                        Log.e(TAG, "Speech recognition error: $errorMessage")
                    }
                    
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val result = matches?.get(0) ?: ""
                        _speechResult.value = SpeechResult.Success(result)
                        isListening = false
                        Log.d(TAG, "Speech recognition result: $result")
                    }
                    
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partialResult = matches?.get(0) ?: ""
                        _speechResult.value = SpeechResult.PartialResult(partialResult)
                        Log.d(TAG, "Partial result: $partialResult")
                    }
                    
                    override fun onEvent(eventType: Int, params: Bundle?) {
                        Log.d(TAG, "Event: $eventType")
                    }
                })
            }
        } else {
            _speechResult.value = SpeechResult.Error("Speech recognition not available on this device")
        }
    }
    
    /**
     * Start listening for speech input
     */
    fun startListening() {
        if (isListening) return
        
        speechRecognizer?.let { recognizer ->
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            try {
                recognizer.startListening(intent)
                isListening = true
                _speechResult.value = SpeechResult.Listening
            } catch (e: Exception) {
                _speechResult.value = SpeechResult.Error("Failed to start listening: ${e.message}")
                Log.e(TAG, "Failed to start listening", e)
            }
        } ?: run {
            _speechResult.value = SpeechResult.Error("Speech recognizer not initialized")
        }
    }
    
    /**
     * Stop listening for speech input
     */
    fun stopListening() {
        if (!isListening) return
        
        speechRecognizer?.stopListening()
        isListening = false
        _speechResult.value = SpeechResult.Idle
    }
    
    /**
     * Cancel current speech recognition
     */
    fun cancel() {
        speechRecognizer?.cancel()
        isListening = false
        _speechResult.value = SpeechResult.Idle
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        isListening = false
    }
    
    /**
     * Get human-readable error message from error code
     */
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error: $errorCode"
        }
    }
    
    companion object {
        private const val TAG = "SpeechRecognitionService"
    }
}

/**
 * Sealed class representing different states of speech recognition
 */
sealed class SpeechResult {
    object Idle : SpeechResult()
    object Listening : SpeechResult()
    object Processing : SpeechResult()
    data class Success(val text: String) : SpeechResult()
    data class PartialResult(val text: String) : SpeechResult()
    data class Error(val message: String) : SpeechResult()
}
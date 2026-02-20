package com.talkar.app.ar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Service for handling speech recognition in AR interactions.
 * 
 * This service:
 * - Listens to user speech after video completion
 * - Converts speech to text
 * - Provides callbacks for recognition results
 * - Handles errors and edge cases
 * 
 * Usage:
 * ```
 * val speechService = SpeechRecognitionService(context)
 * speechService.onResult = { text ->
 *     // Handle recognized text
 * }
 * speechService.startListening()
 * ```
 */
class SpeechRecognitionService(private val context: Context) {
    
    companion object {
        private const val TAG = "SpeechRecognition"
        
        // Silence timeout in milliseconds
        private const val SILENCE_TIMEOUT_MS = 3000L
    }
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    // Callbacks
    var onResult: ((text: String) -> Unit)? = null
    var onError: ((error: String) -> Unit)? = null
    var onReadyForSpeech: (() -> Unit)? = null
    var onBeginningOfSpeech: (() -> Unit)? = null
    var onEndOfSpeech: (() -> Unit)? = null
    
    init {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition not available on this device")
            onError?.invoke("Speech recognition not available")
        }
    }
    
    /**
     * Starts listening for speech input.
     * 
     * @param language Language code (e.g., "en-US", "es-ES")
     * @param maxResults Maximum number of recognition results
     */
    fun startListening(
        language: String = Locale.getDefault().toString(),
        maxResults: Int = 1
    ) {
        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }
        
        try {
            // Create speech recognizer if needed
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(createRecognitionListener())
            }
            
            // Create recognition intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, maxResults)
                
                // Set silence timeout
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    SILENCE_TIMEOUT_MS
                )
                putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    SILENCE_TIMEOUT_MS
                )
                
                // Request partial results for better UX
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            
            // Start listening
            speechRecognizer?.startListening(intent)
            isListening = true
            
            Log.d(TAG, "Started listening (language: $language)")
            
        } catch (e: Exception) {
            val error = "Failed to start speech recognition: ${e.message}"
            Log.e(TAG, error, e)
            onError?.invoke(error)
            isListening = false
        }
    }
    
    /**
     * Stops listening for speech input.
     */
    fun stopListening() {
        if (!isListening) {
            return
        }
        
        try {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }
    
    /**
     * Cancels speech recognition.
     */
    fun cancel() {
        if (!isListening) {
            return
        }
        
        try {
            speechRecognizer?.cancel()
            isListening = false
            Log.d(TAG, "Cancelled speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }
    
    /**
     * Checks if currently listening.
     */
    fun isListening(): Boolean = isListening
    
    /**
     * Releases resources.
     * Call this when the service is no longer needed.
     */
    fun cleanup() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            Log.d(TAG, "Speech recognizer cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up speech recognizer", e)
        }
    }
    
    /**
     * Creates a recognition listener with callbacks.
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                onReadyForSpeech?.invoke()
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
                onBeginningOfSpeech?.invoke()
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Audio level changed (can be used for visual feedback)
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Audio buffer received
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                isListening = false
                onEndOfSpeech?.invoke()
            }
            
            override fun onError(error: Int) {
                isListening = false
                
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error: $error"
                }
                
                Log.e(TAG, "Recognition error: $errorMessage")
                onError?.invoke(errorMessage)
            }
            
            override fun onResults(results: Bundle?) {
                isListening = false
                
                results?.let { bundle ->
                    val matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        Log.i(TAG, "✅ Recognized: \"$recognizedText\"")
                        onResult?.invoke(recognizedText)
                    } else {
                        Log.w(TAG, "No recognition results")
                        onError?.invoke("No speech recognized")
                    }
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.let { bundle ->
                    val matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    
                    if (!matches.isNullOrEmpty()) {
                        val partialText = matches[0]
                        Log.d(TAG, "Partial result: \"$partialText\"")
                        // Could provide partial results to UI for better feedback
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Custom events
            }
        }
    }
}

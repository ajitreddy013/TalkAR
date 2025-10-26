package com.talkar.app.data.services

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service to handle Whisper API integration for speech-to-text conversion
 */
class WhisperService(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    
    // Initialize OkHttpClient with timeout settings
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Start recording audio for Whisper transcription
     */
    fun startRecording(): Boolean {
        if (isRecording) return false
        
        try {
            // Create temporary file for audio recording
            audioFile = File.createTempFile("whisper_audio", ".wav", context.cacheDir)
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // Using MP4 for better compatibility
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                setAudioSamplingRate(16000) // Standard sampling rate for Whisper
                setAudioEncodingBitRate(128000)
                
                prepare()
                start()
            }
            
            isRecording = true
            Log.d(TAG, "Started recording audio")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            return false
        }
    }
    
    /**
     * Stop recording and return the audio file
     */
    fun stopRecording(): File? {
        if (!isRecording) return null
        
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            Log.d(TAG, "Stopped recording audio")
            return audioFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            return null
        }
    }
    
    /**
     * Transcribe audio file using Whisper API
     *
     * @param audioFile The audio file to transcribe
     * @param apiKey The Whisper API key
     * @param language The language of the audio (optional)
     * @return The transcribed text or null if failed
     */
    suspend fun transcribeAudio(
        audioFile: File,
        apiKey: String,
        language: String? = null
    ): String? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
            // Handle cancellation if needed
        }
        
        try {
            // Create multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/wav".toMediaType())
                )
                .addFormDataPart("model", "whisper-1")
                
            // Add language parameter if provided
            language?.let {
                requestBody.addFormDataPart("language", it)
            }
            
            // Build request
            val request = Request.Builder()
                .url(WHISPER_API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody.build())
                .build()
            
            // Execute request
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e(TAG, "Whisper API call failed", e)
                    continuation.resume(null) {}
                }
                
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        if (!response.isSuccessful) {
                            Log.e(TAG, "Whisper API call unsuccessful: ${response.code}")
                            continuation.resume(null) {}
                            return
                        }
                        
                        val responseBody = response.body?.string()
                        if (responseBody.isNullOrEmpty()) {
                            Log.e(TAG, "Empty response from Whisper API")
                            continuation.resume(null) {}
                            return
                        }
                        
                        // Parse JSON response to extract transcription text
                        val transcription = parseTranscriptionResponse(responseBody)
                        continuation.resume(transcription) {}
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Whisper API response", e)
                        continuation.resume(null) {}
                    } finally {
                        response.close()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing Whisper API request", e)
            continuation.resume(null) {}
        }
    }
    
    /**
     * Parse the JSON response from Whisper API to extract transcription text
     *
     * @param jsonResponse The JSON response string
     * @return The transcribed text or null if parsing failed
     */
    private fun parseTranscriptionResponse(jsonResponse: String): String? {
        try {
            // Simple JSON parsing - in a production app, you'd use a proper JSON library
            // This is a basic implementation for demonstration
            val textStart = jsonResponse.indexOf("\"text\":\"")
            if (textStart == -1) return null
            
            val textEnd = jsonResponse.indexOf("\"", textStart + 8)
            if (textEnd == -1) return null
            
            return jsonResponse.substring(textStart + 8, textEnd).replace("\\n", "\n")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing transcription response", e)
            return null
        }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
        
        mediaRecorder = null
        isRecording = false
        
        // Delete temporary audio file
        audioFile?.delete()
        audioFile = null
    }
    
    companion object {
        private const val TAG = "WhisperService"
        private const val WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions"
    }
}
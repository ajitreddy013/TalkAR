package com.talkar.app.data.services

import android.content.Context
import android.util.Log
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.ConversationalQueryRequest
import com.talkar.app.data.api.PosterAdContentRequest
import com.talkar.app.data.api.LipSyncRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ConversationState {
    IDLE,
    SCANNING,
    LOADING, // New state for API calls
    DETECTED,
    PLAYING_INTRO,
    LISTENING,
    PLAYING_RESPONSE,
    FINISHED
}

// Updated to hold URL string or ResId
data class ARContent(
    val introVideoSource: Any, // Int or String
    val responseVideoSource: Any?, // Int or String, null initially
    val objectName: String,
    val posterId: String? = null
)

class ConversationalARService(
    private val context: Context,
    private val speechManager: SpeechRecognitionManager,
    private val imageMatcher: ImageMatcherService // 🔥 Shared instance
) {
    private val TAG = "ConversationalARService"
    private val scope = CoroutineScope(Dispatchers.Main)
    private var apiService = ApiClient.create()
    var mediaPlayer: android.media.MediaPlayer? = null // 🔥 SINGLE INSTANCE - Shared with UI

    private val _state = MutableStateFlow(ConversationState.IDLE)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val _currentContent = MutableStateFlow<ARContent?>(null)
    val currentContent: StateFlow<ARContent?> = _currentContent.asStateFlow()

    private val _currentVideoSource = MutableStateFlow<Any?>(null)
    val currentVideoSource: StateFlow<Any?> = _currentVideoSource.asStateFlow()
    
    // UI Message for loading/error
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    val transcript = speechManager.transcript
    val isListening = speechManager.isListening

    private val _isDetectionPaused = MutableStateFlow(false)
    val isDetectionPaused: StateFlow<Boolean> = _isDetectionPaused.asStateFlow()

    // Mapping object names to backend IDs for testing
    // In a real app, the recognition result would contain the ID directly
    private val contentMap = mapOf(
        "sunrich" to "poster_sunrich_123", 
        "lebron" to "poster_lebron_456",
        "chanel" to "poster_chanel_789"
    )

    fun startScanning() {
        if (_state.value == ConversationState.IDLE || _state.value == ConversationState.FINISHED) {
            _state.value = ConversationState.SCANNING
            speechManager.initialize()
            _uiMessage.value = null
        }
    }

    fun onObjectDetected(objectId: String, objectName: String) {
        if (_state.value != ConversationState.SCANNING || _isDetectionPaused.value) return
        
        Log.d(TAG, "Object detected: id=$objectId, name=$objectName")

        // Hotfix: Stop listening IMMEDIATELY to avoid "No match" noise/overlap
        speechManager.stopListening()

        // Determine the best key for content mapping
        val key = when {
            objectId.contains("sunrich", ignoreCase = true) || objectName.contains("Sunrich", ignoreCase = true) -> "sunrich"
            objectId.contains("lebron", ignoreCase = true) || objectName.contains("LeBron", ignoreCase = true) -> "lebron"
            objectId.contains("chanel", ignoreCase = true) || objectName.contains("Chanel", ignoreCase = true) -> "chanel"
            else -> {
                Log.w(TAG, "Unknown object detected: $objectName ($objectId)")
                return
            }
        }

        // Prefer backend ID from matcher result if it looks like a real ID
        val posterId = if (objectId.startsWith("poster_")) objectId else contentMap[key] ?: return
        
        Log.d(TAG, "Mapped to key: $key, posterId: $posterId")
        _state.value = ConversationState.LOADING
        _uiMessage.value = "Generating intro..."

        scope.launch {
            try {
                // Just identify the object first. Don't fetch video yet.
                val content = ARContent(
                    introVideoSource = 0, // Placeholder
                    responseVideoSource = null, 
                    objectName = key,
                    posterId = posterId
                )
                
                _currentContent.value = content
                _state.value = ConversationState.DETECTED
                _uiMessage.value = "Hold to scan..." // Or "Tap to interact"
                // Do NOT auto-play. Wait for confirmSelection()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve object", e)
                _state.value = ConversationState.SCANNING
            }
        }
    }
    
    // New method to manually trigger the flow
    fun confirmSelection() {
        if (_state.value != ConversationState.DETECTED) return
        
        val content = _currentContent.value ?: return
        Log.d(TAG, "Selection confirmed, loading content for ${content.objectName}...")
        
        _state.value = ConversationState.LOADING
        _uiMessage.value = "Generating intro..."
        
        scope.launch {
            try {
                // Fetch intro video (Logic moved here from onObjectDetected)
                val videoSource = fetchIntroFromBackend(content.posterId!!)
                
                if (videoSource == null) {
                    val fallback = getLocalFallback(content.objectName)
                    
                    if (fallback != 0) {
                        Log.w(TAG, "No backend intro available for ${content.objectName}, using local fallback: $fallback")
                        val updatedContent = content.copy(introVideoSource = fallback)
                        _currentContent.value = updatedContent
                        imageMatcher.onVideoStarted()
                        playIntro()
                    } else {
                        Log.e(TAG, "No local video found for product: ${content.objectName}")
                        _uiMessage.value = "Video not available"
                        delay(2000)
                        // Don't finish conversation here, maybe just go back to detected state?
                        // Or finish if we can't do anything.
                        // For now, let's go back to DETECTED so user can try again or scan something else.
                        _state.value = ConversationState.DETECTED
                    }
                    return@launch
                }
                
                val updatedContent = content.copy(introVideoSource = videoSource)
                _currentContent.value = updatedContent
                
                // Hotfix Fix 2: Extend cooldown in matcher
                imageMatcher.onVideoStarted()
                
                playIntro()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load intro", e)
                _uiMessage.value = "Failed. Retrying..."
                delay(2000)
                _state.value = ConversationState.DETECTED
                _isDetectionPaused.value = false // Resume on error
            }
        }
    }
    

    
    private suspend fun fetchIntroFromBackend(posterId: String): Any? {
        return withContext(Dispatchers.IO) {
            try {
                // Call generateAdContentFromPoster to get the intro video
                val response = apiService.generateAdContentFromPoster(
                    PosterAdContentRequest(image_id = posterId)
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.video_url
                    if (!url.isNullOrEmpty()) {
                        Log.d(TAG, "Fetched intro video URL: $url")
                        return@withContext url
                    }
                }
                
                Log.w(TAG, "Backend returned no video URL for intro")
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "API Error fetching intro", e)
                return@withContext null
            }
        }
    }
    private fun playIntro() {
        val content = _currentContent.value ?: return
        
        // Hotfix Fix 4: Destroy SpeechRecognizer immediately on confirmation
        speechManager.destroy()
        
        _isDetectionPaused.value = true // 🔥 PAUSE DETECTION
        _currentVideoSource.value = content.introVideoSource
        _state.value = ConversationState.PLAYING_INTRO
        _uiMessage.value = null
    }

    fun onVideoCompleted() {
        Log.d(TAG, "Video completed. Current state: ${_state.value}")
        when (_state.value) {
            ConversationState.PLAYING_INTRO -> startListening()
            ConversationState.PLAYING_RESPONSE -> finishConversation()
            else -> { /* No-op */ }
        }
    }

    private fun startListening() {
        _state.value = ConversationState.LISTENING
        _currentVideoSource.value = null // Clear video
        _uiMessage.value = "Listening..."
        
        // Start speech recognition
        speechManager.startListening(onSilence = {
            Log.d(TAG, "Silence detected, processing query...")
            processUserQuery(_transcript.value) // Using internal flow if accessible, or public property
        })
    }
    
    // Direct access for the service - assuming this property exists in potential previous versions or implied
    // Since transcript is public in SpeechManager, we can use it. But earlier code used _transcript.
    // Let's redefine it here as per original file standard if needed, or just use transcript property.
    // In original code (line 61): val transcript = speechManager.transcript
    // In original code (line 231): private val _transcript = speechManager.transcript
    private val _transcript = speechManager.transcript

    private var _contextImageFile: java.io.File? = null

    fun setContextImage(bitmap: android.graphics.Bitmap) {
        scope.launch(Dispatchers.IO) {
            try {
                val file = java.io.File(context.cacheDir, "context_image.jpg")
                val stream = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                stream.close()
                _contextImageFile = file
                Log.d(TAG, "Context image saved: ${file.length()} bytes")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save context image", e)
            }
        }
    }

    private fun processUserQuery(text: String) {
        // Ensure we stop listening
        speechManager.stopListening()
        
        if (text.isBlank()) {
            Log.d(TAG, "No speech detected")
            finishConversation()
            return
        }
        
        _state.value = ConversationState.LOADING
        _uiMessage.value = "Thinking..."
        
        val content = _currentContent.value ?: return
        val posterId = content.posterId ?: return
        
        scope.launch {
            try {
                // Check if we have a visual context
                val videoUrl = if (_contextImageFile != null && _contextImageFile!!.exists()) {
                    Log.d(TAG, "Sending VISUAL query with context image...")
                    fetchVisualResponseFromBackend(text, posterId, _contextImageFile!!)
                } else {
                    Log.d(TAG, "Sending text-only query...")
                    fetchResponseFromBackend(text, posterId)
                }
                
                if (videoUrl == null) {
                    val fallback = getLocalResponseFallback(content.objectName)
                    
                    if (fallback != 0) {
                        Log.w(TAG, "No backend response available for ${content.objectName}, using local fallback: $fallback")
                        val updatedContent = content.copy(responseVideoSource = fallback)
                        _currentContent.value = updatedContent
                        playResponse()
                    } else {
                         Log.e(TAG, "No local response video found for product: ${content.objectName}")
                        _uiMessage.value = "Response not available"
                        delay(2000)
                        finishConversation()
                    }
                    return@launch
                }
                
                // Update content with response
                val updatedContent = content.copy(responseVideoSource = videoUrl)
                _currentContent.value = updatedContent
                
                playResponse()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process query", e)
                _uiMessage.value = "Error processing. Try again."
                delay(2000)
                finishConversation()
            }
        }
    }

    private suspend fun fetchVisualResponseFromBackend(query: String, posterId: String, imageFile: java.io.File): String? {
        return withContext(Dispatchers.IO) {
            try {
                val requestFile = okhttp3.RequestBody.create("image/jpeg".tookhttpMediaTypeOrNull(), imageFile)
                val body = okhttp3.MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                val textPart = okhttp3.RequestBody.create("text/plain".tookhttpMediaTypeOrNull(), query)
                val posterIdPart = okhttp3.RequestBody.create("text/plain".tookhttpMediaTypeOrNull(), posterId)

                val response = apiService.sendVisualQuery(body, textPart, posterIdPart)

                if (response.isSuccessful && response.body()?.success == true) {
                    val textResponse = response.body()?.response
                    if (!textResponse.isNullOrEmpty()) {
                         // Chain to LipSync
                         val lipSyncResponse = apiService.generateLipSyncVideo(
                            LipSyncRequest(
                                imageId = posterId,
                                text = textResponse
                            )
                        )
                        if (lipSyncResponse.isSuccessful && lipSyncResponse.body()?.success == true) {
                            // Clear context image after successful use
                            _contextImageFile = null
                            return@withContext lipSyncResponse.body()?.videoUrl
                        }
                    }
                }
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "Visual API Error", e)
                return@withContext null
            }
        }
    }

    
    private suspend fun fetchResponseFromBackend(query: String, posterId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.processConversationalQuery(
                    ConversationalQueryRequest(
                        query = query,
                        imageId = posterId
                    )
                )
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()?.audioUrl
                    
                    val textResponse = response.body()?.response
                    if (!textResponse.isNullOrEmpty()) {
                        Log.d(TAG, "Got text response: $textResponse, generating video...")
                        
                        // Now call LipSync
                        val lipSyncResponse = apiService.generateLipSyncVideo(
                            LipSyncRequest(
                                imageId = posterId,
                                text = textResponse
                            )
                        )
                        
                        if (lipSyncResponse.isSuccessful && lipSyncResponse.body()?.success == true) {
                            return@withContext lipSyncResponse.body()?.videoUrl
                        }
                    }
                }
                
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "API Chain Error", e)
                return@withContext null
            }
        }
    }

    private fun playResponse() {
        val content = _currentContent.value ?: return
        
        // Ensure speech is destroyed if somehow still active
        speechManager.destroy()
        
        _isDetectionPaused.value = true // 🔥 PAUSE DETECTION
        _currentVideoSource.value = content.responseVideoSource
        _state.value = ConversationState.PLAYING_RESPONSE
        _uiMessage.value = null
    }

    private fun finishConversation() {
        _state.value = ConversationState.FINISHED
        _currentVideoSource.value = null
        _uiMessage.value = null
        
        // Auto-restart scanning after a delay (Emergency Hotfix: 3s strict delay)
        scope.launch {
            Log.d(TAG, "Video completed, re-enabling detection in 3 seconds...")
            delay(3000) // Strictly 3 seconds as requested
            _isDetectionPaused.value = false
            Log.d(TAG, "Detection re-enabled")
            
            _state.value = ConversationState.SCANNING
            _currentContent.value = null
            
            // Re-initialize speech for the next session
            speechManager.initialize()
        }
    }

    fun reset() {
        speechManager.destroy()
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = ConversationState.IDLE
        _currentContent.value = null
        _currentVideoSource.value = null
        _uiMessage.value = null
    }



    private fun getLocalFallback(objectName: String): Int {
        // Use explicit R.raw references for safety
        // Files are: sunrich_1.mp4, sunrich_2.mp4, etc.
        return when (objectName.lowercase()) {
            "sunrich" -> com.talkar.app.R.raw.sunrich_2 // Swapped based on user feedback
            "lebron" -> com.talkar.app.R.raw.lebron_1
            "chanel" -> com.talkar.app.R.raw.chanel_1
            else -> 0
        }
    }

    private fun getLocalResponseFallback(objectName: String): Int {
         return when (objectName.lowercase()) {
            "sunrich" -> com.talkar.app.R.raw.sunrich_1 // Swapped based on user feedback
            "lebron" -> com.talkar.app.R.raw.lebron_2
            "chanel" -> com.talkar.app.R.raw.chanel_2
            else -> 0
        }
    }

    private fun String.tookhttpMediaTypeOrNull(): okhttp3.MediaType? {
        return this.toMediaTypeOrNull()
    }
}

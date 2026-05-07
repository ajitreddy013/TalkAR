package com.talkar.app.data.services

import android.content.Context
import com.google.ar.core.LightEstimate
import com.talkar.app.data.models.AdContent
import com.talkar.app.data.models.ImageRecognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal stub of EnhancedARService to satisfy compile-time references across the project.
 * Provides lightweight defaults and no-op implementations for features used by UI and tests.
 */
class EnhancedARService(private val context: Context) {

	/** Lighting quality categories used by environmental realism features. */
	enum class LightingQuality {
		EXCELLENT,
		GOOD,
		FAIR,
		POOR,
		UNKNOWN
	}

	/** Tracking quality categories used by UI to show corner colors and hints. */
	enum class TrackingQuality {
		EXCELLENT,
		GOOD,
		FAIR,
		POOR,
		UNKNOWN
	}

	/** Motion stability for UI guidance */
	enum class MotionStability {
		STABLE,
		MODERATE,
		UNSTABLE,
		UNKNOWN
	}

	// Underlying AR service (real detection engine) and scope
	private val imageService = ARImageRecognitionService(context)
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	// Public observable state flows consumed by viewmodels and UI
	private val _isTracking = MutableStateFlow(false)
	val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

	// Simple tracking state alias to match consumers expecting com.google.ar.core.TrackingState
	private val _trackingState = MutableStateFlow(com.google.ar.core.TrackingState.STOPPED)
	val trackingState: StateFlow<com.google.ar.core.TrackingState> = _trackingState.asStateFlow()

	private val _trackingQuality = MutableStateFlow(TrackingQuality.UNKNOWN)
	val trackingQuality: StateFlow<TrackingQuality> = _trackingQuality.asStateFlow()

	private val _lightingQuality = MutableStateFlow(LightingQuality.UNKNOWN)
	val lightingQuality: StateFlow<LightingQuality> = _lightingQuality.asStateFlow()

	private val _lightEstimate = MutableStateFlow<LightEstimate?>(null)
	val lightEstimate: StateFlow<LightEstimate?> = _lightEstimate.asStateFlow()

	private val _isAvatarSpeaking = MutableStateFlow(false)
	val isAvatarSpeaking: StateFlow<Boolean> = _isAvatarSpeaking.asStateFlow()

	// Additional UI-facing flows and state
	private val _recognizedImages = MutableStateFlow<List<com.google.ar.core.AugmentedImage>>(emptyList())
	val recognizedImages: StateFlow<List<com.google.ar.core.AugmentedImage>> = _recognizedImages.asStateFlow()

	private val _motionStability = MutableStateFlow(MotionStability.UNKNOWN)
	val motionStability: StateFlow<MotionStability> = _motionStability.asStateFlow()

	private val _trackingGuidance = MutableStateFlow<String?>(null)
	val trackingGuidance: StateFlow<String?> = _trackingGuidance.asStateFlow()

	private val _error = MutableStateFlow<String?>(null)
	val error: StateFlow<String?> = _error.asStateFlow()

	// Ad content cache
	private val adContentCache = ConcurrentHashMap<String, AdContent>()

	/**
	 * Initialize the AR service. This bridges to ARImageRecognitionService in the background.
	 * Returns true to indicate initialization was started.
	 */
	fun initialize(): Boolean {
		// Bridge flows once during initialization
		synchronized(this) {
			if (!_bridgeStarted) {
				startBridgingFlows()
				_bridgeStarted = true
			}
		}
		// Start underlying initialization asynchronously to avoid blocking UI
		scope.launch {
			val ok = imageService.initialize()
			if (!ok) {
				_error.value = imageService.error.value ?: "Failed to initialize AR service"
			}
		}
		_trackingState.value = com.google.ar.core.TrackingState.STOPPED
		return true
	}

	@Volatile
	private var _bridgeStarted: Boolean = false

	private fun startBridgingFlows() {
		// Mirror tracking flag and map to trackingState
		scope.launch {
			imageService.isTracking.collect { tracking ->
				_isTracking.value = tracking
				_trackingState.value = if (tracking) com.google.ar.core.TrackingState.TRACKING else com.google.ar.core.TrackingState.PAUSED
			}
		}
		// Mirror recognized images
		scope.launch {
			imageService.recognizedImages.collect { imgs ->
				_recognizedImages.value = imgs
			}
		}
		// Mirror errors
		scope.launch {
			imageService.error.collect { err ->
				_error.value = err
			}
		}
		// Lighting/tracking quality not provided by imageService; keep UNKNOWN defaults
	}

	fun resumeTracking() {
		// Use resumeProcessing() to avoid GL frame loop where not available
		imageService.resumeProcessing()
	}

	fun pauseTracking() {
		imageService.pauseProcessing()
	}

	fun stopTracking() {
		imageService.stopTracking()
		_isTracking.value = false
		_trackingState.value = com.google.ar.core.TrackingState.STOPPED
	}

	/**
	 * Return cached AdContent if available
	 */
	fun getCachedAdContent(imageId: String): AdContent? = adContentCache[imageId]

	/**
	 * Cache AdContent for future use
	 */
	fun cacheAdContent(imageId: String, adContent: AdContent) {
		adContentCache[imageId] = adContent
	}

	fun setAvatarSpeaking(isSpeaking: Boolean) {
		_isAvatarSpeaking.value = isSpeaking
	}

	// Ambient audio controls - no-op stubs
	fun startAmbientAudio() {}
	fun stopAmbientAudio() {}
	fun pauseAmbientAudio() {}
	fun resumeAmbientAudio() {}

	/**
	 * Generate ad content for the provided backend image id. Returns Result wrapping AdContent or error.
	 * Calls the production generation pipeline via ApiClient.
	 */
	suspend fun generateAdContentForImage(imageId: String, productName: String): Result<AdContent?> {
		// Check cache first
		val cachedContent = getCachedAdContent(imageId)
		if (cachedContent != null) {
			return Result.success(cachedContent)
		}
		
		return try {
			val apiClient = com.talkar.app.TalkARApplication.instance.apiClient
			val request = com.talkar.app.data.api.PosterAdContentRequest(image_id = imageId)
			val response = apiClient.generateAdContentFromPoster(request)
			
			if (response.isSuccessful) {
				val body = response.body()
				if (body != null && body.success) {
					val adContent = AdContent(
						script = body.script ?: "Welcome to $productName!",
						audioUrl = body.audio_url,
						videoUrl = body.video_url,
						productName = body.metadata?.product_name ?: productName
					)
					cacheAdContent(imageId, adContent)
					Result.success(adContent)
				} else {
					Result.failure(Exception(body?.script ?: "Failed to generate ad content"))
				}
			} else {
				Result.failure(Exception("API error: ${response.code()}"))
			}
		} catch (e: Exception) {
			android.util.Log.e("EnhancedARService", "Error generating ad content", e)
			Result.failure(e)
		}
	}

	/**
	 * Get recognized image by name. Some callers expect this API on AR service.
	 */
	fun getRecognizedImage(name: String): ImageRecognition? = imageService.getRecognizedImage(name)

	/**
	 * Return a small set of tracking metrics used in logs/tests.
	 */
	fun getTrackingMetrics(): Map<String, Any> = mapOf(
		"trackingState" to _trackingState.value.name,
		"isTracking" to _isTracking.value,
		"recognizedImagesCount" to _recognizedImages.value.size
	)

	/** Helpers for tests to mutate stub state */
	fun setLightingQuality(quality: LightingQuality) { _lightingQuality.value = quality }
	fun setTrackingQuality(quality: TrackingQuality) { _trackingQuality.value = quality }
	fun setLightEstimate(estimate: LightEstimate?) { _lightEstimate.value = estimate }

	// --- ARCore Delegation Methods ---

	/** Get the underlying AR session if available */
	fun getSession(): com.google.ar.core.Session? = imageService.getSession()

	/** Process an AR frame for image recognition */
	fun processFrame(frame: com.google.ar.core.Frame) {
		imageService.processFrame(frame)
	}

	/** Set display geometry for the AR session */
	fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
		imageService.getSession()?.setDisplayGeometry(rotation, width, height)
	}

	/** Set camera texture name for the AR session */
	fun setCameraTextureName(textureId: Int) {
		imageService.getSession()?.setCameraTextureName(textureId)
	}

	/** Load images from API into AR database */
	suspend fun loadImagesFromAPI(images: List<ImageRecognition>) {
		imageService.loadImagesFromAPI(images)
	}

	/** Clean up all resources used by the service */
	fun cleanup() {
		imageService.cleanup()
	}
}
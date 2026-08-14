package com.talkar.app.ui.components

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import android.graphics.Bitmap
import com.talkar.app.data.services.ImageMatcherService
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.content.ContextCompat
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.google.ar.core.ArCoreApk
import com.talkar.app.ar.video.models.TrackingData
import com.talkar.app.ar.video.tracking.ARTrackingManager
import com.talkar.app.ar.video.tracking.ARTrackingManagerImpl
import com.talkar.app.data.repository.PosterRepository
import com.talkar.app.ar.video.tracking.ReferencePoster
import com.talkar.app.ar.video.tracking.TrackingListener
import com.talkar.app.ar.video.tracking.TrackedPoster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Composable wrapper for AR Camera View with poster detection.
 * 
 * Integrates ARTrackingManager for poster detection and tracking.
 * 
 * Requirements: 1.1, 1.4, 6.1, 6.2
 */
@Composable
fun ArSceneViewComposable(
    modifier: Modifier = Modifier,
    onPreviewReady: () -> Unit = {},
    onPreviewModeChanged: (String) -> Unit = {},
    onTrackingReady: () -> Unit = {},
    onPosterSourceChanged: (String) -> Unit = {},
    onPosterDatabaseReady: (count: Int, source: String) -> Unit = { _, _ -> },
    onPosterDetected: (posterId: String, posterName: String, anchor: com.google.ar.core.Anchor?) -> Unit = { _, _, _ -> },
    onPosterLost: (posterId: String) -> Unit = {},
    onTrackingUpdate: (trackingData: TrackingData) -> Unit = {},
    onError: (errorMessage: String) -> Unit = {},
    sessionRecreateTrigger: Int = 0,
    softResetTrigger: Int = 0
) {
    // Log composable entry
    Log.d("ArSceneView", "🎬 ArSceneViewComposable entered")
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val imageMatcher = remember { ImageMatcherService(context) }
    
    var arTrackingManager by remember { mutableStateOf<ARTrackingManager?>(null) }
    var session by remember { mutableStateOf<Session?>(null) }
    var glSurfaceViewRef by remember { mutableStateOf<GLSurfaceView?>(null) }
    // isInitializing: true only while ARCore session itself is being created (fast, <1s).
    // Poster loading happens in background after camera is already showing.
    var isInitializing by remember { mutableStateOf(true) }
    var isLoadingPosters by remember { mutableStateOf(false) }
    var frameProcessingStarted by remember { mutableStateOf(false) }
    var cameraRendererActivated by remember { mutableStateOf(false) }
    var isSessionRunning by remember { mutableStateOf(false) }
    var isResumingSession by remember { mutableStateOf(false) }
    var useBasicCameraFallback by remember { mutableStateOf(false) }
    var currentlyTrackedPosterId by remember { mutableStateOf<String?>(null) }
    var posterDetectionLocked by remember { mutableStateOf(false) }
    var sessionRecreateCount by remember { mutableIntStateOf(0) }
    var internalRecreateTrigger by remember { mutableIntStateOf(0) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastErrorTimestamp by remember { mutableLongStateOf(0L) }
    var lastErrorMessage by remember { mutableStateOf<String?>(null) }
    var posterRepositoryRef by remember { mutableStateOf<com.talkar.app.data.repository.PosterRepository?>(null) }
    val latestViewMatrix = remember { FloatArray(16) { if (it % 5 == 0) 1f else 0f } }
    val latestProjectionMatrix = remember { FloatArray(16) { if (it % 5 == 0) 1f else 0f } }
    
    Log.d("ArSceneView", "📊 State: isInitializing=$isInitializing, session=${session != null}, frameProcessingStarted=$frameProcessingStarted, recreateCount=$sessionRecreateCount")

    LaunchedEffect(sessionRecreateTrigger) {
        posterDetectionLocked = false
        currentlyTrackedPosterId = null
        arTrackingManager?.refreshScan()
    }

    // Soft reset: clear tracking state and unlock detection.
    // ARTrackingManagerImpl.refreshScan() records trackedIndexAtReset so the
    // previously-tracked poster is skipped until it leaves frame; any other
    // poster is accepted immediately — no cooldown delay needed.
    LaunchedEffect(softResetTrigger) {
        if (softResetTrigger > 0) {
            currentlyTrackedPosterId = null
            arTrackingManager?.refreshScan()
            posterDetectionLocked = false
            Log.d("ArSceneView", "Soft reset: detection unlocked")
        }
    }
    
    fun resumeSessionSafely(source: String) {
        val currentSession = session ?: return
        if (isSessionRunning || isResumingSession) {
            Log.d("ArSceneView", "Skip resume ($source): isRunning=$isSessionRunning, isResuming=$isResumingSession")
            return
        }
        
        isResumingSession = true
        Log.d("ArSceneView", "Attempting to resume AR session ($source)...")
        
        try {
            currentSession.resume()
            // We set isSessionRunning to true here, but the renderer will only start
            // drawing once session.update() actually succeeds in onDrawFrame.
            isSessionRunning = true
            Log.d("ArSceneView", "✅ AR session resume() called successfully ($source)")
        } catch (e: SessionNotPausedException) {
            isSessionRunning = true
            Log.d("ArSceneView", "AR session already running ($source)")
        } catch (e: CameraNotAvailableException) {
            Log.e("ArSceneView", "❌ Camera not available yet ($source), will retry on next frame or lifecycle event", e)
            // Notify UI but don't fail permanently yet
            onError("Camera is busy or not ready. Please wait...")
        } catch (e: Exception) {
            Log.e("ArSceneView", "❌ Failed to resume AR session ($source)", e)
            if (e is FatalException || e.message?.contains("fatal") == true || e.message?.contains("sensor") == true) {
                Log.e("ArSceneView", "🚨 Fatal ARCore error detected (sensor/native). Immediately switching to CameraX fallback.")
                
                // Clean up session and state
                isSessionRunning = false
                frameProcessingStarted = false
                cameraRendererActivated = false
                
                val oldSession = session
                session = null
                arTrackingManager?.release()
                arTrackingManager = null

                // Close after state detachment so GL thread won't keep touching a stale session instance.
                scope.launch(Dispatchers.Main) {
                    delay(50)
                    try {
                        oldSession?.close()
                    } catch (closeEx: Exception) {
                        Log.w("ArSceneView", "Error closing failed session", closeEx)
                    }
                }
                
                useBasicCameraFallback = true
                onPreviewModeChanged("CAMERAX_FALLBACK")
                onError("AR hardware is unresponsive. Using basic camera mode.")
            }
        } finally {
            isResumingSession = false
        }
    }

    // Lifecycle management for AR session
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("ArSceneView", "Lifecycle resumed")
                    glSurfaceViewRef?.onResume()
                    resumeSessionSafely("lifecycle")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Log.d("ArSceneView", "Lifecycle paused")
                    glSurfaceViewRef?.onPause()
                    val currentSession = session
                    if (currentSession != null && isSessionRunning) {
                        try {
                            currentSession.pause()
                            isSessionRunning = false
                            isResumingSession = false // Also reset this if it was in progress
                            Log.d("ArSceneView", "AR session paused")
                        } catch (e: Exception) {
                            Log.e("ArSceneView", "Failed to pause AR session", e)
                        }
                    } else {
                        isResumingSession = false
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d("ArSceneView", "🗑️ ArSceneViewComposable disposed, closing AR session")
            lifecycleOwner.lifecycle.removeObserver(observer)
            
            // Reset all state flags to prevent leakage on re-entry
            isSessionRunning = false
            isResumingSession = false
            frameProcessingStarted = false
            cameraRendererActivated = false
            frameCount = 0
            sessionRecreateCount = 0 
            
            val currentSession = session
            session = null
            if (currentSession != null) {
                try {
                    currentSession.pause()
                    currentSession.close()
                    Log.d("ArSceneView", "✅ AR session closed successfully")
                } catch (e: Exception) {
                    Log.e("ArSceneView", "Error closing AR session", e)
                }
            }
            
            // Release tracking manager resources
            arTrackingManager?.release()
            Log.d("ArSceneView", "✅ Tracking manager released")
            
            imageMatcher.destroy()
            Log.d("ArSceneView", "✅ Image matcher destroyed")
            
            glSurfaceViewRef?.onPause()
            glSurfaceViewRef = null
        }
    }

    // Resume once after session becomes available while already in foreground.
    LaunchedEffect(session) {
        if (session != null && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            // Small delay avoids resume races during first composition/observer attachment.
            delay(120)
            resumeSessionSafely("session-ready")
        }
    }
    
    var hasInitStarted by remember { mutableStateOf(false) }

    // Initialize ARCore session and tracking manager on background thread
    // Listens to triggers to support recovery from fatal errors or manual resets
    LaunchedEffect(sessionRecreateTrigger, internalRecreateTrigger) {
        if (hasInitStarted && isInitializing) return@LaunchedEffect
        hasInitStarted = true
        isInitializing = true
        
        withContext(Dispatchers.IO) {
            try {
                Log.d("ArSceneView", "Checking ARCore availability...")
                
                // Check if ARCore is supported and up to date
                var availability = ArCoreApk.getInstance().checkAvailability(context)
                
                if (availability.isUnknown) {
                    var checks = 0
                    while (availability.isUnknown && checks < 25) { // up to 5 seconds
                        delay(200)
                        availability = ArCoreApk.getInstance().checkAvailability(context)
                        checks++
                    }
                }
                
                when (availability) {
                    ArCoreApk.Availability.SUPPORTED_INSTALLED -> {
                        Log.d("ArSceneView", "✅ ARCore supported and installed")
                    }
                    ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                    ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                        Log.w("ArSceneView", "⚠️ ARCore needs update or installation. Falling back to CameraX.")
                        withContext(Dispatchers.Main) {
                            useBasicCameraFallback = true
                            onPreviewModeChanged("CAMERAX_FALLBACK")
                            onError("AR mode requires an ARCore update. Using basic camera mode instead.")
                        }
                        return@withContext
                    }
                    else -> {
                        Log.e("ArSceneView", "❌ ARCore not supported on this device ($availability)")
                        withContext(Dispatchers.Main) {
                            useBasicCameraFallback = true
                            onPreviewModeChanged("CAMERAX_FALLBACK")
                            onError("AR is not supported on this device. Using basic camera mode.")
                        }
                        return@withContext
                    }
                }
                
                Log.d("ArSceneView", "Creating ARCore session...")
                
                // Create ARCore session with comprehensive error handling
                val arSession = try {
                    Session(context)
                } catch (e: UnavailableArcoreNotInstalledException) {
                    Log.e("ArSceneView", "❌ ARCore not installed", e)
                    withContext(Dispatchers.Main) {
                        onError("ARCore is not installed. Please install from Play Store.")
                    }
                    return@withContext
                } catch (e: UnavailableApkTooOldException) {
                    Log.e("ArSceneView", "❌ ARCore APK too old", e)
                    withContext(Dispatchers.Main) {
                        onError("ARCore is outdated. Please update from Play Store.")
                    }
                    return@withContext
                } catch (e: UnavailableSdkTooOldException) {
                    Log.e("ArSceneView", "❌ Android SDK too old", e)
                    withContext(Dispatchers.Main) {
                        onError("Your Android version is too old for ARCore.")
                    }
                    return@withContext
                } catch (e: UnavailableDeviceNotCompatibleException) {
                    Log.e("ArSceneView", "❌ Device not compatible with ARCore", e)
                    withContext(Dispatchers.Main) {
                        onError("Your device is not compatible with ARCore.")
                    }
                    return@withContext
                } catch (e: IllegalArgumentException) {
                    // Handle vendor-specific camera tag errors (e.g., OPPO tags on non-OPPO devices)
                    if (e.message?.contains("Could not find tag") == true) {
                        Log.w("ArSceneView", "⚠️ Vendor-specific camera tag not found (this is normal on non-vendor devices)")
                        Log.w("ArSceneView", "Continuing with standard ARCore features...")
                        try {
                            Session(context) // Try again, ARCore should continue despite the warning
                        } catch (retryException: Exception) {
                            Log.e("ArSceneView", "❌ Failed to create session on retry", retryException)
                            withContext(Dispatchers.Main) {
                                useBasicCameraFallback = true
                                onPreviewModeChanged("CAMERAX_FALLBACK")
                                onError("Failed to initialize AR camera: ${retryException.message}")
                            }
                            return@withContext
                        }
                    } else {
                        Log.e("ArSceneView", "❌ Illegal argument creating session", e)
                        withContext(Dispatchers.Main) {
                            useBasicCameraFallback = true
                            onPreviewModeChanged("CAMERAX_FALLBACK")
                            onError("AR configuration not supported on this device.")
                        }
                        return@withContext
                    }
                } catch (e: Exception) {
                    Log.e("ArSceneView", "❌ Unexpected error creating ARCore session", e)
                    withContext(Dispatchers.Main) {
                        useBasicCameraFallback = true
                        onPreviewModeChanged("CAMERAX_FALLBACK")
                        onError("Failed to initialize AR: ${e.message}")
                    }
                    return@withContext
                }
                
                Log.d("ArSceneView", "✅ ARCore session created successfully")
                
                // Configure session optimized for image tracking
                val config = Config(arSession).apply {
                    focusMode = Config.FocusMode.AUTO
                    
                    // Use LATEST_CAMERA_IMAGE to prevent GL thread hangs if camera is slow
                    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    
                    // Disable features that might cause performance issues
                    planeFindingMode = Config.PlaneFindingMode.DISABLED
                    lightEstimationMode = Config.LightEstimationMode.DISABLED
                    depthMode = Config.DepthMode.DISABLED
                    
                    // Enable instant placement mode for faster tracking
                    instantPlacementMode = Config.InstantPlacementMode.DISABLED
                    
                    Log.d("ArSceneView", "Configured ARCore with BLOCKING mode for stable tracking")
                }
                
                // Check if configuration is supported
                if (!arSession.isSupported(config)) {
                    Log.e("ArSceneView", "❌ ARCore configuration not supported on this device")
                    withContext(Dispatchers.Main) {
                        onError("AR configuration not supported on this device.")
                    }
                    arSession.close()
                    return@withContext
                }
                
                arSession.configure(config)
                Log.d("ArSceneView", "✅ ARCore session configured. UpdateMode=${config.updateMode}, FocusMode=${config.focusMode}")

                // Create tracking manager and register listener BEFORE publishing session.
                val trackingManager = ARTrackingManagerImpl(context, arSession)

                trackingManager.setListener(object : TrackingListener {
                    override fun onPosterDetected(poster: TrackedPoster) {
                        if (posterDetectionLocked) {
                            Log.d("ArSceneView", "Detection locked — ignoring: ${poster.id}")
                            return
                        }
                        posterDetectionLocked = true
                        Log.d("ArSceneView", "Poster detected: id=${poster.id} name=${poster.name}")
                        currentlyTrackedPosterId = poster.id
                        val source = when {
                            poster.id.startsWith("test_") -> "test_fallback"
                            poster.id.startsWith("fallback_") -> "bundled_fallback"
                            else -> "backend"
                        }
                        onPosterSourceChanged(source)
                        onPosterDetected(poster.id, poster.name, poster.anchor)
                    }

                    override fun onPosterTracking(poster: TrackedPoster) {
                        val pose = poster.anchor.pose
                        Log.d("ArSceneView", "onPosterTracking: extentX=${poster.extentX}m extentZ=${poster.extentZ}m ratio=${poster.extentZ/poster.extentX}")
                        onTrackingUpdate(TrackingData(
                            position = com.talkar.app.ar.video.models.Vector3(pose.tx(), pose.ty(), pose.tz()),
                            rotation = com.talkar.app.ar.video.models.Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()),
                            scale = com.talkar.app.ar.video.models.Vector2(poster.extentX, poster.extentZ),
                            isTracking = true,
                            timestamp = System.currentTimeMillis(),
                            viewMatrix = latestViewMatrix.copyOf(),
                            projectionMatrix = latestProjectionMatrix.copyOf()
                        ))
                    }

                    override fun onPosterLost(posterId: String) {
                        Log.d("ArSceneView", "Poster lost: $posterId")
                        if (currentlyTrackedPosterId == posterId) currentlyTrackedPosterId = null
                        onPosterLost(posterId)
                    }

                    override fun onDetectionTimeout() {
                        Log.w("ArSceneView", "Poster detection timeout")
                        scope.launch(Dispatchers.Main) {
                            onError("No poster detected. Please point camera at a poster with a human face.")
                        }
                    }
                })

                // ── Phase 1: Publish session AND clear isInitializing so camera shows immediately ──
                withContext(Dispatchers.Main) {
                    arTrackingManager = trackingManager
                    session = arSession
                    isInitializing = false   // camera view renders now; poster loading continues below
                    isLoadingPosters = true
                }
                Log.d("ArSceneView", "Loading posters from backend (camera already showing)...")

                val posterRepository = com.talkar.app.data.repository.PosterRepository(
                    apiClient = com.talkar.app.TalkARApplication.instance.apiClient,
                    imageRepository = com.talkar.app.TalkARApplication.instance.imageRepository,
                    context = context
                )
                withContext(Dispatchers.Main) { posterRepositoryRef = posterRepository }

                val postersResult = withContext(Dispatchers.IO) {
                    try {
                        kotlinx.coroutines.withTimeout(60_000) { posterRepository.loadPosters() }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Log.w("ArSceneView", "Poster loading timed out; using bundled fallback")
                        posterRepository.loadBundledFallbackPosters().recoverCatching {
                            posterRepository.loadTestPoster().getOrThrow().let { listOf(it) }
                        }
                    }
                }

                val posters: List<ReferencePoster> = postersResult.getOrElse {
                    Log.e("ArSceneView", "Failed to load posters", it)
                    posterRepository.loadTestPoster().getOrNull()?.let { listOf(it) } ?: emptyList()
                }

                imageMatcher.loadTemplates(posters)

                val posterDatabaseSource = if (posters.isEmpty()) "none"
                    else PosterRepository.classifySourceByPosterId(posters.first().id).name.lowercase()
                withContext(Dispatchers.Main) {
                    onPosterDatabaseReady(posters.size, posterDatabaseSource)
                }

                if (posters.isNotEmpty()) {
                    Log.d("ArSceneView", "Initializing tracking with ${posters.size} posters")
                    val initResult = trackingManager.initialize(posters)
                    if (initResult.isSuccess) {
                        try {
                            val sessionConfig = arSession.config
                            val database = trackingManager.getAugmentedImageDatabase()
                            if (database != null) {
                                sessionConfig.augmentedImageDatabase = database
                                arSession.configure(sessionConfig)
                                Log.d("ArSceneView", "✅ ARCore image database loaded: ${database.numImages} images")
                            }
                        } catch (e: Exception) {
                            Log.e("ArSceneView", "Failed to update ARCore config with image database", e)
                        }
                    } else {
                        // Fallback: try test poster
                        posterRepository.loadTestPoster().getOrNull()?.let { tp ->
                            trackingManager.initialize(listOf(tp)).onSuccess {
                                try {
                                    val db = trackingManager.getAugmentedImageDatabase()
                                    if (db != null) {
                                        val cfg = arSession.config
                                        cfg.augmentedImageDatabase = db
                                        arSession.configure(cfg)
                                        Log.d("ArSceneView", "✅ ARCore initialized with test poster fallback")
                                    }
                                } catch (e: Exception) {
                                    Log.e("ArSceneView", "Failed to configure ARCore with test poster", e)
                                }
                            }
                        }
                    }
                } else {
                    Log.w("ArSceneView", "⚠️ No posters loaded — detection will not work")
                }

                withContext(Dispatchers.Main) {
                    isLoadingPosters = false
                    onTrackingReady()
                }
                
            } catch (e: Exception) {
                Log.e("ArSceneView", "❌ Failed to initialize ARCore", e)
                withContext(Dispatchers.Main) {
                    lastErrorMessage = "Init Error: ${e.message}"
                    lastErrorTimestamp = System.currentTimeMillis()
                    onError("Failed to initialize AR: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isInitializing = false
                }
            }
        }
    }

    // Periodic backend poster index refresh while AR screen is active.
    // To avoid disrupting active playback/tracking, only reconfigure when no poster is currently tracked.
    LaunchedEffect(session, arTrackingManager, useBasicCameraFallback, currentlyTrackedPosterId) {
        while (isActive) {
            delay(120_000) // 2 minutes
            
            if (currentlyTrackedPosterId != null) {
                Log.d("ArSceneView", "Skipping periodic poster refresh; poster is currently tracked")
                continue
            }
            
            val posterRepository = posterRepositoryRef ?: continue
            
            try {
                val refreshedPosters = withContext(Dispatchers.IO) {
                    posterRepository.loadPosters().getOrElse { emptyList() }
                }
                if (refreshedPosters.isEmpty()) {
                    Log.w("ArSceneView", "Periodic poster refresh returned empty list")
                    continue
                }
                
                imageMatcher.loadTemplates(refreshedPosters)
                
                val arSession = session
                val trackingManager = arTrackingManager
                if (arSession != null && trackingManager != null) {
                    val initResult = withContext(Dispatchers.IO) {
                        trackingManager.initialize(refreshedPosters)
                    }
                    if (initResult.isSuccess) {
                        try {
                            val cfg = arSession.config
                            val database = trackingManager.getAugmentedImageDatabase()
                            if (database != null) {
                                cfg.augmentedImageDatabase = database
                                arSession.configure(cfg)
                                Log.d("ArSceneView", "Periodic poster refresh applied to ARCore: ${database.numImages} posters")
                            }
                        } catch (e: Exception) {
                            Log.e("ArSceneView", "Failed applying periodic poster refresh config to ARCore", e)
                        }
                    } else {
                        Log.w("ArSceneView", "Periodic poster refresh initialize failed for ARCore")
                    }
                }
            } catch (e: Exception) {
                Log.e("ArSceneView", "Periodic poster refresh failed", e)
            }
        }
    }
    
    // Watchdog: If ARCore fails to produce frames after initialization, fallback to CameraX
    LaunchedEffect(isInitializing, session) {
        if (!isInitializing && session != null) {
            Log.d("ArSceneView", "⏱️ Starting 15s frame watchdog")
            delay(15000)
            if (frameCount == 0 && !useBasicCameraFallback) {
                Log.e("ArSceneView", "🚨 Watchdog triggered: No frames detected after 15s. Falling back to CameraX.")
                useBasicCameraFallback = true
                onPreviewModeChanged("CAMERAX_FALLBACK")
                onError("AR initialization timed out. Using basic camera.")
            } else {
                Log.d("ArSceneView", "✅ Watchdog satisfied: frameCount=$frameCount")
            }
        }
    }
    
    // --- UI Layout ---
    Box(modifier = modifier) {
        if (useBasicCameraFallback) {
            onPreviewModeChanged("CAMERAX_FALLBACK")

            LaunchedEffect(useBasicCameraFallback) {
                if (useBasicCameraFallback) {
                    val posterRepository = posterRepositoryRef ?: PosterRepository(
                        apiClient = com.talkar.app.TalkARApplication.instance.apiClient,
                        imageRepository = com.talkar.app.TalkARApplication.instance.imageRepository,
                        context = context
                    ).also { posterRepositoryRef = it }

                    val fallbackPosters = withContext(Dispatchers.IO) {
                        posterRepository.loadPosters().getOrElse { emptyList() }
                    }

                    if (fallbackPosters.isNotEmpty()) {
                        imageMatcher.loadTemplates(fallbackPosters)
                    }

                    val source = when {
                        fallbackPosters.isEmpty() -> "none"
                        else -> PosterRepository.classifySourceByPosterId(fallbackPosters.first().id).name.lowercase()
                    }
                    onPosterDatabaseReady(fallbackPosters.size, source)
                    onTrackingReady()
                }
            }
            
            BasicCameraPreview(
                modifier = Modifier.fillMaxSize(),
                lifecycleOwner = lifecycleOwner,
                imageMatcher = imageMatcher,
                isDetectionLocked = posterDetectionLocked,
                onPosterDetected = { posterId, posterName ->
                    if (posterDetectionLocked) return@BasicCameraPreview
                    posterDetectionLocked = true
                    currentlyTrackedPosterId = posterId
                    val source = PosterRepository.classifySourceByPosterId(posterId).name.lowercase()
                    onPosterSourceChanged(source)
                    onPosterDetected(posterId, posterName, null)
                },
                onPreviewReady = onPreviewReady,
                onError = onError
            )
        } else if (isInitializing) {
            // Session creation is fast (<1s) — minimal black screen with a clear message.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                    Text("Starting camera...", color = Color.White, fontSize = 15.sp)
                }
            }
        } else {
            // Render AR camera view only after initialization confirmed SUPPORTED
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val layout = createARCameraView(context = ctx)
                    val tagPair = layout.tag as? Pair<*, *>
                    glSurfaceViewRef = tagPair?.first as? GLSurfaceView
                    layout
                },
                update = { view ->
                    if (session != null) {
                        onPreviewReady()
                        if (!frameProcessingStarted) {
                            frameProcessingStarted = true
                            setupCameraRenderer(
                                view = view,
                                sessionProvider = { session },
                                trackingManagerProvider = { arTrackingManager },
                                shouldProcessTrackingProvider = { true },
                                isSessionRunningProvider = { isSessionRunning },
                                latestViewMatrix = latestViewMatrix,
                                latestProjectionMatrix = latestProjectionMatrix,
                                lifecycleOwner = lifecycleOwner,
                                onRendererActivated = { },
                                onFrameDrawn = { count ->
                                    if (count >= 10 && !cameraRendererActivated) {
                                        cameraRendererActivated = true
                                        onPreviewModeChanged("ARCORE")
                                    }
                                    frameCount = count
                                },
                                onError = { msg ->
                                    lastErrorMessage = msg
                                    lastErrorTimestamp = System.currentTimeMillis()
                                    onError(msg)
                                }
                            )
                        }
                    }
                }
            )
        }


        // --- Detected Image Overlay ---
        if (currentlyTrackedPosterId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .background(
                        color = Color.Green.copy(alpha = 0.8f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Image Detected!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

    }
}

@Composable
private fun BasicCameraPreview(
    modifier: Modifier,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageMatcher: ImageMatcherService,
    isDetectionLocked: Boolean,
    onPosterDetected: (String, String) -> Unit,
    onPreviewReady: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder()
                        .setTargetRotation(android.view.Surface.ROTATION_0)
                        .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    var lastCheckTime = 0L
                    val checkIntervalMs = 1000L // Analyze 1 frame per second
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()
                        
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        if (isDetectionLocked) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastCheckTime < checkIntervalMs) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastCheckTime = currentTime
                        
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val rawBitmap = try {
                            imageProxy.toBitmap()
                        } catch (e: Exception) {
                            Log.e("BasicCameraPreview", "Failed to convert image proxy to bitmap", e)
                            null
                        }
                        
                        if (rawBitmap != null) {
                            scope.launch(Dispatchers.Default) {
                                val rotatedBitmap = rotateBitmapHelper(rawBitmap, rotationDegrees)
                                val match = imageMatcher.matchFrame(rotatedBitmap)
                                rotatedBitmap.recycle()
                                if (match != null) {
                                    withContext(Dispatchers.Main) {
                                        onPosterDetected(match.imageId, match.imageName)
                                    }
                                }
                            }
                        }
                        imageProxy.close()
                    }
                    
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    onPreviewReady()
                    Log.d("ArSceneView", "✅ CameraX fallback preview and analyzer started")
                } catch (e: Exception) {
                    Log.e("ArSceneView", "Failed to start CameraX fallback preview", e)
                    onError("Camera preview unavailable: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun rotateBitmapHelper(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = android.graphics.Matrix()
    matrix.postRotate(degrees.toFloat())
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}


/**
 * Delegating renderer that switches from placeholder to camera rendering.
 * This avoids calling setRenderer() twice which causes IllegalStateException.
 */
private class ArSceneDelegatingRenderer : android.opengl.GLSurfaceView.Renderer {
    @Volatile
    private var delegateRenderer: android.opengl.GLSurfaceView.Renderer? = null
    
    private var surfaceCreatedCalled = false
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastGl: javax.microedition.khronos.opengles.GL10? = null
    private var lastConfig: javax.microedition.khronos.egl.EGLConfig? = null
    
    fun updateDelegate(newDelegate: android.opengl.GLSurfaceView.Renderer) {
        delegateRenderer = newDelegate
        
        // Immediately notify the new delegate of existing surface state if it was already created/changed
        if (surfaceCreatedCalled) {
            Log.d("ArSceneView", "ArSceneDelegatingRenderer: Applying cached onSurfaceCreated to new delegate")
            newDelegate.onSurfaceCreated(lastGl, lastConfig)
        }
        if (lastWidth > 0 && lastHeight > 0) {
            Log.d("ArSceneView", "ArSceneDelegatingRenderer: Applying cached onSurfaceChanged (${lastWidth}x${lastHeight}) to new delegate")
            newDelegate.onSurfaceChanged(lastGl, lastWidth, lastHeight)
        }
    }
    
    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        Log.d("ArSceneView", "ArSceneDelegatingRenderer: onSurfaceCreated")
        surfaceCreatedCalled = true
        lastGl = gl
        lastConfig = config
        
        delegateRenderer?.onSurfaceCreated(gl, config) ?: run {
            // Placeholder rendering
            android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            Log.d("ArSceneView", "Placeholder renderer: Surface created")
        }
    }
    
    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        Log.d("ArSceneView", "ArSceneDelegatingRenderer: onSurfaceChanged ${width}x${height}")
        lastWidth = width
        lastHeight = height
        lastGl = gl
        
        delegateRenderer?.onSurfaceChanged(gl, width, height) ?: run {
            // Placeholder rendering
            android.opengl.GLES20.glViewport(0, 0, width, height)
            Log.d("ArSceneView", "Placeholder renderer: Surface changed ${width}x${height}")
        }
    }
    
    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        delegateRenderer?.onDrawFrame(gl) ?: run {
            // Placeholder rendering
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
        }
    }
}

/**
 * Creates the AR camera view with ARCore rendering.
 * Uses a delegating renderer that can switch from placeholder to camera rendering.
 */
private fun createARCameraView(
    context: Context
): android.view.View {
    val layout = FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.BLACK)
    }
    
    // Create delegating renderer that we can reconfigure later
    val delegatingRenderer = ArSceneDelegatingRenderer()
    
    // Create GLSurfaceView for AR camera preview with OpenGL rendering
    val glSurfaceView = android.opengl.GLSurfaceView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        preserveEGLContextOnPause = true
        setEGLContextClientVersion(2) // OpenGL ES 2.0
        setEGLConfigChooser(8, 8, 8, 8, 16, 0) // RGBA_8888, 16-bit depth
        
        // Set the delegating renderer (only called once!)
        setRenderer(delegatingRenderer)
        
        // Use CONTINUOUSLY mode for AR tracking
        renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }
    
    layout.addView(glSurfaceView)
    
    // Store both GL surface view and delegating renderer in layout tag for later access
    layout.tag = Pair(glSurfaceView, delegatingRenderer)
    
    Log.d("ArSceneView", "✅ GLSurfaceView created with delegating renderer")
    return layout
}

/**
 * Set up camera renderer for ARCore frames.
 * Called from AndroidView update block when session is ready.
 * Updates the delegating renderer to use camera rendering instead of placeholder.
 */
private fun setupCameraRenderer(
    view: android.view.View,
    sessionProvider: () -> Session?,
    trackingManagerProvider: () -> ARTrackingManager?,
    shouldProcessTrackingProvider: () -> Boolean,
    isSessionRunningProvider: () -> Boolean,
    latestViewMatrix: FloatArray,
    latestProjectionMatrix: FloatArray,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onRendererActivated: () -> Unit,
    onFrameDrawn: (Int) -> Unit = {},
    onError: (String) -> Unit
) {
    // Get the GL surface view and delegating renderer from layout
    val layout = view as? FrameLayout ?: return
    val tagPair = layout.tag as? Pair<*, *> ?: return
    val glSurfaceView = tagPair.first as? android.opengl.GLSurfaceView ?: return
    val delegatingRenderer = tagPair.second as? ArSceneDelegatingRenderer ?: return
    
    Log.d("ArSceneView", "Setting up camera renderer with ARCore session")
    
    // Create a renderer for ARCore camera feed with background rendering
    val cameraRenderer = object : android.opengl.GLSurfaceView.Renderer {
        private var cameraTextureConfigured = false
        private var cameraTextureId = -1
        private var shaderProgram = 0
        private var positionAttrib = 0
        private var texCoordAttrib = 0
        private var textureUniform = 0
        private var frameCounter = 0
        private val quadCoords = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
        // ARCore transformDisplayUvCoords expects (0,0) at top-left (bitmap convention).
        // V is therefore flipped vs the OpenGL "v=0 is bottom" convention.
        private val quadTexCoords = floatArrayOf(
            0.0f, 1.0f,  // NDC bottom-left  → camera bottom-left
            1.0f, 1.0f,  // NDC bottom-right → camera bottom-right
            0.0f, 0.0f,  // NDC top-left     → camera top-left
            1.0f, 0.0f   // NDC top-right    → camera top-right
        )
        private val transformedTexCoords = FloatArray(8)
        private val vertexBuffer = java.nio.ByteBuffer.allocateDirect(quadCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(quadCoords)
                position(0)
            }
        private val inputUvBuffer = java.nio.ByteBuffer.allocateDirect(quadTexCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(quadTexCoords)
                position(0)
            }
        private val outputUvBuffer = java.nio.ByteBuffer.allocateDirect(transformedTexCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        private val texCoordBuffer = java.nio.ByteBuffer.allocateDirect(transformedTexCoords.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        
        // Vertex shader for camera background
        private val vertexShaderCode = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()
        
        // Fragment shader for external OES texture (ARCore camera)
        private val fragmentShaderCode = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES u_Texture;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """.trimIndent()
        
        override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
            Log.d("ArSceneView", "Camera renderer: GL Surface created")
            try {
                // Set clear color
                android.opengl.GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
                
                // Generate texture for camera
                val textures = IntArray(1)
                android.opengl.GLES20.glGenTextures(1, textures, 0)
                cameraTextureId = textures[0]
                
                // Bind and configure texture
                android.opengl.GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
                android.opengl.GLES20.glTexParameteri(
                    android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    android.opengl.GLES20.GL_TEXTURE_MIN_FILTER,
                    android.opengl.GLES20.GL_LINEAR
                )
                android.opengl.GLES20.glTexParameteri(
                    android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    android.opengl.GLES20.GL_TEXTURE_MAG_FILTER,
                    android.opengl.GLES20.GL_LINEAR
                )
                
                // Configure ARCore to use the camera texture
                if (!cameraTextureConfigured && cameraTextureId != -1) {
                    val currentSession = sessionProvider() ?: return
                    currentSession.setCameraTextureName(cameraTextureId)
                    cameraTextureConfigured = true
                    Log.d("ArSceneView", "✅ ARCore camera texture configured: $cameraTextureId")
                }
                
                // Compile shaders with error checking
                val vertexShader = loadShader(android.opengl.GLES20.GL_VERTEX_SHADER, vertexShaderCode)
                val fragmentShader = loadShader(android.opengl.GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
                
                if (vertexShader == 0 || fragmentShader == 0) {
                    Log.e("ArSceneView", "❌ Shader compilation failed!")
                    return
                }
                
                // Create shader program
                shaderProgram = android.opengl.GLES20.glCreateProgram()
                android.opengl.GLES20.glAttachShader(shaderProgram, vertexShader)
                android.opengl.GLES20.glAttachShader(shaderProgram, fragmentShader)
                android.opengl.GLES20.glLinkProgram(shaderProgram)
                
                // Check linking
                val linkStatus = IntArray(1)
                android.opengl.GLES20.glGetProgramiv(shaderProgram, android.opengl.GLES20.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] == 0) {
                    Log.e("ArSceneView", "❌ Shader linking failed: ${android.opengl.GLES20.glGetProgramInfoLog(shaderProgram)}")
                    return
                }
                
                // Get attribute and uniform locations
                positionAttrib = android.opengl.GLES20.glGetAttribLocation(shaderProgram, "a_Position")
                texCoordAttrib = android.opengl.GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord")
                textureUniform = android.opengl.GLES20.glGetUniformLocation(shaderProgram, "u_Texture")
                
                Log.d("ArSceneView", "✅ Camera renderer initialized with shaders")
            } catch (e: Exception) {
                Log.e("ArSceneView", "Failed to initialize camera renderer", e)
                glSurfaceView.post { onError("GL Init Error: ${e.message}") }
            }
        }

        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = android.opengl.GLES20.glCreateShader(type)
            android.opengl.GLES20.glShaderSource(shader, shaderCode)
            android.opengl.GLES20.glCompileShader(shader)
            
            // Check compilation status
            val compileStatus = IntArray(1)
            android.opengl.GLES20.glGetShaderiv(shader, android.opengl.GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                Log.e("ArSceneView", "❌ Shader compilation error: ${android.opengl.GLES20.glGetShaderInfoLog(shader)}")
                android.opengl.GLES20.glDeleteShader(shader)
                return 0
            }
            return shader
        }
        
        private var viewportWidth = 0
        private var viewportHeight = 0
        private var viewportChanged = false

        override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
            viewportWidth = width
            viewportHeight = height
            viewportChanged = true
            
            Log.d("ArSceneView", "Camera renderer: GL Surface changed: ${width}x${height}")
            android.opengl.GLES20.glViewport(0, 0, width, height)
            
            try {
                val windowManager = glSurfaceView.context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
                val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    glSurfaceView.context.display?.rotation ?: windowManager.defaultDisplay.rotation
                } else {
                    @Suppress("DEPRECATION")
                    windowManager.defaultDisplay.rotation
                }
                
                val currentSession = sessionProvider() ?: return
                currentSession.setDisplayGeometry(
                    rotation,
                    width,
                    height
                )
                Log.d("ArSceneView", "setDisplayGeometry: rotation=$rotation viewport=${width}x${height}")
            } catch (e: Exception) {
                Log.e("ArSceneView", "Error setting display geometry", e)
            }
        }
        
        override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
            // Clear screen
            android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT or android.opengl.GLES20.GL_DEPTH_BUFFER_BIT)
            
            try {
                // Check lifecycle state
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    return
                }

                // Check ARCore session state
                val currentSession = sessionProvider() ?: return
                try {
                    currentSession.cameraConfig
                } catch (e: Exception) {
                    return
                }
                
                // Update ARCore frame with error handling for timestamp issues
                val frame = try {
                    if (!isSessionRunningProvider()) {
                        return
                    }
                    currentSession.update()
                } catch (e: SessionPausedException) {
                    // Session paused between lifecycle transitions; skip this frame safely.
                    return
                } catch (e: IllegalArgumentException) {
                    // Handle sensor timestamp errors gracefully
                    if (e.message?.contains("timestamp") == true || e.message?.contains("monotonic") == true) {
                        // Skip this frame and continue - ARCore will recover
                        return
                    }
                    throw e
                } catch (e: Exception) {
                    // Handle resource exhaustion
                    if (e.message?.contains("RESOURCE_EXHAUSTED") == true) {
                        // Skip this frame - system is overloaded
                        return
                    }
                    throw e
                }
                
                val camera = frame.camera

                // Always draw camera background; tracking may be PAUSED initially.
                drawCameraBackground(frame)

                // Update matrices BEFORE processFrame so tracking callbacks receive
                // same-frame matrices (avoids one-frame lag that shifts the overlay).
                if (camera.trackingState == TrackingState.TRACKING) {
                    camera.getViewMatrix(latestViewMatrix, 0)
                    camera.getProjectionMatrix(latestProjectionMatrix, 0, 0.1f, 100f)
                }

                if (shouldProcessTrackingProvider()) {
                    trackingManagerProvider()?.processFrame(frame)
                }
                
                // Signal successful frame draw
                frameCounter++
                if (frameCounter % 60 == 0) {
                    Log.d("ArSceneView", "📊 AR Frame drawn: $frameCounter")
                }
                glSurfaceView.post { onFrameDrawn(frameCounter) }
                
            } catch (e: Exception) {
                val isSessionPaused = e is SessionPausedException || 
                                    e.javaClass.simpleName.contains("SessionPaused") || 
                                    e.message?.contains("SessionPaused") == true
                
                if (!isSessionPaused && e !is CameraNotAvailableException) {
                    Log.e("ArSceneView", "Error in draw frame: ${e.message}")
                }
            }
        }
        
        private fun drawCameraBackground(frame: Frame) {
            if (cameraTextureId == -1 || shaderProgram == 0) return
            
            try {
                // Use shader program
                android.opengl.GLES20.glUseProgram(shaderProgram)
                
                // Bind camera texture
                android.opengl.GLES20.glActiveTexture(android.opengl.GLES20.GL_TEXTURE0)
                android.opengl.GLES20.glBindTexture(android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
                android.opengl.GLES20.glUniform1i(textureUniform, 0)
                
                // Transform texture coordinates for ARCore (correct buffer flow).
                inputUvBuffer.position(0)
                outputUvBuffer.position(0)
                frame.transformDisplayUvCoords(
                    inputUvBuffer,
                    outputUvBuffer
                )
                outputUvBuffer.position(0)
                outputUvBuffer.get(transformedTexCoords)
                vertexBuffer.position(0)
                
                android.opengl.GLES20.glVertexAttribPointer(
                    positionAttrib, 2, android.opengl.GLES20.GL_FLOAT, false, 0, vertexBuffer
                )
                android.opengl.GLES20.glEnableVertexAttribArray(positionAttrib)
                
                // Set texture coordinates
                texCoordBuffer.position(0)
                texCoordBuffer.put(transformedTexCoords)
                texCoordBuffer.position(0)
                
                android.opengl.GLES20.glVertexAttribPointer(
                    texCoordAttrib, 2, android.opengl.GLES20.GL_FLOAT, false, 0, texCoordBuffer
                )
                android.opengl.GLES20.glEnableVertexAttribArray(texCoordAttrib)
                
                // Draw quad
                android.opengl.GLES20.glDrawArrays(android.opengl.GLES20.GL_TRIANGLE_STRIP, 0, 4)
                
                // Cleanup
                android.opengl.GLES20.glDisableVertexAttribArray(positionAttrib)
                android.opengl.GLES20.glDisableVertexAttribArray(texCoordAttrib)
                
            } catch (e: Exception) {
                Log.e("ArSceneView", "Error drawing camera background", e)
            }
        }
    }
    
    // Update the delegating renderer to use the camera renderer
    // This doesn't call setRenderer() again, just updates the delegate
    try {
        Log.d("ArSceneView", "Switching delegating renderer to camera renderer")
        Log.d("ArSceneView", "Setting up camera renderer on GL thread")
        glSurfaceView.queueEvent {
            Log.d("ArSceneView", "GL Thread: Updating renderer delegate")
            delegatingRenderer.updateDelegate(cameraRenderer)
            glSurfaceView.post { 
                Log.d("ArSceneView", "Main Thread: Renderer activated callback")
                onRendererActivated() 
            }
        }
        Log.d("ArSceneView", "✅ Camera renderer activated, camera feed should now be visible")
    } catch (e: Exception) {
        Log.e("ArSceneView", "❌ Error activating camera renderer", e)
        onError("Failed to activate camera renderer: ${e.message}")
    }
}

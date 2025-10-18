package com.talkar.app.ar

import android.content.Context
import android.util.Log
import com.google.ar.core.*
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.talkar.app.data.models.AvatarModel3D
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Avatar 3D Renderer
 * 
 * Handles the rendering of 3D avatars in the AR scene.
 * Manages ARCore session, frame updates, and scene lifecycle.
 */
class Avatar3DRenderer(
    context: Context,
    private val arSceneView: ArSceneView
) {
    private val TAG = "Avatar3DRenderer"
    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Avatar manager instance
    private lateinit var avatarManager: AvatarManager
    
    // ARCore session
    private var arSession: Session? = null
    
    // Scene reference
    private val scene: Scene = arSceneView.scene
    
    // Current tracked augmented images
    private val trackedImages = mutableMapOf<String, AugmentedImage>()
    
    // Rendering state
    private val _renderingState = MutableStateFlow<RenderingState>(RenderingState.NotStarted)
    val renderingState: StateFlow<RenderingState> = _renderingState.asStateFlow()
    
    // Frame update callback
    private var frameUpdateListener: Scene.OnUpdateListener? = null
    
    init {
        Log.d(TAG, "Avatar3DRenderer initialized")
        initializeAvatarManager()
    }
    
    /**
     * Initialize avatar manager
     */
    private fun initializeAvatarManager() {
        val context = contextRef.get() ?: return
        avatarManager = AvatarManager(context)
        Log.d(TAG, "AvatarManager initialized")
    }
    
    /**
     * Setup ARCore session with image tracking
     */
    fun setupSession(session: Session, imageDatabase: AugmentedImageDatabase) {
        try {
            arSession = session
            
            // Configure session for image tracking
            val config = Config(session).apply {
                augmentedImageDatabase = imageDatabase
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
            }
            
            session.configure(config)
            arSceneView.setupSession(session)
            
            _renderingState.value = RenderingState.Ready
            Log.d(TAG, "ARCore session configured with image database")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup ARCore session", e)
            _renderingState.value = RenderingState.Error(e.message ?: "Session setup failed")
        }
    }
    
    /**
     * Start rendering avatars
     */
    fun startRendering() {
        if (_renderingState.value !is RenderingState.Ready) {
            Log.w(TAG, "Cannot start rendering - session not ready")
            return
        }
        
        try {
            // Add frame update listener
            frameUpdateListener = Scene.OnUpdateListener { frameTime ->
                onFrameUpdate(frameTime)
            }
            scene.addOnUpdateListener(frameUpdateListener)
            
            _renderingState.value = RenderingState.Rendering
            Log.d(TAG, "Started rendering avatars")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start rendering", e)
            _renderingState.value = RenderingState.Error(e.message ?: "Rendering start failed")
        }
    }
    
    /**
     * Stop rendering avatars
     */
    fun stopRendering() {
        frameUpdateListener?.let {
            scene.removeOnUpdateListener(it)
        }
        frameUpdateListener = null
        
        _renderingState.value = RenderingState.Stopped
        Log.d(TAG, "Stopped rendering avatars")
    }
    
    /**
     * Frame update callback
     * Called every frame to process AR tracking updates
     */
    private fun onFrameUpdate(frameTime: com.google.ar.sceneform.FrameTime) {
        val session = arSession ?: return
        
        try {
            val frame = session.update()
            
            // Process tracked images
            val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
            
            for (augmentedImage in updatedAugmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.TRACKING -> {
                        handleImageTracking(augmentedImage)
                    }
                    TrackingState.PAUSED -> {
                        handleImagePaused(augmentedImage)
                    }
                    TrackingState.STOPPED -> {
                        handleImageStopped(augmentedImage)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in frame update", e)
        }
    }
    
    /**
     * Handle image tracking detected
     */
    private fun handleImageTracking(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name
        val imageIndex = augmentedImage.index
        
        // Create unique image ID
        val imageId = "$imageName-$imageIndex"
        
        // Check if already being tracked
        if (trackedImages.containsKey(imageId)) {
            // Update existing tracking
            trackedImages[imageId] = augmentedImage
            return
        }
        
        // New image detected
        Log.d(TAG, "New image detected: $imageName (index: $imageIndex)")
        trackedImages[imageId] = augmentedImage
        
        // Load and display avatar for this image
        scope.launch {
            try {
                val anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
                
                avatarManager.loadAvatarForImage(
                    imageId = imageId,
                    anchor = anchor,
                    onLoaded = { avatarNode ->
                        Log.d(TAG, "✅ Avatar loaded and anchored for: $imageName")
                    },
                    onError = { error ->
                        Log.e(TAG, "Failed to load avatar for $imageName: ${error.message}")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating anchor for image: $imageName", e)
            }
        }
    }
    
    /**
     * Handle image tracking paused
     */
    private fun handleImagePaused(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name
        val imageIndex = augmentedImage.index
        val imageId = "$imageName-$imageIndex"
        
        Log.d(TAG, "Image tracking paused: $imageName")
        
        // Keep the avatar but reduce opacity or pause animations
        avatarManager.getActiveAvatarNode(imageId)?.let { node ->
            node.stopAnimations()
        }
    }
    
    /**
     * Handle image tracking stopped
     */
    private fun handleImageStopped(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name
        val imageIndex = augmentedImage.index
        val imageId = "$imageName-$imageIndex"
        
        Log.d(TAG, "Image tracking stopped: $imageName")
        
        // Remove avatar from scene
        trackedImages.remove(imageId)
        avatarManager.removeAvatar(imageId)
    }
    
    /**
     * Manually load avatar for a specific image ID
     * Useful for testing or manual triggers
     */
    fun loadAvatarForImage(imageId: String, anchor: Anchor) {
        scope.launch {
            avatarManager.loadAvatarForImage(
                imageId = imageId,
                anchor = anchor,
                onLoaded = { avatarNode ->
                    Log.d(TAG, "Avatar loaded for: $imageId")
                },
                onError = { error ->
                    Log.e(TAG, "Failed to load avatar: ${error.message}")
                }
            )
        }
    }
    
    /**
     * Register avatar configurations
     */
    fun registerAvatar(avatar: AvatarModel3D) {
        avatarManager.registerAvatar(avatar)
    }
    
    /**
     * Register multiple avatars
     */
    fun registerAvatars(avatars: List<AvatarModel3D>) {
        avatarManager.registerAvatars(avatars)
    }
    
    /**
     * Map image to avatar
     */
    fun mapImageToAvatar(imageId: String, avatarId: String) {
        avatarManager.mapImageToAvatar(imageId, avatarId)
    }
    
    /**
     * Get avatar manager instance
     */
    fun getAvatarManager(): AvatarManager {
        return avatarManager
    }
    
    /**
     * Remove all avatars from scene
     */
    fun removeAllAvatars() {
        avatarManager.removeAllAvatars()
        trackedImages.clear()
    }
    
    /**
     * Pause rendering
     */
    fun pause() {
        stopRendering()
        Log.d(TAG, "Renderer paused")
    }
    
    /**
     * Resume rendering
     */
    fun resume() {
        if (_renderingState.value is RenderingState.Ready || 
            _renderingState.value is RenderingState.Stopped) {
            startRendering()
        }
        Log.d(TAG, "Renderer resumed")
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopRendering()
        avatarManager.cleanup()
        arSession?.close()
        arSession = null
        scope.cancel()
        Log.d(TAG, "Avatar3DRenderer cleaned up")
    }
}

/**
 * Rendering states
 */
sealed class RenderingState {
    object NotStarted : RenderingState()
    object Ready : RenderingState()
    object Rendering : RenderingState()
    object Stopped : RenderingState()
    data class Error(val message: String) : RenderingState()
}

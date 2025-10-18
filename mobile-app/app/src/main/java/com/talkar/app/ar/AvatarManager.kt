package com.talkar.app.ar

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.talkar.app.data.models.AvatarModel3D
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.IdleAnimation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Avatar Manager Module
 * 
 * Manages loading, caching, and lifecycle of 3D avatars in AR scene.
 * Dynamically loads avatars based on detected image IDs.
 */
class AvatarManager(context: Context) {
    
    private val TAG = "AvatarManager"
    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Avatar registry: maps avatar ID to AvatarModel3D
    private val avatarRegistry = mutableMapOf<String, AvatarModel3D>()
    
    // Image-to-Avatar mapping: which avatar to load for which image
    private val imageAvatarMappings = mutableMapOf<String, String>()
    
    // Loaded renderable cache: avoid reloading same models
    private val renderableCache = mutableMapOf<String, ModelRenderable>()
    
    // Active avatar nodes in the scene
    private val activeAvatarNodes = mutableMapOf<String, AvatarNode>()
    
    // Pose tracking and stability components
    private val poseTrackers = mutableMapOf<String, PoseTracker>()
    private val depthController = DepthController()
    private val anchorStabilizer = AnchorStabilizer()
    private val transitionManager = TransitionManager()
    
    // Loading states
    private val _loadingState = MutableStateFlow<AvatarLoadingState>(AvatarLoadingState.Idle)
    val loadingState: StateFlow<AvatarLoadingState> = _loadingState.asStateFlow()
    
    // Currently loaded avatar
    private val _currentAvatar = MutableStateFlow<AvatarModel3D?>(null)
    val currentAvatar: StateFlow<AvatarModel3D?> = _currentAvatar.asStateFlow()
    
    init {
        Log.d(TAG, "AvatarManager initialized")
        // Initialize with default avatars (will be populated from backend/config)
        initializeDefaultAvatars()
    }
    
    /**
     * Initialize default avatar configurations
     * This can be replaced with backend-driven configuration
     */
    private fun initializeDefaultAvatars() {
        // TODO: Load from backend or local config
        // For now, define placeholder avatars
        
        val defaultAvatar = AvatarModel3D(
            id = "avatar_default_1",
            name = "Generic Presenter",
            description = "Default professional presenter avatar",
            modelUrl = null, // Will be loaded from res/raw or backend
            scale = 1.0f,
            idleAnimation = IdleAnimation.BREATHING_AND_BLINKING
        )
        
        registerAvatar(defaultAvatar)
        
        Log.d(TAG, "Initialized ${avatarRegistry.size} default avatars")
    }
    
    /**
     * Register a new avatar in the system
     */
    fun registerAvatar(avatar: AvatarModel3D) {
        avatarRegistry[avatar.id] = avatar
        
        // Register image mappings
        avatar.mappedImageIds.forEach { imageId ->
            imageAvatarMappings[imageId] = avatar.id
        }
        
        Log.d(TAG, "Registered avatar: ${avatar.name} (${avatar.id})")
    }
    
    /**
     * Register multiple avatars at once
     */
    fun registerAvatars(avatars: List<AvatarModel3D>) {
        avatars.forEach { registerAvatar(it) }
    }
    
    /**
     * Map an image ID to an avatar ID
     */
    fun mapImageToAvatar(imageId: String, avatarId: String) {
        imageAvatarMappings[imageId] = avatarId
        Log.d(TAG, "Mapped image $imageId to avatar $avatarId")
    }
    
    /**
     * Get avatar for a detected image
     */
    fun getAvatarForImage(imageId: String): AvatarModel3D? {
        val avatarId = imageAvatarMappings[imageId]
        return if (avatarId != null) {
            avatarRegistry[avatarId]
        } else {
            Log.w(TAG, "No avatar mapped for image: $imageId")
            // Return default avatar as fallback
            avatarRegistry["avatar_default_1"]
        }
    }
    
    /**
     * Load and display avatar for a detected image
     */
    suspend fun loadAvatarForImage(
        imageId: String,
        anchor: Anchor,
        onLoaded: (AvatarNode) -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                _loadingState.value = AvatarLoadingState.Loading(imageId)
                Log.d(TAG, "Loading avatar for image: $imageId")
                
                val avatar = getAvatarForImage(imageId)
                if (avatar == null) {
                    val error = Exception("No avatar found for image: $imageId")
                    _loadingState.value = AvatarLoadingState.Error(error.message ?: "Unknown error")
                    onError(error)
                    return@withContext
                }
                
                _currentAvatar.value = avatar
                
                // Load the 3D model renderable
                val renderable = loadRenderable(avatar)
                
                // Create avatar node and attach to anchor
                val avatarNode = createAvatarNode(avatar, anchor, renderable)
                
                // Store active node
                activeAvatarNodes[imageId] = avatarNode
                
                // Apply idle animation
                applyIdleAnimation(avatarNode, avatar.idleAnimation)
                
                _loadingState.value = AvatarLoadingState.Loaded(avatar)
                onLoaded(avatarNode)
                
                Log.d(TAG, "✅ Avatar loaded successfully: ${avatar.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load avatar for image: $imageId", e)
                _loadingState.value = AvatarLoadingState.Error(e.message ?: "Unknown error")
                onError(e)
            }
        }
    }
    
    /**
     * Load renderable for an avatar (with caching)
     */
    private suspend fun loadRenderable(avatar: AvatarModel3D): ModelRenderable {
        // Check cache first
        renderableCache[avatar.id]?.let {
            Log.d(TAG, "Using cached renderable for: ${avatar.name}")
            return it
        }
        
        // Load from resource or URL
        return withContext(Dispatchers.IO) {
            val context = contextRef.get() ?: throw IllegalStateException("Context is null")
            
            try {
                val renderable = when {
                    avatar.modelResourceId != null -> {
                        // Load from res/raw
                        loadRenderableFromResource(context, avatar.modelResourceId)
                    }
                    avatar.modelUrl != null -> {
                        // Load from remote URL
                        loadRenderableFromUrl(context, avatar.modelUrl)
                    }
                    else -> {
                        throw IllegalArgumentException("Avatar ${avatar.id} has no model source")
                    }
                }
                
                // Cache the loaded renderable
                renderableCache[avatar.id] = renderable
                
                renderable
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load renderable for avatar: ${avatar.name}", e)
                throw e
            }
        }
    }
    
    /**
     * Load renderable from res/raw resource
     */
    private suspend fun loadRenderableFromResource(
        context: Context,
        resourceId: Int
    ): ModelRenderable {
        return suspendCancellableCoroutine { continuation ->
            ModelRenderable.builder()
                .setSource(context, resourceId)
                .build()
                .thenAccept { renderable ->
                    if (continuation.isActive) {
                        continuation.resume(renderable, null)
                    }
                }
                .exceptionally { throwable ->
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(throwable))
                    }
                    null
                }
        }
    }
    
    /**
     * Load renderable from remote URL
     */
    private suspend fun loadRenderableFromUrl(
        context: Context,
        url: String
    ): ModelRenderable {
        return suspendCancellableCoroutine { continuation ->
            ModelRenderable.builder()
                .setSource(context, Uri.parse(url))
                .build()
                .thenAccept { renderable ->
                    if (continuation.isActive) {
                        continuation.resume(renderable, null)
                    }
                }
                .exceptionally { throwable ->
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(throwable))
                    }
                    null
                }
        }
    }
    
    /**
     * Create avatar node and attach to anchor
     */
    private fun createAvatarNode(
        avatar: AvatarModel3D,
        anchor: Anchor,
        renderable: ModelRenderable
    ): AvatarNode {
        val anchorNode = AnchorNode(anchor)
        
        // Calculate Z-offset to keep avatar above poster
        val distance = depthController.calculateDistance(anchor.pose)
        val zOffset = depthController.calculateZOffset(avatar.scale, distance)
        
        // Apply Z-offset to anchor pose
        val offsetPose = depthController.applyZOffset(anchor.pose, zOffset)
        
        val avatarNode = AvatarNode(avatar).apply {
            setParent(anchorNode)
            this.renderable = renderable
            
            // Apply scale
            localScale = com.google.ar.sceneform.math.Vector3(
                avatar.scale,
                avatar.scale,
                avatar.scale
            )
            
            // Apply position offset (including Z-offset)
            localPosition = com.google.ar.sceneform.math.Vector3(
                avatar.positionOffset.x,
                avatar.positionOffset.y,
                avatar.positionOffset.z + zOffset
            )
            
            // Apply rotation offset
            localRotation = com.google.ar.sceneform.math.Quaternion.eulerAngles(
                com.google.ar.sceneform.math.Vector3(
                    avatar.rotationOffset.x,
                    avatar.rotationOffset.y,
                    avatar.rotationOffset.z
                )
            )
        }
        
        return avatarNode
    }
    
    /**
     * Apply idle animation to avatar node
     */
    private fun applyIdleAnimation(avatarNode: AvatarNode, animationType: IdleAnimation) {
        when (animationType) {
            IdleAnimation.BREATHING -> {
                avatarNode.startBreathingAnimation()
            }
            IdleAnimation.BLINKING -> {
                avatarNode.startBlinkingAnimation()
            }
            IdleAnimation.BREATHING_AND_BLINKING -> {
                avatarNode.startBreathingAnimation()
                avatarNode.startBlinkingAnimation()
            }
            IdleAnimation.CUSTOM -> {
                avatarNode.playEmbeddedAnimation()
            }
            IdleAnimation.NONE -> {
                // No idle animation
            }
        }
    }
    
    /**
     * Remove avatar for a specific image
     */
    fun removeAvatar(imageId: String) {
        activeAvatarNodes[imageId]?.let { node ->
            node.setParent(null)
            node.renderable = null
            activeAvatarNodes.remove(imageId)
            
            // Remove pose tracker
            poseTrackers.remove(imageId)
            
            Log.d(TAG, "Removed avatar for image: $imageId")
        }
    }
    
    /**
     * Update avatar pose with tracking data
     * Should be called every frame for smooth tracking
     */
    fun updateAvatarPose(
        imageId: String,
        rawPose: com.google.ar.core.Pose,
        trackingState: com.google.ar.core.TrackingState,
        trackingConfidence: Float = 1.0f
    ) {
        val avatarNode = activeAvatarNodes[imageId] ?: return
        
        // Get or create pose tracker for this avatar
        val poseTracker = poseTrackers.getOrPut(imageId) {
            PoseTracker()
        }
        
        // Update pose with smoothing
        val smoothedPose = poseTracker.updatePose(rawPose, trackingState, trackingConfidence)
        
        // Calculate Z-offset
        val distance = depthController.calculateDistance(smoothedPose)
        val zOffset = depthController.calculateZOffset(avatarNode.avatarModel.scale, distance)
        
        // Apply Z-offset
        val finalPose = depthController.applyZOffset(smoothedPose, zOffset)
        
        // Update avatar node position (if using custom positioning)
        // Note: In Sceneform, the anchor handles positioning automatically
        // This is for additional adjustments
        
        // Update opacity based on tracking confidence
        val alpha = transitionManager.updateTransition(trackingConfidence, 0.016f)
        avatarNode.updateOpacity(alpha)
        
        // Log stability
        if (poseTracker.isStable.value) {
            android.util.Log.d(TAG, "Avatar $imageId is stable (confidence: $trackingConfidence)")
        }
    }
    
    /**
     * Remove all active avatars from the scene
     */
    fun removeAllAvatars() {
        activeAvatarNodes.forEach { (imageId, node) ->
            node.setParent(null)
            node.renderable = null
        }
        activeAvatarNodes.clear()
        Log.d(TAG, "Removed all avatars from scene")
    }
    
    /**
     * Get active avatar node for an image
     */
    fun getActiveAvatarNode(imageId: String): AvatarNode? {
        return activeAvatarNodes[imageId]
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        removeAllAvatars()
        renderableCache.clear()
        Log.d(TAG, "AvatarManager cleaned up")
    }
}

/**
 * Custom avatar node with animation support
 */
class AvatarNode(val avatarModel: AvatarModel3D) : Node() {
    
    private val TAG = "AvatarNode"
    private var breathingJob: Job? = null
    private var blinkingJob: Job? = null
    private var currentOpacity = 1.0f
    
    /**
     * Update opacity for confidence-based fading
     */
    fun updateOpacity(opacity: Float) {
        currentOpacity = opacity.coerceIn(0f, 1f)
        
        // Update renderable material alpha
        renderable?.let { r ->
            r.material.setFloat3("baseColorFactor", 1f, 1f, 1f)
            r.material.setFloat("metallicFactor", currentOpacity)
        }
    }
    
    /**
     * Start breathing animation (subtle scale oscillation)
     */
    fun startBreathingAnimation() {
        breathingJob?.cancel()
        breathingJob = CoroutineScope(Dispatchers.Main).launch {
            var time = 0f
            while (isActive) {
                time += 0.016f // ~60 FPS
                
                // Breathing frequency: 0.3 Hz (18 breaths per minute)
                val breathScale = 1.0f + 0.02f * kotlin.math.sin(time * 2 * Math.PI.toFloat() * 0.3f)
                
                localScale = com.google.ar.sceneform.math.Vector3(
                    avatarModel.scale * breathScale,
                    avatarModel.scale * breathScale,
                    avatarModel.scale * breathScale
                )
                
                delay(16) // ~60 FPS
            }
        }
    }
    
    /**
     * Start blinking animation
     * Note: This is a simplified version. Real blinking requires morph targets
     */
    fun startBlinkingAnimation() {
        blinkingJob?.cancel()
        blinkingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                // Random blink interval: 2-6 seconds
                delay((2000..6000).random().toLong())
                
                // TODO: Implement actual eye blink using morph targets
                // For now, just log
                Log.d(TAG, "Blink animation triggered")
            }
        }
    }
    
    /**
     * Play embedded animation from the GLB model
     */
    fun playEmbeddedAnimation() {
        // TODO: Implement when Sceneform animation API is available
        Log.d(TAG, "Playing embedded animation (not yet implemented)")
    }
    
    /**
     * Stop all animations
     */
    fun stopAnimations() {
        breathingJob?.cancel()
        blinkingJob?.cancel()
    }
    
    override fun onDeactivate() {
        super.onDeactivate()
        stopAnimations()
    }
}

/**
 * Avatar loading states
 */
sealed class AvatarLoadingState {
    object Idle : AvatarLoadingState()
    data class Loading(val imageId: String) : AvatarLoadingState()
    data class Loaded(val avatar: AvatarModel3D) : AvatarLoadingState()
    data class Error(val message: String) : AvatarLoadingState()
}

package com.talkar.app.ar

import android.util.Log
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.AugmentedImageDatabase

/**
 * Configures ARCore Session for stable image tracking.
 * 
 * This configuration optimizes for:
 * - Image tracking only (no plane detection)
 * - Battery efficiency
 * - Stable tracking performance
 */
class ARSessionConfig {
    
    companion object {
        private const val TAG = "ARSessionConfig"
    }
    
    /**
     * Configures an ARCore session for augmented image tracking.
     * 
     * Key settings:
     * - Plane detection: DISABLED (saves battery, we only need image tracking)
     * - Update mode: LATEST_CAMERA_IMAGE (best tracking performance)
     * - Focus mode: AUTO (handles varying distances)
     * - Augmented images: ENABLED with provided database
     * 
     * @param session The ARCore session to configure
     * @param database The augmented image database with reference images
     * @return true if configuration succeeded, false otherwise
     */
    fun configure(session: Session, database: AugmentedImageDatabase): Boolean {
        return try {
            val config = Config(session).apply {
                // Disable plane detection - we only need image tracking
                // This saves battery and processing power
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                
                // Set the augmented image database
                augmentedImageDatabase = database
                
                // Use latest camera image for best tracking
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                
                // Auto focus for varying distances
                focusMode = Config.FocusMode.AUTO
                
                // Enable depth if device supports it (improves occlusion)
                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    depthMode = Config.DepthMode.AUTOMATIC
                    Log.d(TAG, "✅ Depth mode enabled")
                } else {
                    depthMode = Config.DepthMode.DISABLED
                    Log.d(TAG, "ℹ️ Depth mode not supported on this device")
                }
                
                // Disable instant placement (not needed for image tracking)
                instantPlacementMode = Config.InstantPlacementMode.DISABLED
                
                // Enable light estimation for better rendering
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            }
            
            // Apply configuration to session
            session.configure(config)
            
            Log.i(TAG, "✅ AR Session configured successfully")
            Log.d(TAG, "  - Plane detection: DISABLED")
            Log.d(TAG, "  - Image tracking: ENABLED")
            Log.d(TAG, "  - Update mode: LATEST_CAMERA_IMAGE")
            Log.d(TAG, "  - Focus mode: AUTO")
            Log.d(TAG, "  - Images in database: ${database.numImages}")
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to configure AR session", e)
            false
        }
    }
    
    /**
     * Checks if the device supports ARCore and required features.
     * Call this before creating an AR session.
     * 
     * @param session The ARCore session to check
     * @return true if all required features are supported
     */
    fun checkDeviceSupport(session: Session): Boolean {
        val supported = mutableListOf<String>()
        val unsupported = mutableListOf<String>()
        
        // Check depth support
        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
            supported.add("Depth mode")
        } else {
            unsupported.add("Depth mode (optional)")
        }
        
        // Log support status
        if (supported.isNotEmpty()) {
            Log.d(TAG, "✅ Supported features: ${supported.joinToString(", ")}")
        }
        if (unsupported.isNotEmpty()) {
            Log.d(TAG, "ℹ️ Unsupported features: ${unsupported.joinToString(", ")}")
        }
        
        // All core features are always supported if ARCore is available
        return true
    }
    
    /**
     * Updates session configuration at runtime if needed.
     * Use this to change settings without recreating the session.
     * 
     * @param session The ARCore session to update
     * @param enableDepth Whether to enable depth mode
     */
    fun updateConfiguration(session: Session, enableDepth: Boolean = true) {
        try {
            val config = session.config
            
            if (enableDepth && session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            } else {
                config.depthMode = Config.DepthMode.DISABLED
            }
            
            session.configure(config)
            Log.d(TAG, "✅ Configuration updated")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update configuration", e)
        }
    }
    
    /**
     * Gets recommended configuration for different scenarios.
     */
    object Presets {
        /**
         * Battery-optimized configuration.
         * Use when battery life is more important than tracking quality.
         */
        fun batteryOptimized(session: Session, database: AugmentedImageDatabase): Config {
            return Config(session).apply {
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                augmentedImageDatabase = database
                updateMode = Config.UpdateMode.BLOCKING
                focusMode = Config.FocusMode.AUTO
                depthMode = Config.DepthMode.DISABLED
                lightEstimationMode = Config.LightEstimationMode.DISABLED
            }
        }
        
        /**
         * Performance-optimized configuration.
         * Use for best tracking quality and responsiveness.
         */
        fun performanceOptimized(session: Session, database: AugmentedImageDatabase): Config {
            return Config(session).apply {
                planeFindingMode = Config.PlaneFindingMode.DISABLED
                augmentedImageDatabase = database
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                focusMode = Config.FocusMode.AUTO
                depthMode = if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    Config.DepthMode.AUTOMATIC
                } else {
                    Config.DepthMode.DISABLED
                }
                lightEstimationMode = Config.LightEstimationMode.AMBIENT_INTENSITY
            }
        }
    }
}

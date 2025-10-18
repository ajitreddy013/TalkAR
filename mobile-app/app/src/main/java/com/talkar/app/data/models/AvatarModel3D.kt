package com.talkar.app.data.models

import androidx.annotation.RawRes

/**
 * 3D Avatar Model representation for AR rendering
 * 
 * This model defines the properties of a 3D avatar that can be loaded
 * and displayed in AR over detected images.
 */
data class AvatarModel3D(
    /**
     * Unique identifier for this avatar
     */
    val id: String,
    
    /**
     * Display name of the avatar (e.g., "SRK Style Avatar", "Female Presenter")
     */
    val name: String,
    
    /**
     * Description of the avatar
     */
    val description: String? = null,
    
    /**
     * Resource ID of the GLB/GLTF model file in res/raw
     * Use R.raw.avatar_model_name
     */
    @RawRes
    val modelResourceId: Int? = null,
    
    /**
     * Remote URL of the GLB model (for dynamic loading)
     * Either modelResourceId or modelUrl must be provided
     */
    val modelUrl: String? = null,
    
    /**
     * Scale factor for the 3D model (1.0 = original size)
     */
    val scale: Float = 1.0f,
    
    /**
     * Initial position offset from the image anchor (x, y, z in meters)
     */
    val positionOffset: Position3D = Position3D(0f, 0f, 0f),
    
    /**
     * Initial rotation in degrees (x, y, z)
     */
    val rotationOffset: Rotation3D = Rotation3D(0f, 0f, 0f),
    
    /**
     * Animation configuration for idle state
     */
    val idleAnimation: IdleAnimation = IdleAnimation.BREATHING,
    
    /**
     * Voice ID associated with this avatar (for TTS)
     */
    val voiceId: String? = null,
    
    /**
     * Whether this avatar is currently active/enabled
     */
    val isActive: Boolean = true,
    
    /**
     * Mapped to which backend image ID(s)
     * When this image is detected, this avatar will be loaded
     */
    val mappedImageIds: List<String> = emptyList(),
    
    /**
     * Avatar type/category for filtering
     */
    val avatarType: AvatarType = AvatarType.GENERIC,
    
    /**
     * Gender representation (optional, for filtering)
     */
    val gender: Gender = Gender.NEUTRAL
)

/**
 * 3D Position offset
 */
data class Position3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

/**
 * 3D Rotation (Euler angles in degrees)
 */
data class Rotation3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
)

/**
 * Idle animation types for avatars
 */
enum class IdleAnimation {
    NONE,           // No idle animation
    BREATHING,      // Subtle chest/body breathing motion
    BLINKING,       // Eye blinking animation
    BREATHING_AND_BLINKING,  // Combined breathing and blinking
    CUSTOM          // Custom animation from model file
}

/**
 * Avatar type categories
 */
enum class AvatarType {
    CELEBRITY,      // Celebrity-style avatar
    GENERIC,        // Generic presenter/speaker
    SPORTS,         // Sports personality
    HISTORICAL,     // Historical figure
    CUSTOM          // Custom avatar
}

/**
 * Gender representation for avatars
 */
enum class Gender {
    MALE,
    FEMALE,
    NEUTRAL
}

/**
 * Avatar 3D Model mapping to backend images
 * This defines which avatar to load for which detected image
 */
data class ImageAvatarMapping(
    val imageId: String,
    val avatarId: String,
    val priority: Int = 0  // Higher priority mappings take precedence
)

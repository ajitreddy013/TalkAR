package com.talkar.app.data.models

/**
 * Avatar data model representing a 3D avatar from the backend
 * Matches the enhanced backend schema for Week 4 Phase 1
 */
data class Avatar(
    val id: String,
    val name: String,
    val description: String? = null,
    val avatarImageUrl: String, // 2D preview image
    val avatarVideoUrl: String? = null, // Optional 2D video fallback
    val avatar3DModelUrl: String? = null, // 3D model file path (GLB/GLTF)
    val voiceId: String? = null, // Voice ID for TTS
    val idleAnimationType: String? = "breathing", // Type of idle animation
    val isActive: Boolean = true
)

// BackendImage and Dialogue are already defined in other files

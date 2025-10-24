package com.talkar.app.data.models

/**
 * Avatar data model
 */
data class Avatar(
    val id: String,
    val name: String,
    val description: String? = null,
    val avatarImageUrl: String,
    val avatarVideoUrl: String? = null,
    val voiceId: String? = null,
    val isActive: Boolean = true
)

// BackendImage and Dialogue are already defined in other files

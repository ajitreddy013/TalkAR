package com.talkar.app.data.models

import com.talkar.app.data.api.ImageAvatarMapping

data class BackendImage(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val dialogues: List<Dialogue> = emptyList(),
    val avatarMapping: AvatarMappingDetails? = null // Optional avatar mapping details
)

/**
 * Avatar mapping details included when fetching image with avatar data
 */
data class AvatarMappingDetails(
    val id: String,
    val avatarId: String,
    val script: String? = null,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val visemeDataUrl: String? = null,
    val avatar: Avatar? = null
)

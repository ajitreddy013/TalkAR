package com.talkar.app.data.models

data class BackendImage(
    val id: String,
    val name: String,
    val description: String,
    val imageUrl: String,
    val thumbnailUrl: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val dialogues: List<Dialogue> = emptyList()
)

package com.talkar.app.data.models

data class AdContent(
    val script: String,
    val audioUrl: String? = null,
    val videoUrl: String? = null,
    val productName: String,
    val timestamp: Long = System.currentTimeMillis()
)
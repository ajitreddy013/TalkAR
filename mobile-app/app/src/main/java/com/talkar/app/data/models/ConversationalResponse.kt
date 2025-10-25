package com.talkar.app.data.models

/**
 * Data class representing a conversational response from the AI model
 */
data class ConversationalResponse(
    val success: Boolean,
    val text: String,
    val audioUrl: String? = null,
    val timestamp: Long
)
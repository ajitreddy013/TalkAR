package com.talkar.app.data.models

import com.google.gson.annotations.SerializedName

data class ImageRecognition(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("dialogues")
    val dialogues: List<Dialogue>,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
)

data class Dialogue(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("language")
    val language: String,
    
    @SerializedName("voiceId")
    val voiceId: String?,
    
    @SerializedName("isDefault")
    val isDefault: Boolean = false
)

data class SyncRequest(
    val text: String,
    val language: String,
    val voiceId: String? = null
)

data class SyncResponse(
    val videoUrl: String,
    val duration: Long,
    val status: String
)


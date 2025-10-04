package com.talkar.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.talkar.app.data.local.Converters

@Entity(tableName = "imagerecognition")
@TypeConverters(Converters::class)
data class ImageRecognition(
    @PrimaryKey
    @SerializedName("id")
    val id: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("dialogues")
    val dialogues: List<Dialogue> = emptyList(),
    
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
    val voiceId: String? = null,
    val imageUrl: String? = null
)

data class SyncResponse(
    val videoUrl: String,
    val duration: Long,
    val status: String
)

data class TalkingHeadVideo(
    @SerializedName("imageId")
    val imageId: String,
    
    @SerializedName("videoUrl")
    val videoUrl: String,
    
    @SerializedName("duration")
    val duration: Int,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("language")
    val language: String,
    
    @SerializedName("voiceId")
    val voiceId: String,
    
    @SerializedName("createdAt")
    val createdAt: String
)


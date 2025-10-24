package com.talkar.app.data.models

import com.google.gson.annotations.SerializedName

data class UserPreferences(
    @SerializedName("language")
    val language: String = "English",
    
    @SerializedName("preferred_tone")
    val preferredTone: String = "casual"
)
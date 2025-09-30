package com.talkar.app.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.talkar.app.data.models.Dialogue

class Converters {
    
    @TypeConverter
    fun fromDialogueList(value: List<Dialogue>?): String? {
        return if (value == null) null else Gson().toJson(value)
    }
    
    @TypeConverter
    fun toDialogueList(value: String?): List<Dialogue>? {
        return if (value == null) null else {
            val listType = object : TypeToken<List<Dialogue>>() {}.type
            Gson().fromJson(value, listType)
        }
    }
}

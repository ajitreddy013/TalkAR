package com.talkar.app

import android.app.Application
import com.talkar.app.data.repository.ImageRepository
import com.talkar.app.data.repository.SyncRepository
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.local.ImageDatabase

class TalkARApplication : Application() {
    
    // Repository instances
    val imageRepository by lazy { 
        ImageRepository(
            apiClient = ApiClient.create(),
            database = ImageDatabase.getDatabase(this)
        )
    }
    
    val syncRepository by lazy { 
        SyncRepository(
            apiClient = ApiClient.create()
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: TalkARApplication
            private set
    }
}


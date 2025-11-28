package com.talkar.app

import android.app.Application
import com.talkar.app.data.repository.ImageRepository
import com.talkar.app.data.repository.SyncRepository
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.local.ImageDatabase
import com.talkar.app.data.services.ARImageRecognitionService
import com.talkar.app.data.services.ConfigSyncService
import com.talkar.app.data.services.UserPreferencesService
import com.talkar.app.data.services.NetworkMonitor
import com.talkar.app.data.services.DatabaseCleanupService

class TalkARApplication : Application() {
    
    // Repository instances
    val imageRepository by lazy { 
        ImageRepository(
            apiClient = ApiClient.create(),
            database = ImageDatabase.getDatabase(this),
            context = this
        )
    }
    
    val syncRepository by lazy { 
        SyncRepository(
            apiClient = ApiClient.create()
        )
    }
    
    val apiClient by lazy { 
        ApiClient.create()
    }
    
    val arImageRecognitionService by lazy {
        ARImageRecognitionService(this)
    }
    
    val userPreferencesService by lazy {
        UserPreferencesService.getInstance(this)
    }
    
    val configSyncService by lazy {
        ConfigSyncService()
    }
    
    val networkMonitor by lazy {
        NetworkMonitor(this)
    }
    
    val databaseCleanupService by lazy {
        DatabaseCleanupService(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Start configuration sync service
        configSyncService.startSync(30) // Sync every 30 seconds
        
        // Clean up old database entries
        databaseCleanupService.cleanupOldData()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        // Clean up resources to prevent memory leaks
        configSyncService.cleanup()
        arImageRecognitionService.cleanup()
        networkMonitor.stopMonitoring()
    }
    
    companion object {
        lateinit var instance: TalkARApplication
            private set
    }
}
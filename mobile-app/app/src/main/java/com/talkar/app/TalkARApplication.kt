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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TalkARApplication : Application() {
    
    // Application-scoped coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
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
        
        // Defer heavy initialization to background thread
        android.util.Log.d("TalkARApplication", "Starting lightweight initialization")
        
        // Start critical services on background thread to avoid blocking main thread
        applicationScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("TalkARApplication", "Starting background initialization")
                
                // Start configuration sync service
                configSyncService.startSync(30) // Sync every 30 seconds
                
                // Clean up old database entries
                databaseCleanupService.cleanupOldData()
                
                android.util.Log.d("TalkARApplication", "✅ Background initialization complete")
            } catch (e: Exception) {
                android.util.Log.e("TalkARApplication", "❌ Background initialization failed", e)
            }
        }
        
        android.util.Log.d("TalkARApplication", "✅ Lightweight initialization complete")
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
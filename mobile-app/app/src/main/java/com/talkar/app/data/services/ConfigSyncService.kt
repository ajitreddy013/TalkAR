package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.data.api.ApiClient
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 * Service for periodically syncing configuration from the backend
 */
class ConfigSyncService {
    private val TAG = "ConfigSyncService"
    private val apiService = ApiClient.create()
    private var isSyncing = false
    private var syncJob: Job? = null
    private var coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Configuration cache
    private var cachedConfigs: Map<String, String> = emptyMap()
    
    // Callback interface for configuration updates
    interface ConfigUpdateListener {
        fun onConfigUpdated(key: String, value: String)
        fun onConfigsUpdated(configs: Map<String, String>)
        fun onSyncStatusChanged(syncing: Boolean)
    }
    
    private var configUpdateListener: ConfigUpdateListener? = null
    
    fun setConfigUpdateListener(listener: ConfigUpdateListener) {
        this.configUpdateListener = listener
    }
    
    /**
     * Start periodic configuration sync
     */
    fun startSync(intervalSeconds: Long = 30) {
        if (isSyncing) {
            Log.d(TAG, "Configuration sync already running")
            return
        }
        
        isSyncing = true
        configUpdateListener?.onSyncStatusChanged(true)
        
        syncJob = coroutineScope.launch {
            while (isSyncing) {
                try {
                    syncConfigs()
                    delay(TimeUnit.SECONDS.toMillis(intervalSeconds))
                } catch (e: Exception) {
                    Log.e(TAG, "Error during config sync", e)
                    delay(TimeUnit.SECONDS.toMillis(5)) // Retry after 5 seconds on error
                }
            }
        }
        
        Log.d(TAG, "Configuration sync started with interval: ${intervalSeconds}s")
    }
    
    /**
     * Stop periodic configuration sync
     */
    fun stopSync() {
        isSyncing = false
        syncJob?.cancel()
        configUpdateListener?.onSyncStatusChanged(false)
        Log.d(TAG, "Configuration sync stopped")
    }
    
    /**
     * Force immediate configuration sync
     */
    suspend fun syncConfigs() {
        try {
            Log.d(TAG, "Syncing configurations from backend")
            
            // In a real implementation, we would have an endpoint to get all configs
            // For now, we'll simulate getting updated configs
            val updatedConfigs = fetchConfigsFromBackend()
            
            // Check for changes and notify listeners
            updatedConfigs.forEach { (key, value) ->
                val oldValue = cachedConfigs[key]
                if (oldValue != value) {
                    Log.d(TAG, "Config updated: $key = $value")
                    configUpdateListener?.onConfigUpdated(key, value)
                }
            }
            
            // Update cache and notify bulk update
            if (cachedConfigs != updatedConfigs) {
                cachedConfigs = updatedConfigs
                configUpdateListener?.onConfigsUpdated(updatedConfigs)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing configurations", e)
        }
    }
    
    /**
     * Fetch configurations from backend
     */
    private suspend fun fetchConfigsFromBackend(): Map<String, String> {
        // This is a simplified implementation
        // In reality, you would make API calls to fetch the actual configurations
        val configs = mutableMapOf<String, String>()
        
        try {
            // Fetch default tone
            try {
                val toneResponse = apiService.getDefaultTone()
                if (toneResponse.isSuccessful) {
                    configs["default_tone"] = toneResponse.body()?.tone ?: "friendly"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching default tone", e)
            }
            
            // Fetch prompt template
            try {
                val templateResponse = apiService.getPromptTemplate()
                if (templateResponse.isSuccessful) {
                    configs["prompt_template"] = templateResponse.body()?.template 
                        ?: "Create a short, engaging script for a product advertisement. The product is {product}. Highlight its key features and benefits in a {tone} tone."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching prompt template", e)
            }
            
            // You would fetch other configs here as well
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching configs from backend", e)
        }
        
        return configs
    }
    
    /**
     * Get cached configuration value
     */
    fun getConfig(key: String): String? {
        return cachedConfigs[key]
    }
    
    /**
     * Get all cached configurations
     */
    fun getAllConfigs(): Map<String, String> {
        return cachedConfigs
    }
    
    /**
     * Check if sync is currently running
     */
    fun isSyncing(): Boolean {
        return isSyncing
    }
}
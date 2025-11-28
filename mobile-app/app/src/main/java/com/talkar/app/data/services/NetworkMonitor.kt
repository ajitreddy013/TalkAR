package com.talkar.app.data.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service to monitor network connectivity and provide offline handling capabilities
 */
class NetworkMonitor(private val context: Context) {
    private val tag = "NetworkMonitor"
    
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    init {
        startMonitoring()
    }
    
    /**
     * Start monitoring network connectivity changes
     */
    private fun startMonitoring() {
        try {
            connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Log.d(tag, "Network available")
                    _isConnected.value = true
                    checkInternetConnectivity()
                }
                
                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.d(tag, "Network lost")
                    // Check if any network is still available
                    checkNetworkConnectivity()
                }
                
                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.d(tag, "Network unavailable")
                    _isConnected.value = false
                    _isOnline.value = false
                }
            }
            
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
            checkNetworkConnectivity()
            
        } catch (e: Exception) {
            Log.e(tag, "Error starting network monitoring", e)
            _isConnected.value = false
            _isOnline.value = false
        }
    }
    
    /**
     * Check current network connectivity status
     */
    private fun checkNetworkConnectivity() {
        try {
            val activeNetwork = connectivityManager?.activeNetwork
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
            
            val isConnected = networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isConnected.value = isConnected
            
            if (isConnected) {
                checkInternetConnectivity()
            } else {
                _isOnline.value = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error checking network connectivity", e)
            _isConnected.value = false
            _isOnline.value = false
        }
    }
    
    /**
     * Check if we can actually reach the internet
     */
    private fun checkInternetConnectivity() {
        try {
            // For now, we'll assume that if we have network connectivity, we're online
            // In a more sophisticated implementation, we could ping a known endpoint
            _isOnline.value = _isConnected.value
        } catch (e: Exception) {
            Log.e(tag, "Error checking internet connectivity", e)
            _isOnline.value = false
        }
    }
    
    /**
     * Check if device has internet connectivity
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e(tag, "Error checking network availability", e)
            false
        }
    }
    
    /**
     * Stop monitoring network connectivity
     */
    fun stopMonitoring() {
        try {
            networkCallback?.let { callback ->
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping network monitoring", e)
        }
    }
    
    /**
     * Get current network status message for user display
     */
    fun getNetworkStatusMessage(): String {
        return when {
            _isOnline.value -> "Connected to internet"
            _isConnected.value -> "Network connected but no internet access"
            else -> "No network connection"
        }
    }
    
    /**
     * Check if we should use offline mode
     */
    fun shouldUseOfflineMode(): Boolean {
        return !_isOnline.value
    }
}
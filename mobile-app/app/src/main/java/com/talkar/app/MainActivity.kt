package com.talkar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// import com.talkar.app.ui.screens.ARScreen // Disabled for new AR implementation
// import com.talkar.app.ui.screens.Week2ARScreen // Disabled for new AR implementation
import com.talkar.app.ui.screens.TalkARScreen
import com.talkar.app.ui.theme.TalkARTheme
import com.talkar.app.ui.viewmodels.SimpleARViewModel
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import com.talkar.app.data.repository.ImageRepository
import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.local.ImageDatabase
import com.talkar.app.data.services.ConfigSyncService

class MainActivity : ComponentActivity() {
    
    private var hasCameraPermission by mutableStateOf(false)
    private var hasAudioPermission by mutableStateOf(false)
    private lateinit var configSyncService: ConfigSyncService
    
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        
        android.util.Log.d("MainActivity", "Camera permission: $hasCameraPermission")
        android.util.Log.d("MainActivity", "Audio permission: $hasAudioPermission")
        
        if (!hasCameraPermission) {
            android.util.Log.e("MainActivity", "Camera permission denied")
        }
        if (!hasAudioPermission) {
            android.util.Log.e("MainActivity", "Audio permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("MainActivity", "MainActivity onCreate - starting lightweight initialization")
        
        // Initialize config sync service
        configSyncService = (application as TalkARApplication).configSyncService
        configSyncService.setConfigUpdateListener(object : ConfigSyncService.ConfigUpdateListener {
            override fun onConfigUpdated(key: String, value: String) {
                android.util.Log.d("MainActivity", "Config updated: $key = $value")
                // Handle specific config updates here
                // For example, update UI elements or restart services
            }
            
            override fun onConfigsUpdated(configs: Map<String, String>) {
                android.util.Log.d("MainActivity", "All configs updated: $configs")
                // Handle bulk config updates here
            }
            
            override fun onSyncStatusChanged(syncing: Boolean) {
                android.util.Log.d("MainActivity", "Config sync status changed: $syncing")
            }
        })
        
        // Defer heavy work and set up UI immediately
        setContent {
            TalkARTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Lazy initialization of ViewModels
                    val simpleViewModel: SimpleARViewModel = viewModel()
                    
                    // Enhanced AR ViewModel for Week 2
                    val enhancedViewModel: EnhancedARViewModel = viewModel {
                        val apiService = ApiClient.create()
                        val database = ImageDatabase.getDatabase(this@MainActivity)
                        val repository = ImageRepository(apiService, database, this@MainActivity)
                        EnhancedARViewModel(repository)
                    }
                    
                    // Check permissions in background to avoid blocking UI
                    LaunchedEffect(Unit) {
                        withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val cameraGranted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            val audioGranted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                hasCameraPermission = cameraGranted
                                hasAudioPermission = audioGranted
                                
                                android.util.Log.d("MainActivity", "Camera permission: $hasCameraPermission")
                                android.util.Log.d("MainActivity", "Audio permission: $hasAudioPermission")
                                
                                if (!hasCameraPermission || !hasAudioPermission) {
                                    android.util.Log.d("MainActivity", "Requesting permissions...")
                                    requestPermissionsLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                } else {
                                    android.util.Log.d("MainActivity", "All permissions granted")
                                }
                            }
                        }
                    }
                    
                    // TalkAR Screen - ARCore Augmented Images
                    if (hasCameraPermission && hasAudioPermission) {
                        TalkARScreen()
                    } else {
                        // Show permission request UI
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "📷🎤 Permissions Required",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "TalkAR needs camera and microphone access for AR features and voice interaction",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            requestPermissionsLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.CAMERA,
                                                    Manifest.permission.RECORD_AUDIO
                                                )
                                            )
                                        }
                                    ) {
                                        Text("Grant Permissions")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
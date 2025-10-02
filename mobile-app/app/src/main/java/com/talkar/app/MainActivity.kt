package com.talkar.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.talkar.app.ui.screens.ARScreen
import com.talkar.app.ui.theme.TalkARTheme
import com.talkar.app.ui.viewmodels.SimpleARViewModel

class MainActivity : ComponentActivity() {
    
    private var hasCameraPermission by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        android.util.Log.d("MainActivity", "Permission result: $isGranted")
        if (!isGranted) {
            // Permission denied, show error message
            android.util.Log.e("MainActivity", "Camera permission denied")
        } else {
            android.util.Log.d("MainActivity", "Camera permission granted!")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check camera permission
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        android.util.Log.d("MainActivity", "Camera permission status: $hasCameraPermission")
        
        if (!hasCameraPermission) {
            android.util.Log.d("MainActivity", "Requesting camera permission...")
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            android.util.Log.d("MainActivity", "Camera permission already granted")
        }
        
        setContent {
            TalkARTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: SimpleARViewModel = viewModel()
                    ARScreen(
                        viewModel = viewModel,
                        hasCameraPermission = hasCameraPermission,
                        onPermissionCheck = {
                            // Re-check permission when requested
                            hasCameraPermission = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            android.util.Log.d("MainActivity", "Re-checked permission: $hasCameraPermission")
                        }
                    )
                }
            }
        }
    }
}


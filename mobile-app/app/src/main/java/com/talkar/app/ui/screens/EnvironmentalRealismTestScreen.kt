package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ar.core.LightEstimate
import com.talkar.app.data.services.EnhancedARService
import com.talkar.app.ui.components.LightingTestView
import com.talkar.app.ui.viewmodels.EnhancedARViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentalRealismTestScreen(
    viewModel: EnhancedARViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val isTracking by viewModel.isTracking.collectAsState()
    val trackingQuality by viewModel.trackingQuality.collectAsState()
    val lightingQuality by viewModel.lightingQuality.collectAsState()
    val lightEstimate by viewModel.lightEstimate.collectAsState()
    val isAvatarSpeaking by viewModel.isAvatarSpeaking.collectAsState()
    
    var testVolume by remember { mutableStateOf(0.5f) }
    var isAmbientPlaying by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Environmental Realism Test") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tracking Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Tracking Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Tracking: ${if (isTracking) "Active" else "Inactive"}")
                    Text("Quality: $trackingQuality")
                    Text("Lighting: $lightingQuality")
                    Text("Avatar Speaking: ${if (isAvatarSpeaking) "Yes" else "No"}")
                }
            }
            
            // Lighting Visualization
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        LightingTestView(context)
                    },
                    update = { view ->
                        view.updateLightEstimate(lightEstimate, lightingQuality.name)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Audio Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Audio Controls",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ambient Volume: ${(testVolume * 100).toInt()}%")
                        
                        Switch(
                            checked = isAmbientPlaying,
                            onCheckedChange = { 
                                isAmbientPlaying = it
                                if (it) {
                                    viewModel.startAmbientAudio()
                                } else {
                                    viewModel.stopAmbientAudio()
                                }
                            }
                        )
                    }
                    
                    Slider(
                        value = testVolume,
                        onValueChange = { 
                            testVolume = it
                            // In a real implementation, we would update the actual volume
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                viewModel.setAvatarSpeaking(true)
                                // Simulate speaking for 3 seconds
                                coroutineScope.launch {
                                    delay(3000)
                                    viewModel.setAvatarSpeaking(false)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Avatar Speech")
                        }
                        
                        Button(
                            onClick = { viewModel.startAmbientAudio() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play Ambient")
                        }
                    }
                }
            }
            
            // Shadow Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Visual Effects",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { /* Test shadow rendering */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Test Shadow Rendering")
                    }
                }
            }
        }
    }
}
package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.talkar.app.ui.components.ARView
import com.talkar.app.ui.components.AROverlay
import com.talkar.app.ui.components.ARVideoOverlay
import com.talkar.app.ui.components.ImageRecognitionCard
import com.talkar.app.ui.components.SyncVideoPlayer
import com.talkar.app.ui.viewmodels.ARViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    viewModel: ARViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val recognizedImage by viewModel.recognizedImage.collectAsState()
    val syncVideo by viewModel.syncVideo.collectAsState()
    val recognizedAugmentedImage by viewModel.recognizedAugmentedImage.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TalkAR") },
                actions = {
                    if (recognizedImage != null) {
                        TextButton(
                            onClick = { viewModel.resetRecognition() }
                        ) {
                            Text("Reset")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AR View
            ARView(
                modifier = Modifier
                    .fillMaxSize(),
                onImageRecognized = { imageRecognition ->
                    viewModel.recognizeImage(imageRecognition)
                },
                onAugmentedImageRecognized = { augmentedImage ->
                    viewModel.setRecognizedAugmentedImage(augmentedImage)
                }
            )
            
            // AR Overlay positioned over recognized image
            if (recognizedAugmentedImage != null && syncVideo != null) {
                AROverlay(
                    recognizedImage = recognizedAugmentedImage,
                    syncVideo = syncVideo,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Fallback video overlay for non-AR display
            if (recognizedImage != null && syncVideo != null && recognizedAugmentedImage == null) {
                ARVideoOverlay(
                    videoUrl = syncVideo.videoUrl,
                    isVisible = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
            
            // Content based on state
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                recognizedImage != null -> {
                    // Show recognized image info and sync video
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val currentRecognizedImage = recognizedImage
                        if (currentRecognizedImage != null) {
                            ImageRecognitionCard(
                                image = currentRecognizedImage,
                                onGenerateVideo = { text, language, voiceId ->
                                    viewModel.generateSyncVideo(text, language, voiceId)
                                }
                            )
                        }
                        
                        val currentSyncVideo = syncVideo
                        if (currentSyncVideo != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SyncVideoPlayer(
                                videoUrl = currentSyncVideo.videoUrl,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                else -> {
                    // Show available images
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.images) { image ->
                            ImageRecognitionCard(
                                image = image,
                                onGenerateVideo = { text, language, voiceId ->
                                    viewModel.generateSyncVideo(text, language, voiceId)
                                }
                            )
                        }
                    }
                }
            }
            
            // Error handling
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}


package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.talkar.app.ui.components.ARView
import com.talkar.app.ui.components.ImageRecognitionCard
import com.talkar.app.ui.components.SyncVideoPlayer
import com.talkar.app.ui.viewmodels.ARViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARScreen(
    viewModel: ARViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recognizedImage by viewModel.recognizedImage.collectAsStateWithLifecycle()
    val syncVideo by viewModel.syncVideo.collectAsStateWithLifecycle()
    
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // AR View
            ARView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onImageRecognized = { imageRecognition ->
                    viewModel.recognizeImage(imageRecognition)
                }
            )
            
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
                        ImageRecognitionCard(
                            image = recognizedImage,
                            onGenerateVideo = { text, language, voiceId ->
                                viewModel.generateSyncVideo(text, language, voiceId)
                            }
                        )
                        
                        if (syncVideo != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SyncVideoPlayer(
                                videoUrl = syncVideo.videoUrl,
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


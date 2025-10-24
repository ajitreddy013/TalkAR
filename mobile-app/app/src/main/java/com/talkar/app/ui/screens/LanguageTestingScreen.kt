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
import com.talkar.app.ui.components.ImageRecognitionCard
import com.talkar.app.ui.viewmodels.SimpleARViewModel
import com.talkar.app.data.models.ImageRecognition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageTestingScreen(
    viewModel: SimpleARViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val images = uiState.images
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multi-Language Testing") }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (images.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No images available. Please check your connection.")
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(images) { image ->
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
}
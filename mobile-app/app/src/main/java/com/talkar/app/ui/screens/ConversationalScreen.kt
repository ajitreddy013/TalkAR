package com.talkar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.talkar.app.data.services.SpeechResult
import com.talkar.app.ui.components.ConversationalAvatarView
import com.talkar.app.ui.components.VoiceInputButton
import kotlinx.coroutines.launch

/**
 * Conversational screen that handles voice input and AI responses
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationalScreen(
    speechResult: SpeechResult,
    conversationalResponse: String?,
    isProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(conversationalResponse) {
        coroutineScope.launch {
            lazyListState.animateScrollToItem(Int.MAX_VALUE)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Conversation history
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Display user queries and AI responses
            item {
                // Placeholder for conversation history
                // In a real implementation, you would maintain a list of messages
                if (conversationalResponse != null) {
                    ConversationalAvatarView(
                        responseText = conversationalResponse,
                        isTyping = isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Show current speech result
            when (speechResult) {
                is SpeechResult.Success -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = speechResult.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                is SpeechResult.PartialResult -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text(
                                text = speechResult.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                is SpeechResult.Error -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Error: ${speechResult.message}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                else -> {
                    // Do nothing for Idle, Listening, Processing states
                }
            }
            
            // Show processing indicator
            if (isProcessing) {
                item {
                    ConversationalAvatarView(
                        responseText = "Thinking...",
                        isTyping = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Voice input button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            VoiceInputButton(
                isListening = speechResult is SpeechResult.Listening,
                onListenStart = onStartListening,
                onListenStop = onStopListening,
                modifier = Modifier.size(80.dp)
            )
        }
        
        // Status text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val statusText = when (speechResult) {
                is SpeechResult.Idle -> "Tap the microphone to speak"
                is SpeechResult.Listening -> "Listening... Speak now"
                is SpeechResult.Processing -> "Processing your request..."
                is SpeechResult.Success -> "Processing your request..."
                is SpeechResult.PartialResult -> "Listening..."
                is SpeechResult.Error -> "Error: ${speechResult.message}"
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
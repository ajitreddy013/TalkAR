package com.talkar.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Voice input button component that handles speech recognition
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputButton(
    isListening: Boolean,
    onListenStart: () -> Unit,
    onListenStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        FloatingActionButton(
            onClick = {
                isPressed = true
                if (isListening) {
                    onListenStop()
                } else {
                    onListenStart()
                }
                
                // Reset pressed state after a short delay
                coroutineScope.launch {
                    delay(200)
                    isPressed = false
                }
            },
            containerColor = if (isListening) Color.Red else MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .size(64.dp)
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start listening",
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Pulsing animation when listening
        if (isListening) {
            PulsingCircle(
                isListening = isListening,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

/**
 * Pulsing circle animation for voice input feedback
 */
@Composable
fun PulsingCircle(
    isListening: Boolean,
    modifier: Modifier = Modifier
) {
    val pulseAnimation by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = if (isListening) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        }
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) { 
            drawCircle(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                radius = size.minDimension / 2 * pulseAnimation
            )
        }
    }
}
package com.talkar.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Loading Indicator Component
 * 
 * Displays a spinning progress indicator with optional message
 * Used during lip-sync video processing and avatar loading
 */
@Composable
fun LoadingIndicator(
    message: String = "Processing...",
    modifier: Modifier = Modifier,
    isVisible: Boolean = true
) {
    if (!isVisible) return
    
    // Fade-in animation
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "loadingFadeIn"
    )
    
    // Rotation animation for the spinner
    val infiniteTransition = rememberInfiniteTransition(label = "loadingSpinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinnerRotation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .background(
                Color.Black.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Pulsing Dot Indicator
 * 
 * Simple pulsing dot to indicate activity or loading
 */
@Composable
fun PulsingDotIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsingDot")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .size(12.dp)
            .scale(scale)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}

/**
 * Scanning Indicator
 * 
 * Shows a scanning animation when looking for images
 */
@Composable
fun ScanningIndicator(
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    if (!isScanning) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineMove"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scanning line animation representation
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(2.dp)
                    .offset(y = offsetY.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "🔍 Scanning for images...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * Processing States Indicator
 * 
 * Shows different states: Detecting, Generating, Loading, Ready
 */
@Composable
fun ProcessingStateIndicator(
    state: ProcessingState,
    modifier: Modifier = Modifier
) {
    val (icon, message, color) = when (state) {
        ProcessingState.Idle -> Triple("", "", Color.Transparent)
        ProcessingState.Detecting -> Triple("🔍", "Detecting image...", MaterialTheme.colorScheme.primary)
        ProcessingState.Generating -> Triple("⚙️", "Generating lip-sync...", MaterialTheme.colorScheme.secondary)
        ProcessingState.Loading -> Triple("📥", "Loading avatar...", MaterialTheme.colorScheme.tertiary)
        ProcessingState.Ready -> Triple("✅", "Ready!", Color.Green)
    }
    
    if (state == ProcessingState.Idle) return
    
    // Fade in/out animation
    val alpha by animateFloatAsState(
        targetValue = if (state != ProcessingState.Idle) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "stateIndicatorFade"
    )
    
    Row(
        modifier = modifier
            .alpha(alpha)
            .background(
                Color.Black.copy(alpha = 0.7f),
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        
        if (state != ProcessingState.Ready) {
            Spacer(modifier = Modifier.width(8.dp))
            PulsingDotIndicator(color = color)
        }
    }
}

/**
 * Processing State Enum
 */
enum class ProcessingState {
    Idle,
    Detecting,
    Generating,
    Loading,
    Ready
}

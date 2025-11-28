package com.talkar.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enhanced Loading Screen with animated gradient bar and progress states
 */
@Composable
fun EnhancedLoadingScreen(
    currentState: LoadingState = LoadingState.INITIALIZING,
    modifier: Modifier = Modifier
) {
    // Animated gradient for the progress bar
    val infiniteTransition = rememberInfiniteTransition()
    val gradientAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )
    
    // Pulsing animation for the avatar silhouette
    val pulseAnimation by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing avatar silhouette
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(60.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = pulseAnimation),
                            MaterialTheme.colorScheme.primary.copy(alpha = pulseAnimation * 0.5f)
                        )
                    )
                )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Loading text
        Text(
            text = "Generating your experience...",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar with animated gradient
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(when (currentState) {
                        LoadingState.INITIALIZING -> 0.2f
                        LoadingState.GENERATING_SCRIPT -> 0.4f
                        LoadingState.STREAMING_AUDIO -> 0.6f
                        LoadingState.RENDERING_AVATAR -> 0.8f
                        LoadingState.READY -> 1.0f
                    })
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.tertiary
                            ),
                            start = androidx.compose.ui.geometry.Offset(gradientAnimation, 0f),
                            end = androidx.compose.ui.geometry.Offset(gradientAnimation + 500f, 0f)
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Current state indicator
        Card(
            modifier = Modifier.padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = when (currentState) {
                    LoadingState.INITIALIZING -> "🔧 Initializing..."
                    LoadingState.GENERATING_SCRIPT -> "📝 Generating script..."
                    LoadingState.STREAMING_AUDIO -> "🔊 Streaming audio..."
                    LoadingState.RENDERING_AVATAR -> "🎭 Rendering avatar..."
                    LoadingState.READY -> "✅ Ready!"
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Progress percentage
        Text(
            text = "${(when (currentState) {
                LoadingState.INITIALIZING -> 20
                LoadingState.GENERATING_SCRIPT -> 40
                LoadingState.STREAMING_AUDIO -> 60
                LoadingState.RENDERING_AVATAR -> 80
                LoadingState.READY -> 100
            })}%",
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Enum class for loading states
 */
enum class LoadingState {
    INITIALIZING,
    GENERATING_SCRIPT,
    STREAMING_AUDIO,
    RENDERING_AVATAR,
    READY
}
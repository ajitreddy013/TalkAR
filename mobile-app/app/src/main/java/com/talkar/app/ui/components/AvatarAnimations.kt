package com.talkar.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Avatar Entry Animation
 * 
 * Provides smooth entry animation for avatars with:
 * - Fade-in effect
 * - Scale-up spring animation
 * - Optional sound cue trigger
 */
@Composable
fun AvatarEntryAnimation(
    isVisible: Boolean,
    onAnimationComplete: () -> Unit = {},
    onSoundCue: () -> Unit = {},
    content: @Composable () -> Unit
) {
    // Track if animation has started
    var hasAnimated by remember { mutableStateOf(false) }
    
    // Fade-in animation
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "avatarFadeIn",
        finishedListener = {
            if (it == 1f && !hasAnimated) {
                hasAnimated = true
                onAnimationComplete()
            }
        }
    )
    
    // Scale animation with spring physics for bounce effect
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "avatarScaleUp"
    )
    
    // Trigger sound cue when animation starts
    LaunchedEffect(isVisible) {
        if (isVisible && !hasAnimated) {
            // Delay slightly for visual sync
            delay(100)
            onSoundCue()
        }
    }
    
    Box(
        modifier = Modifier
            .alpha(alpha)
            .scale(scale)
    ) {
        content()
    }
}

/**
 * Subtle Glow Effect
 * 
 * Adds a pulsing glow effect for newly appeared avatars
 */
@Composable
fun AvatarGlowEffect(
    isActive: Boolean,
    durationMillis: Int = 2000,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    
    // Auto-stop glow after a few cycles
    var showGlow by remember { mutableStateOf(isActive) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            showGlow = true
            delay(durationMillis * 3L) // Show for 3 cycles
            showGlow = false
        }
    }
    
    Box(modifier = modifier) {
        content()
        
        if (showGlow) {
            // Glow overlay (implement with actual UI if needed)
            // This is a placeholder for the effect
        }
    }
}

/**
 * Avatar Exit Animation
 * 
 * Smooth exit animation when avatar is dismissed
 */
@Composable
fun AvatarExitAnimation(
    isVisible: Boolean,
    onExitComplete: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutLinearInEasing
        ),
        label = "avatarFadeOut",
        finishedListener = {
            if (it == 0f) {
                onExitComplete()
            }
        }
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutLinearInEasing
        ),
        label = "avatarScaleDown"
    )
    
    Box(
        modifier = Modifier
            .alpha(alpha)
            .scale(scale)
    ) {
        content()
    }
}

/**
 * Shimmer Loading Effect
 * 
 * Shows a shimmer effect while avatar is loading
 */
@Composable
fun ShimmerLoadingEffect(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isLoading) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerSlide"
    )
    
    // Shimmer overlay implementation
    // This provides the foundation for shimmer effect
    Box(modifier = modifier) {
        // Implement shimmer gradient overlay here if needed
    }
}

/**
 * Breathing Animation
 * 
 * Subtle breathing effect for idle avatars
 */
@Composable
fun BreathingAnimation(
    isActive: Boolean,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathCycle"
    )
    
    Box(
        modifier = Modifier.scale(if (isActive) breathScale else 1f)
    ) {
        content()
    }
}

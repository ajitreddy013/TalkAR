package com.talkar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
// ARCore imports removed for now - using simplified implementation
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import kotlinx.coroutines.*
import kotlin.math.abs
import kotlin.math.sin

/**
 * Emotional Avatar View with facial expression animations
 */
@Composable
fun EmotionalAvatarView(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    emotion: String = "neutral",
    isTalking: Boolean = false,
    audioLevel: Float = 0f, // Real-time audio level for more responsive animations
    modifier: Modifier = Modifier
) {
    // Fade animation state
    var showAvatar by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500)
    ) { if (!isVisible) showAvatar = false }
    
    // Update showAvatar when isVisible changes
    LaunchedEffect(isVisible) {
        if (isVisible) {
            showAvatar = true
        }
    }
    
    if (!showAvatar || avatar == null || image == null) {
        return
    }

    var mouthProgress by remember { mutableStateOf(0f) }
    var blinkProgress by remember { mutableStateOf(0f) }
    var headTilt by remember { mutableStateOf(0f) }
    val emotionState by rememberUpdatedState(emotion)
    
    val coroutineScope = rememberCoroutineScope()
    var mouthAnimationJob by remember { mutableStateOf<Job?>(null) }
    var blinkAnimationJob by remember { mutableStateOf<Job?>(null) }
    var headTiltJob by remember { mutableStateOf<Job?>(null) }
    
    // Cancel previous jobs when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            mouthAnimationJob?.cancel()
            blinkAnimationJob?.cancel()
            headTiltJob?.cancel()
        }
    }
    
    // Animate mouth for talking with audio level sensitivity
    LaunchedEffect(isTalking, emotionState, audioLevel) {
        mouthAnimationJob?.cancel()
        if (isTalking) {
            mouthAnimationJob = coroutineScope.launch {
                val baseMouth = 0.3f
                while (isActive && isTalking) {
                    // Use audio level to drive mouth animation for more realistic lip-sync
                    mouthProgress = baseMouth + (audioLevel * 0.7f)
                    delay(50) // Faster updates for smoother animation
                }
            }
        } else {
            mouthProgress = 0f
        }
    }
    
    // Blink animation
    LaunchedEffect(Unit) {
        blinkAnimationJob?.cancel()
        blinkAnimationJob = coroutineScope.launch {
            while (isActive) {
                delay(2000L + (Math.random() * 3000).toLong())
                if (isActive) {
                    blinkProgress = 1f
                    delay(100)
                    if (isActive) {
                        blinkProgress = 0f
                    }
                }
            }
        }
    }
    
    // Head tilt animation based on emotion and audio
    LaunchedEffect(emotionState, audioLevel) {
        headTiltJob?.cancel()
        headTiltJob = coroutineScope.launch {
            val time = System.currentTimeMillis()
            while (isActive) {
                // Subtle head tilt that responds to audio and emotion
                val audioInfluence = audioLevel * 0.2f
                val emotionInfluence = when (emotionState) {
                    "happy" -> 0.1f
                    "surprised" -> 0.15f
                    "serious" -> -0.1f
                    else -> 0f
                }
                headTilt = sin(time.toFloat() / 1000) * 0.05f + audioInfluence + emotionInfluence
                delay(100)
            }
        }
    }

    Card(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp)
            .alpha(alpha), // Apply fade animation
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Avatar face with emotional expressions and head tilt
            Box(modifier = Modifier
                .size(150.dp)
                .graphicsLayer(
                    rotationZ = headTilt * 10 // Apply subtle head tilt
                )
            ) {
                AvatarFace(
                    emotion = emotionState,
                    mouthProgress = mouthProgress,
                    blinkProgress = blinkProgress,
                    modifier = Modifier.size(150.dp)
                )
            }
            
            // Avatar name and image info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = avatar.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = image.name,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Progress bar during speech
            if (isTalking) {
                SpeechProgressBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                )
            }
            
            // Live indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(12.dp)
                    .background(
                        color = if (isTalking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

/**
 * Speech progress bar visualization
 */
@Composable
fun SpeechProgressBar(modifier: Modifier = Modifier) {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            progress = (progress + 0.05f) % 1f
            delay(100)
        }
    }
    
    LinearProgressIndicator(
        progress = progress,
        modifier = modifier
            .width(120.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
}

/**
 * Avatar face with emotional expressions
 */
@Composable
private fun AvatarFace(
    emotion: String,
    mouthProgress: Float,
    blinkProgress: Float,
    modifier: Modifier = Modifier
) {
    // Use derivedStateOf to avoid unnecessary recompositions
    val memoizedEmotion by remember(emotion) { derivedStateOf { emotion } }
    val memoizedMouthProgress by remember(mouthProgress) { derivedStateOf { mouthProgress } }
    val memoizedBlinkProgress by remember(blinkProgress) { derivedStateOf { blinkProgress } }
    
    Canvas(modifier = modifier) {
        val faceCenter = Offset(size.width / 2, size.height / 2)
        val faceRadius = size.minDimension / 2 * 0.8f
        
        // Draw face
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = faceRadius,
            center = faceCenter
        )
        
        // Draw eyes based on emotion
        val eyeOffsetX = faceRadius * 0.3f
        val eyeOffsetY = faceRadius * 0.2f
        val eyeRadius = faceRadius * 0.1f
        
        // Left eye
        drawEye(
            center = Offset(faceCenter.x - eyeOffsetX, faceCenter.y - eyeOffsetY),
            radius = eyeRadius,
            emotion = memoizedEmotion,
            blinkProgress = memoizedBlinkProgress
        )
        
        // Right eye
        drawEye(
            center = Offset(faceCenter.x + eyeOffsetX, faceCenter.y - eyeOffsetY),
            radius = eyeRadius,
            emotion = memoizedEmotion,
            blinkProgress = memoizedBlinkProgress
        )
        
        // Draw mouth based on emotion and talking state
        drawMouth(
            center = Offset(faceCenter.x, faceCenter.y + faceRadius * 0.3f),
            width = faceRadius * 0.6f,
            emotion = memoizedEmotion,
            mouthProgress = memoizedMouthProgress
        )
        
        // Draw eyebrows based on emotion
        drawEyebrows(
            center = Offset(faceCenter.x, faceCenter.y - faceRadius * 0.5f),
            width = faceRadius * 0.7f,
            emotion = memoizedEmotion
        )
    }
}

/**
 * Draw an eye with emotional expression
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEye(
    center: Offset,
    radius: Float,
    emotion: String,
    blinkProgress: Float
) {
    val eyeHeight = radius * (1 - blinkProgress * 0.9f) // Animate blink
    
    // Draw eye white
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - radius, center.y - eyeHeight),
        size = Size(radius * 2, eyeHeight * 2)
    )
    
    // Draw pupil based on emotion
    val pupilRadius = radius * 0.4f
    val pupilOffset = when (emotion) {
        "surprised" -> Offset.Zero
        "happy" -> Offset(0f, -pupilRadius * 0.2f)
        "serious" -> Offset(0f, pupilRadius * 0.2f)
        else -> Offset.Zero
    }
    
    drawCircle(
        color = Color.Black,
        radius = pupilRadius,
        center = center + pupilOffset
    )
}

/**
 * Draw a mouth with emotional expression
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMouth(
    center: Offset,
    width: Float,
    emotion: String,
    mouthProgress: Float
) {
    val path = Path()
    
    when (emotion) {
        "happy" -> {
            // Happy smile
            val height = width * 0.3f * (0.5f + mouthProgress * 0.5f)
            path.moveTo(center.x - width / 2, center.y)
            path.quadraticBezierTo(
                center.x, center.y + height,
                center.x + width / 2, center.y
            )
        }
        "surprised" -> {
            // Surprised O shape
            val radius = width * 0.2f * (0.7f + mouthProgress * 0.3f)
            drawCircle(
                color = Color.Black,
                radius = radius,
                center = center
            )
            return
        }
        "serious" -> {
            // Serious straight line
            val height = width * 0.05f * mouthProgress
            path.moveTo(center.x - width / 2, center.y + height)
            path.lineTo(center.x + width / 2, center.y + height)
        }
        else -> {
            // Neutral relaxed mouth
            val height = width * 0.1f * (0.5f + mouthProgress * 0.5f)
            path.moveTo(center.x - width / 2, center.y)
            path.quadraticBezierTo(
                center.x, center.y + height,
                center.x + width / 2, center.y
            )
        }
    }
    
    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(width = 4f)
    )
}

/**
 * Draw eyebrows with emotional expression
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEyebrows(
    center: Offset,
    width: Float,
    emotion: String
) {
    val eyebrowPath = Path()
    
    when (emotion) {
        "happy" -> {
            // Happy raised eyebrows
            val height = width * 0.1f
            eyebrowPath.moveTo(center.x - width / 2, center.y - height)
            eyebrowPath.quadraticBezierTo(
                center.x, center.y - height * 2,
                center.x + width / 2, center.y - height
            )
        }
        "surprised" -> {
            // Surprised raised eyebrows
            val height = width * 0.15f
            eyebrowPath.moveTo(center.x - width / 2, center.y - height)
            eyebrowPath.quadraticBezierTo(
                center.x, center.y - height * 1.5f,
                center.x + width / 2, center.y - height
            )
        }
        "serious" -> {
            // Serious lowered eyebrows
            val height = width * 0.05f
            eyebrowPath.moveTo(center.x - width / 2, center.y)
            eyebrowPath.quadraticBezierTo(
                center.x, center.y + height,
                center.x + width / 2, center.y
            )
        }
        else -> {
            // Neutral eyebrows
            val height = width * 0.08f
            eyebrowPath.moveTo(center.x - width / 2, center.y - height / 2)
            eyebrowPath.quadraticBezierTo(
                center.x, center.y - height,
                center.x + width / 2, center.y - height / 2
            )
        }
    }
    
    drawPath(
        path = eyebrowPath,
        color = Color.Black,
        style = Stroke(width = 4f)
    )
}
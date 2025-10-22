package com.talkar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.BackendImage
import kotlinx.coroutines.*


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
    modifier: Modifier = Modifier
) {
    if (!isVisible || avatar == null || image == null) {
        return
    }

    var mouthProgress by remember { mutableStateOf(0f) }
    var blinkProgress by remember { mutableStateOf(0f) }
    val emotionState by rememberUpdatedState(emotion)
    
    val coroutineScope = rememberCoroutineScope()
    var mouthAnimationJob by remember { mutableStateOf<Job?>(null) }
    var blinkAnimationJob by remember { mutableStateOf<Job?>(null) }
    
    // Cancel previous jobs when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            mouthAnimationJob?.cancel()
            blinkAnimationJob?.cancel()
        }
    }
    
    // Animate mouth for talking
    LaunchedEffect(isTalking, emotionState) {
        mouthAnimationJob?.cancel()
        if (isTalking) {
            mouthAnimationJob = coroutineScope.launch {
                while (isActive && isTalking) {
                    mouthProgress = Math.random().toFloat()
                    delay(100)
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

    Card(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Avatar face with emotional expressions
            AvatarFace(
                emotion = emotionState,
                mouthProgress = mouthProgress,
                blinkProgress = blinkProgress,
                modifier = Modifier.size(150.dp)
            )
            
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
        }
    }
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
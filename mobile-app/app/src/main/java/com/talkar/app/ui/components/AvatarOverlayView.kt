package com.talkar.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar
import com.talkar.app.data.models.Dialogue
import kotlinx.coroutines.delay

/**
 * Avatar Overlay View for AR with Interactive Controls
 * Displays avatar overlay with tap interaction and script display
 */
@Composable
fun AvatarOverlayView(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    dialogue: Dialogue? = null,
    isPlaying: Boolean = false,
    videoUrl: String? = null,
    isLoadingVideo: Boolean = false,
    onAvatarTapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isVisible || avatar == null || image == null) {
        return
    }
    
    // Show loading placeholder if video is being fetched
    if (isLoadingVideo) {
        LoadingPlaceholder(
            modifier = modifier,
            showLoadingText = true
        )
        return
    }

    var showScript by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .size(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onAvatarTapped() }
            .background(MaterialTheme.colorScheme.surface)
            .border(
                2.dp,
                if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(16.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar Image/Video with Play/Pause Indicator
            Box(contentAlignment = Alignment.Center) {
                // Video player if video URL is available
                if (videoUrl != null) {
                    CompactVideoPlayer(
                        videoUrl = videoUrl,
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                } else if (isLoadingVideo) {
                    // Show compact loading placeholder while waiting for video
                    CompactLoadingPlaceholder(
                        modifier = Modifier.size(70.dp)
                    )
                } else {
                    // Fallback to avatar image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatar.avatarImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
                // Play/Pause indicator overlay
                Surface(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PlayArrow else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Playing" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Avatar Name
            Text(
                text = avatar.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Image Name
            Text(
                text = image.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isPlaying) "Playing" else "Paused",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPlaying) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline
                )
            }
            
            // Script button (if dialogue exists)
            if (dialogue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                
                OutlinedButton(
                    onClick = { showScript = !showScript },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (showScript) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Toggle Script",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showScript) "Hide Script" else "Show Script",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            // Animated Script Display
            AnimatedVisibility(
                visible = showScript && dialogue != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                dialogue?.let {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = it.text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 3,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simple Avatar Placeholder for testing
 */
@Composable
fun AvatarPlaceholder(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    Card(
        modifier = modifier
            .size(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Placeholder Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👤",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Avatar",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Detected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Avatar Overlay with Smooth Animations
 * Features: Fade in/out, scale, and bounce effects
 */
@Composable
fun AnimatedAvatarOverlay(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    dialogue: Dialogue? = null,
    isPlaying: Boolean = false,
    videoUrl: String? = null,
    isLoadingVideo: Boolean = false,
    onAvatarTapped: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Smooth scale animation with spring physics
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Smooth alpha animation
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )
    
    // Subtle rotation for dynamic feel
    val rotation by animateFloatAsState(
        targetValue = if (isVisible) 0f else -10f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )

    if (isVisible || alpha > 0.01f) {
        AvatarOverlayView(
            isVisible = true,
            avatar = avatar,
            image = image,
            dialogue = dialogue,
            isPlaying = isPlaying,
            videoUrl = videoUrl,
            isLoadingVideo = isLoadingVideo,
            onAvatarTapped = onAvatarTapped,
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    rotationZ = rotation
                }
        )
    }
}

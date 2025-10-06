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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import kotlinx.coroutines.delay

/**
 * Avatar Overlay View for AR
 * Displays a static avatar overlay on detected images
 */
@Composable
fun AvatarOverlayView(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    modifier: Modifier = Modifier
) {
    if (!isVisible || avatar == null || image == null) {
        return
    }

    Card(
        modifier = modifier
            .size(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                2.dp,
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(16.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatar.avatarImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Avatar Name
            Text(
                text = avatar.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Image Name
            Text(
                text = image.name,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AR Active",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
 * Avatar Overlay with Animation
 */
@Composable
fun AnimatedAvatarOverlay(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    modifier: Modifier = Modifier
) {
    var animationScale by remember { mutableStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            isAnimating = true
            animationScale = 0f
            repeat(10) {
                animationScale += 0.1f
                delay(50)
            }
            isAnimating = false
        } else {
            animationScale = 0f
            isAnimating = false
        }
    }

    if (isVisible && animationScale > 0f) {
        AvatarOverlayView(
            isVisible = true,
            avatar = avatar,
            image = image,
            modifier = modifier
        )
    }
}

package com.talkar.app.ui.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.talkar.app.data.models.BackendImage
import com.talkar.app.data.models.Avatar

/**
 * Avatar Overlay View with Feedback Buttons
 * Displays a static avatar overlay on detected images with thumbs up/down feedback options
 */
@Composable
fun FeedbackAvatarOverlay(
    isVisible: Boolean,
    avatar: Avatar?,
    image: BackendImage?,
    onFeedback: (Boolean) -> Unit, // true for thumbs up, false for thumbs down
    modifier: Modifier = Modifier
) {
    if (!isVisible || avatar == null || image == null) {
        return
    }

    Card(
        modifier = modifier
            .width(200.dp)
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
                .fillMaxWidth()
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

            Spacer(modifier = Modifier.height(12.dp))

            // Feedback Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbs Down Button
                OutlinedButton(
                    onClick = { onFeedback(false) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "👎",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Thumbs Up Button
                OutlinedButton(
                    onClick = { onFeedback(true) },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "👍",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
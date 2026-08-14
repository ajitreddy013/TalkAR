package com.talkar.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Deprecated legacy AR entry point.
 *
 * The active scan flow now lives in `TalkingPhotoScreen`.
 */
@Deprecated(
    message = "Use TalkingPhotoScreen instead",
    replaceWith = ReplaceWith("TalkingPhotoScreen()", "com.talkar.app.ui.screens.TalkingPhotoScreen"),
    level = DeprecationLevel.WARNING
)
@Composable
fun TalkARView(
    modifier: Modifier = Modifier,
    onImageDetected: (imageName: String) -> Unit = {},
    onImageLost: (imageName: String) -> Unit = {},
    onImageLongPressed: (imageName: String) -> Unit = {},
    videoUriToPlay: android.net.Uri? = null,
    onVideoCompleted: () -> Unit = {},
    onError: (error: String) -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Legacy AR flow disabled. Use TalkingPhotoScreen.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

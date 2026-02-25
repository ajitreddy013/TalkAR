package com.talkar.app.ui.components

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Composable that displays a video overlay using ExoPlayer's PlayerView.
 * 
 * @param player ExoPlayer instance
 * @param x X position in pixels
 * @param y Y position in pixels
 * @param width Width in pixels
 * @param height Height in pixels
 * @param visible Whether the overlay is visible
 */
@Composable
fun VideoOverlayView(
    player: ExoPlayer?,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    if (!visible || player == null) {
        Log.d("VideoOverlayView", "Not showing overlay: visible=$visible, player=$player")
        return
    }
    
    Log.d("VideoOverlayView", "Rendering overlay at x=$x, y=$y, w=$width, h=$height")
    
    Box(
        modifier = modifier
            .offset(
                x = with(density) { x.toDp() },
                y = with(density) { y.toDp() }
            )
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() }
            )
            .background(Color.Red.copy(alpha = 0.3f)) // Debug: red background to see the box
    ) {
        AndroidView(
            factory = { ctx ->
                Log.d("VideoOverlayView", "Creating PlayerView...")
                PlayerView(ctx).apply {
                    // Set player first
                    this.player = player
                    
                    // Configure PlayerView for video rendering
                    useController = false // Hide controls for AR experience
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Fit video to view
                    
                    // Enable hardware acceleration for video rendering
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                    
                    // Ensure proper layout
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Force visibility
                    alpha = 1f
                    visibility = android.view.View.VISIBLE
                    
                    // Keep screen on during video playback
                    keepScreenOn = true
                    
                    Log.d("VideoOverlayView", "PlayerView created and configured")
                    Log.d("VideoOverlayView", "  Player state: ${player?.playbackState}")
                    Log.d("VideoOverlayView", "  Is playing: ${player?.isPlaying}")
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.visibility = android.view.View.VISIBLE
                // Force a layout pass
                playerView.requestLayout()
                Log.d("VideoOverlayView", "PlayerView updated with player")
                Log.d("VideoOverlayView", "  Player state: ${player?.playbackState}")
                Log.d("VideoOverlayView", "  Is playing: ${player?.isPlaying}")
            }
        )
    }
    
    DisposableEffect(player) {
        onDispose {
            // PlayerView cleanup is handled by the player itself
            Log.d("VideoOverlayView", "PlayerView disposed")
        }
    }
}

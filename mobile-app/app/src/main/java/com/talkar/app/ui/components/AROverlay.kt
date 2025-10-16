package com.talkar.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import android.widget.VideoView
import com.google.ar.core.AugmentedImage
import com.talkar.app.data.models.SyncResponse
import com.talkar.app.data.models.TalkingHeadVideo

@Composable
fun AROverlay(
    recognizedImage: AugmentedImage?,
    talkingHeadVideo: TalkingHeadVideo?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    if (recognizedImage != null && talkingHeadVideo != null) {
        // Create AR overlay positioned over the recognized image
        AndroidView(
            factory = { ctx ->
                createAROverlayView(ctx, recognizedImage, talkingHeadVideo)
            },
            modifier = modifier
        )
    }
}

private fun createAROverlayView(
    context: Context,
    augmentedImage: AugmentedImage,
    talkingHeadVideo: TalkingHeadVideo
): android.view.View {
    return android.widget.FrameLayout(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Create video overlay positioned over the recognized image
        val videoView = VideoView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setVideoPath(talkingHeadVideo.videoUrl)
            setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayer.start()
            }
        }
        
        addView(videoView)
        
        // Position the video overlay over the recognized image
        // This uses ARCore's image tracking to position the overlay
        positionOverlayOnImage(videoView, augmentedImage)
    }
}

private fun positionOverlayOnImage(
    videoView: VideoView,
    augmentedImage: AugmentedImage
) {
    // Get the image's center pose and dimensions
    val imageCenter = augmentedImage.centerPose
    val imageExtentX = augmentedImage.extentX
    val imageExtentZ = augmentedImage.extentZ
    
    // Calculate overlay position and size based on image dimensions
    // This ensures the talking head appears over the recognized image
    val overlayWidth = (imageExtentX * 1000).toInt() // Convert to pixels
    val overlayHeight = (imageExtentZ * 1000).toInt()
    
    videoView.layoutParams = ViewGroup.LayoutParams(overlayWidth, overlayHeight)
    
    // Position the overlay at the image center
    val centerX = (imageCenter.tx() * 1000).toInt()
    val centerY = (imageCenter.ty() * 1000).toInt()
    
    videoView.x = centerX - overlayWidth / 2f
    videoView.y = centerY - overlayHeight / 2f
}

@Composable
fun ARVideoOverlay(
    videoUrl: String,
    isVisible: Boolean,
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    
    // Control video playback
    LaunchedEffect(isPlaying, videoViewRef) {
        videoViewRef?.let { videoView ->
            if (isPlaying) {
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            } else {
                if (videoView.isPlaying) {
                    videoView.pause()
                }
            }
        }
    }
    
    if (isVisible) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Talking Head Overlay",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                300
                            )
                            setVideoPath(videoUrl)
                            setOnPreparedListener { mediaPlayer ->
                                mediaPlayer.isLooping = true
                                videoViewRef = this
                                if (isPlaying) {
                                    mediaPlayer.start()
                                }
                            }
                        }
                    },
                    update = { videoView ->
                        videoViewRef = videoView
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPlayPause,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "Pause" else "Play", color = Color.White)
                    }
                    
                    OutlinedButton(
                        onClick = { 
                            videoViewRef?.seekTo(0)
                            if (isPlaying) {
                                videoViewRef?.start()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = "Restart"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restart")
                    }
                }
            }
        }
    }
}

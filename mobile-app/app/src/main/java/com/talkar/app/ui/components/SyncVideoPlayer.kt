package com.talkar.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import android.widget.VideoView

@Composable
fun SyncVideoPlayer(
    videoUrl: String,
    isPlaying: Boolean = false,
    onPlayPause: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    
    // Control video playback based on isPlaying state
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
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Generated Talking Head",
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
                                start()
                            }
                        }
                        setOnErrorListener { _, what, extra ->
                            android.util.Log.e("SyncVideoPlayer", "Video error: what=$what, extra=$extra")
                            true
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
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPlaying) "Pause" else "Play")
                }
                
                OutlinedButton(
                    onClick = { 
                        videoViewRef?.let { videoView ->
                            videoView.seekTo(0)
                            if (isPlaying) {
                                videoView.start()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Restart"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restart")
                }
            }
        }
    }
}


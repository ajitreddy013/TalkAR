package com.talkar.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.common.C
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource

@Composable
fun SyncVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val exoPlayer = remember { 
        ExoPlayer.Builder(LocalContext.current).apply {
            // Configure load control for better buffering
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    2000, // min buffer - reduced for faster startup
                    5000, // max buffer - optimized for mobile
                    1500, // playback start - faster startup
                    2000  // rebuffer - reduced for better UX
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            setLoadControl(loadControl)
        }.build()
    }
    
    // Prepare media item
    DisposableEffect(videoUrl) {
        val mediaItem = MediaItem.fromUri(videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        
        onDispose {
            exoPlayer.release()
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
                    PlayerView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            300
                        )
                        player = exoPlayer
                        useController = false // We'll provide our own controls
                    }
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
                    onClick = { 
                        exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (exoPlayer.playWhenReady) "Pause" else "Play")
                }
                
                OutlinedButton(
                    onClick = { 
                        exoPlayer.seekTo(0)
                        exoPlayer.playWhenReady = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restart")
                }
            }
        }
    }
}


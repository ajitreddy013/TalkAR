package com.talkar.app.ui.components

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

@Composable
fun VideoOverlayView(
    videoSource: Any, // Can be Int (ResId) or String (URL)
    arService: com.talkar.app.data.services.ConversationalARService, // 🔥 SHARED SERVICE
    onVideoCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(true) }
    
    val videoUri = remember(videoSource) {
        when (videoSource) {
            is Int -> Uri.parse("android.resource://${context.packageName}/$videoSource")
            is String -> Uri.parse(videoSource)
            else -> Uri.EMPTY
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                android.view.SurfaceView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    holder.addCallback(object : android.view.SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                            // Link shared MediaPlayer to this surface
                            val mp = arService.mediaPlayer ?: android.media.MediaPlayer().also { 
                                arService.mediaPlayer = it 
                            }
                            
                            try {
                                mp.reset()
                                mp.setDisplay(holder)
                                mp.setDataSource(ctx, videoUri)
                                mp.setOnPreparedListener { 
                                    it.start()
                                    isBuffering = false
                                }
                                mp.setOnCompletionListener { onVideoCompleted() }
                                mp.setOnErrorListener { _, _, _ -> 
                                    onVideoCompleted()
                                    true
                                }
                                mp.prepareAsync()
                            } catch (e: Exception) {
                                android.util.Log.e("VideoOverlay", "MediaPlayer error", e)
                                onVideoCompleted()
                            }
                        }

                        override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
                        override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                            // Reset display but don't release singleton
                            arService.mediaPlayer?.setDisplay(null)
                        }
                    })
                }
            },
            update = { /* Surface callback handles updates */ },
            modifier = Modifier.fillMaxSize()
        )
        
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                color = Color.White
            )
        }
    }
}

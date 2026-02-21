package com.talkar.app.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Video overlay that displays video content using WebView.
 * 
 * This is a 2D overlay positioned over the AR view that plays video content.
 * While not true 3D, it provides immediate video playback functionality.
 * 
 * @param videoUri URI of the video to play
 * @param onCompleted Callback when video playback completes
 * @param modifier Compose modifier
 */
@Composable
fun VideoOverlay(
    videoUri: Uri?,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    if (videoUri == null) {
        return
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.apply {
                        javaScriptEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                    }
                    
                    setBackgroundColor(0x00000000) // Transparent background
                    
                    webChromeClient = WebChromeClient()
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Auto-play video
                            view?.evaluateJavascript(
                                """
                                var video = document.querySelector('video');
                                if (video) {
                                    video.play();
                                }
                                """.trimIndent(),
                                null
                            )
                        }
                    }
                    
                    // Add JavaScript interface for video completion callback
                    addJavascriptInterface(object {
                        @android.webkit.JavascriptInterface
                        fun onVideoEnded() {
                            onCompleted()
                        }
                    }, "Android")
                    
                    // Load HTML with video
                    val html = createVideoHtml(videoUri)
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
    
    DisposableEffect(videoUri) {
        onDispose {
            // Cleanup handled by WebView
        }
    }
}

/**
 * Creates HTML content for video playback.
 */
private fun createVideoHtml(videoUri: Uri): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    background: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    overflow: hidden;
                }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                }
            </style>
        </head>
        <body>
            <video 
                id="videoPlayer"
                src="$videoUri" 
                autoplay 
                playsinline
                onended="Android.onVideoEnded()">
            </video>
            <script>
                // Ensure video plays
                var video = document.getElementById('videoPlayer');
                video.addEventListener('canplay', function() {
                    video.play().catch(function(error) {
                        console.log('Autoplay failed:', error);
                    });
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}

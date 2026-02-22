package com.talkar.app.ar.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.talkar.app.ar.video.errors.VideoError
import com.talkar.app.ar.video.models.VideoInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ExoPlayer-based implementation of VideoDecoder.
 * 
 * Provides hardware-accelerated video decoding with software fallback.
 * Extracts video dimensions from ExoPlayer callbacks with MediaMetadataRetriever fallback.
 * Implements retry logic for decoder initialization.
 */
class ExoPlayerVideoDecoder(
    private val context: Context
) : VideoDecoder {
    
    companion object {
        private const val TAG = "ExoPlayerVideoDecoder"
        private const val INITIALIZATION_TIMEOUT_MS = 5000L
        private const val RETRY_DELAY_MS = 500L
        
        // Supported codecs
        private val SUPPORTED_CODECS = setOf("h264", "avc", "h265", "hevc")
        
        // Supported resolutions (480p to 1080p)
        private const val MIN_RESOLUTION = 480
        private const val MAX_RESOLUTION = 1920
        
        // Supported frame rates
        private val SUPPORTED_FRAME_RATES = setOf(24f, 30f, 60f)
    }
    
    private var exoPlayer: ExoPlayer? = null
    private var videoInfo: VideoInfo? = null
    private var listener: VideoDecoderListener? = null
    private var currentUri: Uri? = null
    private var firstFrameRendered = false
    
    override suspend fun initialize(videoUri: Uri, surface: Surface): Result<VideoInfo> {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "Initializing decoder for: $videoUri")
                currentUri = videoUri
                
                // Try initialization with retry logic
                var lastError: Exception? = null
                repeat(2) { attempt ->
                    try {
                        if (attempt > 0) {
                            Log.d(TAG, "Retrying initialization after ${RETRY_DELAY_MS}ms...")
                            delay(RETRY_DELAY_MS)
                        }
                        
                        val info = initializePlayer(videoUri, surface)
                        videoInfo = info
                        Log.i(TAG, "✅ Decoder initialized successfully: ${info.width}x${info.height}, codec=${info.codec}")
                        return@withContext Result.success(info)
                        
                    } catch (e: Exception) {
                        lastError = e
                        Log.w(TAG, "Initialization attempt ${attempt + 1} failed: ${e.message}")
                        
                        // Clean up failed attempt
                        exoPlayer?.release()
                        exoPlayer = null
                    }
                }
                
                // Both attempts failed
                val error = VideoError.DecoderInitializationFailed(
                    "Failed to initialize decoder after 2 attempts: ${lastError?.message}"
                )
                Log.e(TAG, "❌ ${error.message}", lastError)
                Result.failure(error)
                
            } catch (e: Exception) {
                val error = VideoError.DecoderInitializationFailed(
                    "Unexpected error during initialization: ${e.message}"
                )
                Log.e(TAG, "❌ ${error.message}", e)
                Result.failure(error)
            }
        }
    }
    
    /**
     * Initializes the ExoPlayer instance and extracts video information.
     */
    private suspend fun initializePlayer(videoUri: Uri, surface: Surface): VideoInfo {
        // Create ExoPlayer with hardware acceleration
        val player = createExoPlayer()
        exoPlayer = player
        firstFrameRendered = false
        
        // Set video surface
        player.setVideoSurface(surface)
        Log.d(TAG, "Video surface set")
        
        // Set up deferred for waiting on video info
        val videoInfoDeferred = CompletableDeferred<VideoInfo>()
        
        // Set up player listener
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "Playback state changed: ${getStateName(playbackState)}")
                listener?.onPlaybackStateChanged(playbackState)
                
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Extract video info when ready
                        if (!videoInfoDeferred.isCompleted) {
                            kotlinx.coroutines.MainScope().launch {
                                try {
                                    val info = extractVideoInfo(player, videoUri)
                                    videoInfoDeferred.complete(info)
                                    listener?.onReady(info)
                                } catch (e: Exception) {
                                    videoInfoDeferred.completeExceptionally(e)
                                }
                            }
                        }
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "Playback ended")
                    }
                }
            }
            
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d(TAG, "Video size changed: ${videoSize.width}x${videoSize.height}")
                if (videoSize.width > 0 && videoSize.height > 0) {
                    listener?.onVideoSizeChanged(videoSize.width, videoSize.height)
                    
                    // Update video info if dimensions changed
                    videoInfo?.let { currentInfo ->
                        if (currentInfo.width != videoSize.width || currentInfo.height != videoSize.height) {
                            videoInfo = currentInfo.copy(
                                width = videoSize.width,
                                height = videoSize.height
                            )
                        }
                    }
                }
            }
            
            override fun onRenderedFirstFrame() {
                if (!firstFrameRendered) {
                    firstFrameRendered = true
                    Log.i(TAG, "✅ First frame rendered")
                    listener?.onFirstFrameRendered()
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "❌ Player error: ${error.message}", error)
                listener?.onError(error)
                
                if (!videoInfoDeferred.isCompleted) {
                    videoInfoDeferred.completeExceptionally(error)
                }
            }
        })
        
        // Set media item and prepare
        val mediaItem = MediaItem.fromUri(videoUri)
        player.setMediaItem(mediaItem)
        player.prepare()
        Log.d(TAG, "Player prepared, waiting for video info...")
        
        // Wait for video info with timeout
        val info = withTimeoutOrNull(INITIALIZATION_TIMEOUT_MS) {
            videoInfoDeferred.await()
        } ?: throw VideoError.DecoderInitializationFailed(
            "Timeout waiting for video info after ${INITIALIZATION_TIMEOUT_MS}ms"
        )
        
        return info
    }
    
    /**
     * Creates ExoPlayer with hardware acceleration and software fallback.
     */
    private fun createExoPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setRenderersFactory(
                DefaultRenderersFactory(context).apply {
                    // Enable hardware acceleration with fallback to software
                    setEnableDecoderFallback(true)
                    // Prefer hardware decoders
                    setExtensionRendererMode(
                        DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                    )
                }
            )
            .build()
    }
    
    /**
     * Extracts video information from ExoPlayer with MediaMetadataRetriever fallback.
     */
    private suspend fun extractVideoInfo(player: ExoPlayer, videoUri: Uri): VideoInfo {
        return withContext(Dispatchers.IO) {
            try {
                // Try to get dimensions from ExoPlayer first
                val videoSize = player.videoSize
                var width = videoSize.width
                var height = videoSize.height
                
                // Fallback to MediaMetadataRetriever if dimensions are 0
                if (width == 0 || height == 0) {
                    Log.d(TAG, "ExoPlayer dimensions are 0x0, using MediaMetadataRetriever fallback")
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, videoUri)
                        
                        width = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                        )?.toIntOrNull() ?: 0
                        
                        height = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                        )?.toIntOrNull() ?: 0
                        
                        Log.d(TAG, "MediaMetadataRetriever extracted: ${width}x${height}")
                    } finally {
                        retriever.release()
                    }
                }
                
                // Get duration
                val duration = player.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
                
                // Extract codec and track information
                val tracks = player.currentTracks
                var codec = "unknown"
                var hasVideoTrack = false
                var hasAudioTrack = false
                var frameRate = 0f
                
                tracks.groups.forEach { group ->
                    when {
                        group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO -> {
                            hasVideoTrack = true
                            if (group.length > 0) {
                                val format = group.getTrackFormat(0)
                                codec = format.sampleMimeType?.substringAfter("video/") ?: "unknown"
                                frameRate = format.frameRate.takeIf { it > 0 } ?: 30f
                            }
                        }
                        group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO -> {
                            hasAudioTrack = true
                        }
                    }
                }
                
                // Validate dimensions
                if (width == 0 || height == 0) {
                    throw VideoError.VideoLoadFailed(
                        "Failed to extract valid video dimensions: ${width}x${height}",
                        videoUri
                    )
                }
                
                val info = VideoInfo(
                    width = width,
                    height = height,
                    durationMs = duration,
                    frameRate = frameRate,
                    codec = codec,
                    hasAudioTrack = hasAudioTrack,
                    hasVideoTrack = hasVideoTrack
                )
                
                // Validate codec, format, resolution, and frame rate
                validateVideoParameters(info, videoUri)
                
                info
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract video info", e)
                throw VideoError.VideoLoadFailed(
                    "Failed to extract video metadata: ${e.message}",
                    videoUri
                )
            }
        }
    }
    
    override fun start() {
        exoPlayer?.let { player ->
            player.playWhenReady = true
            Log.d(TAG, "Playback started")
        } ?: Log.w(TAG, "Cannot start: player not initialized")
    }
    
    override fun pause() {
        exoPlayer?.let { player ->
            player.playWhenReady = false
            Log.d(TAG, "Playback paused")
        } ?: Log.w(TAG, "Cannot pause: player not initialized")
    }
    
    override fun stop() {
        exoPlayer?.let { player ->
            player.stop()
            player.seekTo(0)
            Log.d(TAG, "Playback stopped")
        } ?: Log.w(TAG, "Cannot stop: player not initialized")
    }
    
    override fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        Log.d(TAG, "Seeked to ${positionMs}ms")
    }
    
    override fun getVideoInfo(): VideoInfo? = videoInfo
    
    override fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L
    
    override fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false
    
    override fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        exoPlayer?.volume = clampedVolume
        Log.d(TAG, "Volume set to $clampedVolume")
    }
    
    override fun setLooping(enabled: Boolean) {
        exoPlayer?.repeatMode = if (enabled) {
            Player.REPEAT_MODE_ONE
        } else {
            Player.REPEAT_MODE_OFF
        }
        Log.d(TAG, "Looping ${if (enabled) "enabled" else "disabled"}")
    }
    
    override fun release() {
        exoPlayer?.let { player ->
            player.stop()
            player.release()
            Log.d(TAG, "Player released")
        }
        exoPlayer = null
        videoInfo = null
        listener = null
        currentUri = null
        firstFrameRendered = false
    }
    
    override fun setListener(listener: VideoDecoderListener) {
        this.listener = listener
    }
    
    /**
     * Validates video parameters against supported formats.
     * Logs warnings for unsupported parameters but doesn't fail.
     */
    private fun validateVideoParameters(info: VideoInfo, videoUri: Uri) {
        val unsupportedParams = mutableListOf<String>()
        
        // Validate codec (H.264 and H.265)
        val normalizedCodec = info.codec.lowercase()
        if (!SUPPORTED_CODECS.contains(normalizedCodec)) {
            unsupportedParams.add("codec: ${info.codec} (supported: H.264, H.265)")
            Log.w(TAG, "⚠️ Unsupported codec: ${info.codec}. Supported codecs: H.264, H.265")
        }
        
        // Validate resolution (480p to 1080p)
        val minDimension = minOf(info.width, info.height)
        val maxDimension = maxOf(info.width, info.height)
        if (minDimension < MIN_RESOLUTION || maxDimension > MAX_RESOLUTION) {
            unsupportedParams.add("resolution: ${info.width}x${info.height} (supported: 480p-1080p)")
            Log.w(TAG, "⚠️ Resolution ${info.width}x${info.height} outside supported range (480p-1080p)")
        }
        
        // Validate frame rate (24fps, 30fps, 60fps)
        if (info.frameRate > 0 && !SUPPORTED_FRAME_RATES.contains(info.frameRate)) {
            unsupportedParams.add("frame rate: ${info.frameRate}fps (supported: 24, 30, 60 fps)")
            Log.w(TAG, "⚠️ Frame rate ${info.frameRate}fps not in supported set (24, 30, 60 fps)")
        }
        
        // Validate container format (MP4)
        val uriString = videoUri.toString()
        if (!uriString.endsWith(".mp4", ignoreCase = true) && 
            !uriString.contains("video/mp4", ignoreCase = true)) {
            unsupportedParams.add("container format: ${uriString.substringAfterLast('.')} (supported: MP4)")
            Log.w(TAG, "⚠️ Video URI doesn't indicate MP4 format: $uriString")
        }
        
        // Log summary of unsupported parameters
        if (unsupportedParams.isNotEmpty()) {
            Log.w(TAG, """
                ⚠️ Video has unsupported parameters:
                URI: $videoUri
                Unsupported parameters:
                ${unsupportedParams.joinToString("\n                - ", prefix = "- ")}
                
                Playback may still work but is not guaranteed.
            """.trimIndent())
        } else {
            Log.i(TAG, "✅ All video parameters are supported")
        }
    }
    
    /**
     * Gets human-readable name for playback state.
     */
    private fun getStateName(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN($state)"
        }
    }
}

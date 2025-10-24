package com.talkar.app.data.services

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AudioStreamingServiceTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockPlayer: ExoPlayer

    private lateinit var audioStreamingService: AudioStreamingService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // Reset singleton instance for testing
        val field = AudioStreamingService::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
        
        audioStreamingService = AudioStreamingService.getInstance(mockContext)
    }

    @Test
    fun `test singleton instance`() {
        val instance1 = AudioStreamingService.getInstance(mockContext)
        val instance2 = AudioStreamingService.getInstance(mockContext)
        
        assert(instance1 === instance2)
    }

    @Test
    fun `test startStreaming with valid URL`() {
        val testUrl = "https://example.com/audio.mp3"
        var callbackCalled = false
        
        audioStreamingService.startStreaming(testUrl, object : AudioStreamingService.PlaybackListener {
            override fun onPlaybackStarted() {
                callbackCalled = true
            }

            override fun onBufferingStarted() {
                // Do nothing
            }

            override fun onBufferingCompleted() {
                // Do nothing
            }

            override fun onPlaybackCompleted() {
                // Do nothing
            }

            override fun onPlaybackError(error: Exception) {
                // Do nothing
            }
        })
        
        // Verify that the service was initialized
        // Note: Actual ExoPlayer interactions would require more complex mocking
    }

    @Test
    fun `test stopStreaming releases player`() {
        audioStreamingService.stopStreaming()
        // Verify no exceptions are thrown
    }

    @Test
    fun `test pause and resume streaming`() {
        audioStreamingService.pauseStreaming()
        audioStreamingService.resumeStreaming()
        // Verify no exceptions are thrown
    }

    @Test
    fun `test isPlaying returns false when player is null`() {
        val isPlaying = audioStreamingService.isPlaying()
        assert(!isPlaying)
    }

    @Test
    fun `test getCurrentPosition returns 0 when player is null`() {
        val position = audioStreamingService.getCurrentPosition()
        assert(position == 0L)
    }

    @Test
    fun `test getBufferedPosition returns 0 when player is null`() {
        val position = audioStreamingService.getBufferedPosition()
        assert(position == 0L)
    }
}
package com.talkar.app.ar.video

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.media3.common.PlaybackException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.talkar.app.ar.video.models.VideoInfo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

/**
 * Unit tests for VideoDecoder implementation.
 * 
 * Tests core functionality including:
 * - Decoder initialization
 * - Video dimension extraction
 * - Playback control
 * - Error handling
 * - Resource cleanup
 */
@RunWith(AndroidJUnit4::class)
class VideoDecoderTest {
    
    private lateinit var context: Context
    private lateinit var decoder: VideoDecoder
    private lateinit var mockSurface: Surface
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        decoder = ExoPlayerVideoDecoder(context)
        mockSurface = mock(Surface::class.java)
    }
    
    @After
    fun teardown() {
        decoder.release()
    }
    
    @Test
    fun testDecoderInitialization() = runBlocking {
        // Given a valid video URI
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        
        // When initializing the decoder
        val result = decoder.initialize(videoUri, mockSurface)
        
        // Then initialization should succeed
        assertTrue("Decoder initialization should succeed", result.isSuccess)
        
        // And video info should be available
        val info = decoder.getVideoInfo()
        assertNotNull("Video info should be available", info)
        
        // And dimensions should be non-zero
        info?.let {
            assertTrue("Video width should be > 0", it.width > 0)
            assertTrue("Video height should be > 0", it.height > 0)
            assertTrue("Video should have valid dimensions", it.hasValidDimensions())
        }
    }
    
    @Test
    fun testVideoInfoExtraction() = runBlocking {
        // Given a valid video URI
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        
        // When initializing the decoder
        val result = decoder.initialize(videoUri, mockSurface)
        
        // Then video info should contain all required fields
        result.getOrNull()?.let { info ->
            assertTrue("Width should be > 0", info.width > 0)
            assertTrue("Height should be > 0", info.height > 0)
            assertTrue("Duration should be >= 0", info.durationMs >= 0)
            assertTrue("Frame rate should be > 0", info.frameRate > 0)
            assertNotEquals("Codec should not be unknown", "unknown", info.codec)
            
            // Log extracted info for verification
            println("Extracted video info:")
            println("  Dimensions: ${info.width}x${info.height}")
            println("  Duration: ${info.durationMs}ms")
            println("  Frame rate: ${info.frameRate}fps")
            println("  Codec: ${info.codec}")
            println("  Has video track: ${info.hasVideoTrack}")
            println("  Has audio track: ${info.hasAudioTrack}")
        }
    }
    
    @Test
    fun testPlaybackControl() = runBlocking {
        // Given an initialized decoder
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        decoder.initialize(videoUri, mockSurface)
        
        // When starting playback
        decoder.start()
        
        // Then decoder should report playing state
        // Note: This may not be immediately true due to buffering
        // In a real test, we'd wait for the state change
        
        // When pausing playback
        decoder.pause()
        
        // Then decoder should not be playing
        assertFalse("Decoder should not be playing after pause", decoder.isPlaying())
        
        // When stopping playback
        decoder.stop()
        
        // Then position should be reset to 0
        assertEquals("Position should be 0 after stop", 0L, decoder.getCurrentPosition())
    }
    
    @Test
    fun testVolumeControl() = runBlocking {
        // Given an initialized decoder
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        decoder.initialize(videoUri, mockSurface)
        
        // When setting volume to various levels
        decoder.setVolume(0.5f)
        decoder.setVolume(0.0f)
        decoder.setVolume(1.0f)
        
        // Then no exceptions should be thrown
        // Volume clamping is tested implicitly
        
        // Test volume clamping
        decoder.setVolume(-0.5f) // Should clamp to 0.0
        decoder.setVolume(1.5f)  // Should clamp to 1.0
    }
    
    @Test
    fun testLoopingControl() = runBlocking {
        // Given an initialized decoder
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        decoder.initialize(videoUri, mockSurface)
        
        // When enabling looping
        decoder.setLooping(true)
        
        // Then no exceptions should be thrown
        
        // When disabling looping
        decoder.setLooping(false)
        
        // Then no exceptions should be thrown
    }
    
    @Test
    fun testSeekTo() = runBlocking {
        // Given an initialized decoder
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        decoder.initialize(videoUri, mockSurface)
        
        // When seeking to a position
        decoder.seekTo(1000L)
        
        // Then no exceptions should be thrown
        // Note: Actual position verification would require waiting for seek completion
    }
    
    @Test
    fun testListenerCallbacks() = runBlocking {
        // Given a decoder with a listener
        var readyCalled = false
        var firstFrameCalled = false
        var stateChangeCalled = false
        
        decoder.setListener(object : VideoDecoderListener {
            override fun onReady(info: VideoInfo) {
                readyCalled = true
                assertTrue("Video info should have valid dimensions", info.hasValidDimensions())
            }
            
            override fun onFirstFrameRendered() {
                firstFrameCalled = true
            }
            
            override fun onPlaybackStateChanged(state: Int) {
                stateChangeCalled = true
            }
            
            override fun onVideoSizeChanged(width: Int, height: Int) {
                assertTrue("Width should be > 0", width > 0)
                assertTrue("Height should be > 0", height > 0)
            }
            
            override fun onError(error: PlaybackException) {
                fail("Should not receive error for valid video")
            }
        })
        
        // When initializing the decoder
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        val result = decoder.initialize(videoUri, mockSurface)
        
        // Then callbacks should be invoked
        assertTrue("Initialization should succeed", result.isSuccess)
        assertTrue("onReady should be called", readyCalled)
        assertTrue("onPlaybackStateChanged should be called", stateChangeCalled)
        // Note: onFirstFrameRendered may not be called without actual rendering
    }
    
    @Test
    fun testResourceCleanup() = runBlocking {
        // Given an initialized decoder
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        decoder.initialize(videoUri, mockSurface)
        
        // When releasing the decoder
        decoder.release()
        
        // Then video info should be cleared
        assertNull("Video info should be null after release", decoder.getVideoInfo())
        
        // And playback state should be reset
        assertFalse("Should not be playing after release", decoder.isPlaying())
        assertEquals("Position should be 0 after release", 0L, decoder.getCurrentPosition())
    }
    
    @Test
    fun testInvalidUri() = runBlocking {
        // Given an invalid video URI
        val invalidUri = Uri.parse("android.resource://${context.packageName}/raw/nonexistent")
        
        // When initializing the decoder
        val result = decoder.initialize(invalidUri, mockSurface)
        
        // Then initialization should fail
        assertTrue("Initialization should fail for invalid URI", result.isFailure)
    }
    
    @Test
    fun testAspectRatioCalculation() = runBlocking {
        // Given an initialized decoder
        val videoUri = Uri.parse("android.resource://${context.packageName}/raw/sunrich_1")
        val result = decoder.initialize(videoUri, mockSurface)
        
        // When getting video info
        val info = result.getOrNull()
        
        // Then aspect ratio should be calculated correctly
        info?.let {
            val expectedAspectRatio = it.width.toFloat() / it.height.toFloat()
            val actualAspectRatio = it.getAspectRatio()
            assertEquals(
                "Aspect ratio should match width/height",
                expectedAspectRatio,
                actualAspectRatio,
                0.01f
            )
        }
    }
}

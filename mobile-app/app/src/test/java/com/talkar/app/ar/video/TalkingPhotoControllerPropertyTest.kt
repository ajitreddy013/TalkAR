package com.talkar.app.ar.video

import android.content.Context
import android.net.Uri
import android.view.Surface
import com.google.ar.core.Anchor
import com.talkar.app.ar.video.backend.BackendVideoFetcher
import com.talkar.app.ar.video.cache.VideoCache
import com.talkar.app.ar.video.models.GenerateResponse
import com.talkar.app.ar.video.models.LipCoordinates
import com.talkar.app.ar.video.models.LipCoordinatesDto
import com.talkar.app.ar.video.models.StatusResponse
import com.talkar.app.ar.video.models.TalkingPhotoRequest
import com.talkar.app.ar.video.models.VideoInfo
import com.talkar.app.ar.video.rendering.LipRegionRenderer
import com.talkar.app.ar.video.rendering.RenderCoordinator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking

/**
 * Property-based tests for TalkingPhotoController.
 * 
 * **Validates: Requirements 2.1**
 * 
 * Property 3: Backend Request on Detection
 * - When a poster is detected, a backend API request must be initiated
 * - The request must include the poster ID
 * - This must happen for all detected posters
 */
class TalkingPhotoControllerPropertyTest : StringSpec({
    
    "Property 3: Backend request is triggered on poster detection" {
        checkAll(10, Arb.string(10..20)) { posterId ->
            // Given: Mock dependencies
            val context = mockk<Context>(relaxed = true)
            val backendFetcher = mockk<BackendVideoFetcher>(relaxed = true)
            val videoCache = mockk<VideoCache>(relaxed = true)
            val videoDecoder = mockk<VideoDecoder>(relaxed = true)
            val lipRenderer = mockk<LipRegionRenderer>(relaxed = true)
            val renderCoordinator = mockk<RenderCoordinator>(relaxed = true)
            val anchor = mockk<Anchor>(relaxed = true)
            
            // Setup cache to return null (cache miss)
            coEvery { videoCache.retrieve(any()) } returns null
            
            // Setup backend to return success
            coEvery { backendFetcher.generateLipSync(any()) } returns Result.success("video_123")
            coEvery { backendFetcher.checkStatus(any()) } returns Result.success(
                StatusResponse(
                    videoId = "video_123",
                    status = "complete",
                    progress = 1.0f,
                    videoUrl = "https://example.com/video.mp4",
                    lipCoordinates = LipCoordinatesDto(0.4f, 0.5f, 0.2f, 0.1f),
                    checksum = "abc123"
                )
            )
            coEvery { backendFetcher.downloadVideo(any(), any(), any()) } returns Result.success("/cache/video.mp4")
            
            // Setup video decoder
            every { lipRenderer.getSurface() } returns mockk<Surface>(relaxed = true)
            coEvery { videoDecoder.initialize(any<Uri>(), any()) } returns Result.success(
                VideoInfo(
                    width = 1920,
                    height = 1080,
                    durationMs = 5000,
                    frameRate = 30f,
                    codec = "h264"
                )
            )
            
            // Setup cache directory
            val cacheDir = mockk<java.io.File>(relaxed = true)
            every { context.cacheDir } returns cacheDir
            every { cacheDir.exists() } returns true
            
            // When: Initialize controller with detected poster
            val controller = TalkingPhotoControllerImpl(
                context = context,
                backendFetcher = backendFetcher,
                videoCache = videoCache,
                videoDecoder = videoDecoder,
                lipRenderer = lipRenderer,
                renderCoordinator = renderCoordinator
            )
            
            runBlocking {
                controller.initialize(anchor, posterId)
            }
            
            // Then: Backend request must be triggered
            coVerify(exactly = 1) {
                backendFetcher.generateLipSync(
                    match { request ->
                        request.posterId == posterId
                    }
                )
            }
        }
    }
    
    "Property 3: Backend request includes poster ID" {
        checkAll(10, Arb.string(10..20)) { posterId ->
            // Given: Mock dependencies
            val context = mockk<Context>(relaxed = true)
            val backendFetcher = mockk<BackendVideoFetcher>(relaxed = true)
            val videoCache = mockk<VideoCache>(relaxed = true)
            val videoDecoder = mockk<VideoDecoder>(relaxed = true)
            val lipRenderer = mockk<LipRegionRenderer>(relaxed = true)
            val renderCoordinator = mockk<RenderCoordinator>(relaxed = true)
            val anchor = mockk<Anchor>(relaxed = true)
            
            var capturedRequest: TalkingPhotoRequest? = null
            
            // Setup cache to return null (cache miss)
            coEvery { videoCache.retrieve(any()) } returns null
            
            // Setup backend to capture request
            coEvery { backendFetcher.generateLipSync(any()) } answers {
                capturedRequest = firstArg()
                Result.success("video_123")
            }
            coEvery { backendFetcher.checkStatus(any()) } returns Result.success(
                StatusResponse(
                    videoId = "video_123",
                    status = "complete",
                    progress = 1.0f,
                    videoUrl = "https://example.com/video.mp4",
                    lipCoordinates = LipCoordinatesDto(0.4f, 0.5f, 0.2f, 0.1f),
                    checksum = "abc123"
                )
            )
            coEvery { backendFetcher.downloadVideo(any(), any(), any()) } returns Result.success("/cache/video.mp4")
            
            // Setup video decoder
            every { lipRenderer.getSurface() } returns mockk<Surface>(relaxed = true)
            coEvery { videoDecoder.initialize(any<Uri>(), any()) } returns Result.success(
                VideoInfo(
                    width = 1920,
                    height = 1080,
                    durationMs = 5000,
                    frameRate = 30f,
                    codec = "h264"
                )
            )
            
            // Setup cache directory
            val cacheDir = mockk<java.io.File>(relaxed = true)
            every { context.cacheDir } returns cacheDir
            every { cacheDir.exists() } returns true
            
            // When: Initialize controller with detected poster
            val controller = TalkingPhotoControllerImpl(
                context = context,
                backendFetcher = backendFetcher,
                videoCache = videoCache,
                videoDecoder = videoDecoder,
                lipRenderer = lipRenderer,
                renderCoordinator = renderCoordinator
            )
            
            runBlocking {
                controller.initialize(anchor, posterId)
            }
            
            // Then: Request must include the poster ID
            capturedRequest shouldBe TalkingPhotoRequest(
                posterId = posterId,
                text = "Hello! Welcome to TalkAR.",
                voiceId = "en-US-male-1"
            )
        }
    }
    
    "Property 3: Backend request is NOT triggered when video is cached" {
        checkAll(10, Arb.string(10..20)) { posterId ->
            // Given: Mock dependencies
            val context = mockk<Context>(relaxed = true)
            val backendFetcher = mockk<BackendVideoFetcher>(relaxed = true)
            val videoCache = mockk<VideoCache>(relaxed = true)
            val videoDecoder = mockk<VideoDecoder>(relaxed = true)
            val lipRenderer = mockk<LipRegionRenderer>(relaxed = true)
            val renderCoordinator = mockk<RenderCoordinator>(relaxed = true)
            val anchor = mockk<Anchor>(relaxed = true)
            
            // Setup cache to return cached video (cache hit)
            val cachedVideo = mockk<com.talkar.app.ar.video.cache.CachedVideo>(relaxed = true)
            every { cachedVideo.videoPath } returns "/cache/video.mp4"
            every { cachedVideo.lipCoordinates } returns LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            coEvery { videoCache.retrieve(posterId) } returns cachedVideo
            
            // Setup video decoder
            every { lipRenderer.getSurface() } returns mockk<Surface>(relaxed = true)
            coEvery { videoDecoder.initialize(any<Uri>(), any()) } returns Result.success(
                VideoInfo(
                    width = 1920,
                    height = 1080,
                    durationMs = 5000,
                    frameRate = 30f,
                    codec = "h264"
                )
            )
            
            // When: Initialize controller with detected poster
            val controller = TalkingPhotoControllerImpl(
                context = context,
                backendFetcher = backendFetcher,
                videoCache = videoCache,
                videoDecoder = videoDecoder,
                lipRenderer = lipRenderer,
                renderCoordinator = renderCoordinator
            )
            
            runBlocking {
                controller.initialize(anchor, posterId)
            }
            
            // Then: Backend request must NOT be triggered (cache hit)
            coVerify(exactly = 0) {
                backendFetcher.generateLipSync(any())
            }
        }
    }
})

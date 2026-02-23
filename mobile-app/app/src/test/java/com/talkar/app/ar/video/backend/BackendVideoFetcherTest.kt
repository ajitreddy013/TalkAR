package com.talkar.app.ar.video.backend

import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.TalkingPhotoRequest
import com.talkar.app.data.api.ApiService
import com.talkar.app.data.api.LipSyncResponse
import com.talkar.app.data.api.TalkingHeadRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import retrofit2.Response
import java.io.File

class BackendVideoFetcherTest {
    
    private lateinit var mockApiService: ApiService
    private lateinit var mockOkHttpClient: OkHttpClient
    private lateinit var fetcher: BackendVideoFetcherImpl
    private lateinit var mockWebServer: MockWebServer
    
    @Before
    fun setup() {
        mockApiService = mock()
        mockOkHttpClient = OkHttpClient()
        fetcher = BackendVideoFetcherImpl(mockApiService, mockOkHttpClient)
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }
    
    @After
    fun teardown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `generateLipSync returns videoId on success`() = runBlocking {
        // Given
        val request = TalkingPhotoRequest(
            posterId = "poster123",
            text = "Hello world",
            voiceId = "en-US-male-1"
        )
        
        val mockResponse = LipSyncResponse(
            success = true,
            videoUrl = "video123",
            status = "processing"
        )
        
        whenever(mockApiService.generateTalkingHeadVideo(any()))
            .thenReturn(Response.success(mockResponse))
        
        // When
        val result = fetcher.generateLipSync(request)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("video123", result.getOrNull())
        
        verify(mockApiService).generateTalkingHeadVideo(
            argThat { 
                imageId == "poster123" && 
                text == "Hello world" && 
                voiceId == "en-US-male-1"
            }
        )
    }
    
    @Test
    fun `generateLipSync returns error on failure`() = runBlocking {
        // Given
        val request = TalkingPhotoRequest(
            posterId = "poster123",
            text = "Hello world"
        )
        
        val mockResponse = LipSyncResponse(
            success = false,
            status = "failed",
            message = "Generation failed"
        )
        
        whenever(mockApiService.generateTalkingHeadVideo(any()))
            .thenReturn(Response.success(mockResponse))
        
        // When
        val result = fetcher.generateLipSync(request)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TalkingPhotoError.GenerationFailed)
    }
    
    @Test
    fun `generateLipSync retries on failure`() = runBlocking {
        // Given
        val request = TalkingPhotoRequest(
            posterId = "poster123",
            text = "Hello world"
        )
        
        val failureResponse = LipSyncResponse(
            success = false,
            status = "failed",
            message = "Temporary error"
        )
        
        val successResponse = LipSyncResponse(
            success = true,
            videoUrl = "video123",
            status = "processing"
        )
        
        // First 2 calls fail, 3rd succeeds
        whenever(mockApiService.generateTalkingHeadVideo(any()))
            .thenReturn(Response.success(failureResponse))
            .thenReturn(Response.success(failureResponse))
            .thenReturn(Response.success(successResponse))
        
        // When
        val result = fetcher.generateLipSync(request)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals("video123", result.getOrNull())
        
        // Verify it was called 3 times (initial + 2 retries)
        verify(mockApiService, times(3)).generateTalkingHeadVideo(any())
    }

    
    @Test
    fun `checkStatus returns status response on success`() = runBlocking {
        // Given
        val videoId = "video123"
        
        val mockResponse = LipSyncResponse(
            success = true,
            videoUrl = "https://example.com/video.mp4",
            status = "complete",
            processingTime = 5000L
        )
        
        whenever(mockApiService.getLipSyncStatus(videoId))
            .thenReturn(Response.success(mockResponse))
        
        // When
        val result = fetcher.checkStatus(videoId)
        
        // Then
        assertTrue(result.isSuccess)
        val statusResponse = result.getOrNull()!!
        assertEquals("video123", statusResponse.videoId)
        assertEquals("complete", statusResponse.status)
        assertEquals("https://example.com/video.mp4", statusResponse.videoUrl)
        assertEquals(1.0f, statusResponse.progress, 0.01f)
    }
    
    @Test
    fun `checkStatus returns error on backend unavailable`() = runBlocking {
        // Given
        val videoId = "video123"
        
        val mockResponse = LipSyncResponse(
            success = false,
            status = "failed",
            message = "Backend unavailable"
        )
        
        whenever(mockApiService.getLipSyncStatus(videoId))
            .thenReturn(Response.success(mockResponse))
        
        // When
        val result = fetcher.checkStatus(videoId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TalkingPhotoError.BackendUnavailable)
    }
    
    @Test
    fun `pollUntilComplete returns when status is complete`() = runBlocking {
        // Given
        val videoId = "video123"
        
        val processingResponse = LipSyncResponse(
            success = true,
            status = "processing"
        )
        
        val completeResponse = LipSyncResponse(
            success = true,
            videoUrl = "https://example.com/video.mp4",
            status = "complete"
        )
        
        // First 2 calls return processing, 3rd returns complete
        whenever(mockApiService.getLipSyncStatus(videoId))
            .thenReturn(Response.success(processingResponse))
            .thenReturn(Response.success(processingResponse))
            .thenReturn(Response.success(completeResponse))
        
        // When
        val result = fetcher.pollUntilComplete(videoId)
        
        // Then
        assertTrue(result.isSuccess)
        val statusResponse = result.getOrNull()!!
        assertEquals("complete", statusResponse.status)
        assertEquals("https://example.com/video.mp4", statusResponse.videoUrl)
        
        // Verify it polled 3 times
        verify(mockApiService, times(3)).getLipSyncStatus(videoId)
    }
    
    @Test
    fun `pollUntilComplete returns error when status is failed`() = runBlocking {
        // Given
        val videoId = "video123"
        
        val failedResponse = LipSyncResponse(
            success = true,
            status = "failed",
            message = "Generation failed"
        )
        
        whenever(mockApiService.getLipSyncStatus(videoId))
            .thenReturn(Response.success(failedResponse))
        
        // When
        val result = fetcher.pollUntilComplete(videoId)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TalkingPhotoError.GenerationFailed)
    }
    
    @Test
    fun `cancel stops ongoing operations`() = runBlocking {
        // Given
        val request = TalkingPhotoRequest(
            posterId = "poster123",
            text = "Hello world"
        )
        
        // When
        fetcher.cancel()
        val result = fetcher.generateLipSync(request)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Operation cancelled", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `downloadVideo saves file and reports progress`() = runBlocking {
        // Given
        val videoUrl = mockWebServer.url("/video.mp4").toString()
        val destinationPath = "${System.getProperty("java.io.tmpdir")}/test_video.mp4"
        val videoContent = "fake video content".toByteArray()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(okio.Buffer().write(videoContent))
                .setHeader("Content-Length", videoContent.size)
        )
        
        val progressUpdates = mutableListOf<Float>()
        
        // When
        val result = fetcher.downloadVideo(videoUrl, destinationPath) { progress ->
            progressUpdates.add(progress)
        }
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(destinationPath, result.getOrNull())
        
        val downloadedFile = File(destinationPath)
        assertTrue(downloadedFile.exists())
        assertArrayEquals(videoContent, downloadedFile.readBytes())
        
        // Verify progress was reported
        assertTrue(progressUpdates.isNotEmpty())
        assertTrue(progressUpdates.last() >= 0.9f) // Should reach near 100%
        
        // Cleanup
        downloadedFile.delete()
    }
    
    @Test
    fun `downloadVideo returns error on HTTP failure`() = runBlocking {
        // Given
        val videoUrl = mockWebServer.url("/video.mp4").toString()
        val destinationPath = "${System.getProperty("java.io.tmpdir")}/test_video.mp4"
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
        )
        
        // When
        val result = fetcher.downloadVideo(videoUrl, destinationPath)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TalkingPhotoError.DownloadFailed)
    }
}

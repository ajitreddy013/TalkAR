package com.talkar.app.data.services

import com.talkar.app.data.api.ApiClient
import com.talkar.app.data.api.AdContentGenerationRequest
import com.talkar.app.data.api.AdContentGenerationResponse
import com.talkar.app.data.api.ApiClient.ApiService
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AdContentGenerationServiceTest {

    @Mock
    private lateinit var mockApiClient: ApiClient.ApiService

    private lateinit var adContentGenerationService: AdContentGenerationService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // Reset singleton instance for testing
        val field = AdContentGenerationService::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
        
        // Use reflection to set the mock apiClient in the singleton instance
        adContentGenerationService = AdContentGenerationService.getInstance()
        val apiClientField = AdContentGenerationService::class.java.getDeclaredField("apiClient")
        apiClientField.isAccessible = true
        apiClientField.set(adContentGenerationService, mockApiClient)
    }

    @Test
    fun `test singleton instance`() {
        val instance1 = AdContentGenerationService.getInstance()
        val instance2 = AdContentGenerationService.getInstance()
        
        assert(instance1 === instance2)
    }

    @Test
    fun `test generateAdContent with successful response`() = runBlocking {
        val productName = "Test Product"
        val mockResponse = Response.success(
            AdContentGenerationResponse(
                success = true,
                script = "Test script",
                audio_url = "https://example.com/audio.mp3",
                video_url = "https://example.com/video.mp4"
            )
        )
        
        `when`(mockApiClient.generateAdContent(any<AdContentGenerationRequest>())).thenReturn(mockResponse)
        
        val result = adContentGenerationService.generateAdContent(productName)
        
        assert(result.isSuccess)
        val response = result.getOrNull()
        assert(response?.success == true)
        assert(response?.script == "Test script")
        assert(response?.audio_url == "https://example.com/audio.mp3")
        assert(response?.video_url == "https://example.com/video.mp4")
        
        verify(mockApiClient).generateAdContent(any<AdContentGenerationRequest>())
    }

    @Test
    fun `test generateAdContentStreaming with successful response`() = runBlocking {
        val productName = "Test Product"
        val mockResponse = Response.success(
            AdContentGenerationResponse(
                success = true,
                script = "Test script",
                audio_url = "https://example.com/audio.mp3",
                video_url = "https://example.com/video.mp4"
            )
        )
        
        `when`(mockApiClient.generateAdContentStreaming(any<AdContentGenerationRequest>())).thenReturn(mockResponse)
        
        val result = adContentGenerationService.generateAdContentStreaming(productName)
        
        assert(result.isSuccess)
        val response = result.getOrNull()
        assert(response?.success == true)
        assert(response?.script == "Test script")
        assert(response?.audio_url == "https://example.com/audio.mp3")
        assert(response?.video_url == "https://example.com/video.mp4")
        
        verify(mockApiClient).generateAdContentStreaming(any<AdContentGenerationRequest>())
    }

    @Test
    fun `test generateAdContent with failed response`() = runBlocking {
        val productName = "Test Product"
        val mockResponse = Response.error<AdContentGenerationResponse>(
            500,
            ResponseBody.create("application/json".toMediaTypeOrNull(), "Internal Server Error")
        )
        
        `when`(mockApiClient.generateAdContent(any<AdContentGenerationRequest>())).thenReturn(mockResponse)
        
        val result = adContentGenerationService.generateAdContent(productName)
        
        assert(result.isFailure)
        
        verify(mockApiClient).generateAdContent(any<AdContentGenerationRequest>())
    }

    @Test
    fun `test generateAdContentStreaming with failed response`() = runBlocking {
        val productName = "Test Product"
        val mockResponse = Response.error<AdContentGenerationResponse>(
            500,
            ResponseBody.create("application/json".toMediaTypeOrNull(), "Internal Server Error")
        )
        
        `when`(mockApiClient.generateAdContentStreaming(any<AdContentGenerationRequest>())).thenReturn(mockResponse)
        
        val result = adContentGenerationService.generateAdContentStreaming(productName)
        
        assert(result.isFailure)
        
        verify(mockApiClient).generateAdContentStreaming(any<AdContentGenerationRequest>())
    }
}
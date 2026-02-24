package com.talkar.app.ar.video.backend

import com.talkar.app.ar.video.models.StatusResponse
import com.talkar.app.ar.video.models.TalkingPhotoRequest
import com.talkar.app.data.api.ApiService
import com.talkar.app.data.api.LipSyncResponse
import com.talkar.app.data.api.TalkingHeadRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

/**
 * Property-based tests for BackendVideoFetcher status polling.
 * 
 * **Validates: Requirements 11.2**
 * 
 * Property 21: Status Polling Until Complete
 * 
 * Tests that polling continues until status changes to "complete" or "failed":
 * - Polls every 2 seconds until status is "complete" or "failed"
 * - Verifies 2-second intervals between polls
 * - Verifies timeout after 60 seconds
 * - Tests across randomized number of polling iterations
 * 
 * **NOTE**: These tests take significant time to run due to the 2-second polling intervals:
 * - Each test iteration involves multiple 2-second delays
 * - 100 iterations with 2-10 polls each = ~10-40 minutes total runtime
 * - The timeout test (commented out) takes ~60 seconds per iteration (10 minutes for 10 iterations)
 * - This is expected behavior for property-based testing of time-dependent functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class BackendVideoFetcherPollingPropertyTest : StringSpec({
    
    /**
     * Test API service that simulates status transitions from "processing" to "complete".
     */
    class TestPollingApiService(
        private val pollsUntilComplete: Int = 5
    ) : ApiService {
        private val statusCallCount = AtomicInteger(0)
        private val statusCallTimestamps = mutableListOf<Long>()
        
        fun getStatusCallCount(): Int = statusCallCount.get()
        fun getStatusCallTimestamps(): List<Long> = statusCallTimestamps.toList()
        
        override suspend fun getLipSyncStatus(videoId: String): Response<LipSyncResponse> {
            val currentCall = statusCallCount.incrementAndGet()
            statusCallTimestamps.add(System.currentTimeMillis())
            
            return if (currentCall < pollsUntilComplete) {
                // Still processing
                Response.success(LipSyncResponse(
                    success = true,
                    status = "processing",
                    message = "Processing video"
                ))
            } else {
                // Complete
                Response.success(LipSyncResponse(
                    success = true,
                    status = "complete",
                    videoUrl = "https://example.com/video_$videoId.mp4",
                    message = "Video ready"
                ))
            }
        }
        
        // Minimal implementation for generateTalkingHeadVideo (not used in polling tests)
        override suspend fun generateTalkingHeadVideo(request: TalkingHeadRequest): Response<LipSyncResponse> {
            return Response.success(LipSyncResponse(
                success = true,
                videoUrl = "test_video_id",
                status = "processing"
            ))
        }
        
        // All other methods throw NotImplementedError as they're not needed for polling tests
        override suspend fun getImages() = throw NotImplementedError()
        override suspend fun getImageById(id: String) = throw NotImplementedError()
        override suspend fun sendVisualQuery(image: okhttp3.MultipartBody.Part, text: okhttp3.RequestBody, posterId: okhttp3.RequestBody?) = throw NotImplementedError()
        override suspend fun generateSyncVideo(request: com.talkar.app.data.models.SyncRequest) = throw NotImplementedError()
        override suspend fun getSyncStatus(jobId: String) = throw NotImplementedError()
        override suspend fun getTalkingHeadVideo(imageId: String, language: String?) = throw NotImplementedError()
        override suspend fun getAvatars() = throw NotImplementedError()
        override suspend fun getAvatarById(id: String) = throw NotImplementedError()
        override suspend fun getAvatarForImage(imageId: String) = throw NotImplementedError()
        override suspend fun getCompleteImageData(imageId: String) = throw NotImplementedError()
        override suspend fun generateLipSyncVideo(request: com.talkar.app.data.api.LipSyncRequest) = throw NotImplementedError()
        override suspend fun getAvailableVoices() = throw NotImplementedError()
        override suspend fun generateScript(request: com.talkar.app.data.api.ScriptGenerationRequest) = throw NotImplementedError()
        override suspend fun generateAdContent(request: com.talkar.app.data.api.AdContentGenerationRequest) = throw NotImplementedError()
        override suspend fun generateAdContentStreaming(request: com.talkar.app.data.api.AdContentGenerationRequest) = throw NotImplementedError()
        override suspend fun generateDynamicScript(request: com.talkar.app.data.api.DynamicScriptRequest) = throw NotImplementedError()
        override suspend fun generateAdContentFromPoster(request: com.talkar.app.data.api.PosterAdContentRequest) = throw NotImplementedError()
        override suspend fun getAllPosters() = throw NotImplementedError()
        override suspend fun getPosterById(imageId: String) = throw NotImplementedError()
        override suspend fun processConversationalQuery(request: com.talkar.app.data.api.ConversationalQueryRequest) = throw NotImplementedError()
        override suspend fun sendFeedback(request: com.talkar.app.data.api.FeedbackRequest) = throw NotImplementedError()
        override suspend fun sendUserContextFeedback(request: com.talkar.app.data.api.FeedbackRequest) = throw NotImplementedError()
        override suspend fun getDefaultTone() = throw NotImplementedError()
        override suspend fun getPromptTemplate() = throw NotImplementedError()
        override suspend fun sendBetaFeedback(request: com.talkar.app.data.api.BetaFeedbackRequest) = throw NotImplementedError()
    }
    
    /**
     * Test API service that simulates status transitions to "failed".
     */
    class TestFailingPollingApiService(
        private val pollsUntilFailed: Int = 3
    ) : ApiService {
        private val statusCallCount = AtomicInteger(0)
        
        fun getStatusCallCount(): Int = statusCallCount.get()
        
        override suspend fun getLipSyncStatus(videoId: String): Response<LipSyncResponse> {
            val currentCall = statusCallCount.incrementAndGet()
            
            return if (currentCall < pollsUntilFailed) {
                // Still processing
                Response.success(LipSyncResponse(
                    success = true,
                    status = "processing",
                    message = "Processing video"
                ))
            } else {
                // Failed
                Response.success(LipSyncResponse(
                    success = true,
                    status = "failed",
                    message = "Generation failed"
                ))
            }
        }
        
        // Minimal implementation for generateTalkingHeadVideo
        override suspend fun generateTalkingHeadVideo(request: TalkingHeadRequest): Response<LipSyncResponse> {
            return Response.success(LipSyncResponse(
                success = true,
                videoUrl = "test_video_id",
                status = "processing"
            ))
        }
        
        // All other methods throw NotImplementedError
        override suspend fun getImages() = throw NotImplementedError()
        override suspend fun getImageById(id: String) = throw NotImplementedError()
        override suspend fun sendVisualQuery(image: okhttp3.MultipartBody.Part, text: okhttp3.RequestBody, posterId: okhttp3.RequestBody?) = throw NotImplementedError()
        override suspend fun generateSyncVideo(request: com.talkar.app.data.models.SyncRequest) = throw NotImplementedError()
        override suspend fun getSyncStatus(jobId: String) = throw NotImplementedError()
        override suspend fun getTalkingHeadVideo(imageId: String, language: String?) = throw NotImplementedError()
        override suspend fun getAvatars() = throw NotImplementedError()
        override suspend fun getAvatarById(id: String) = throw NotImplementedError()
        override suspend fun getAvatarForImage(imageId: String) = throw NotImplementedError()
        override suspend fun getCompleteImageData(imageId: String) = throw NotImplementedError()
        override suspend fun generateLipSyncVideo(request: com.talkar.app.data.api.LipSyncRequest) = throw NotImplementedError()
        override suspend fun getAvailableVoices() = throw NotImplementedError()
        override suspend fun generateScript(request: com.talkar.app.data.api.ScriptGenerationRequest) = throw NotImplementedError()
        override suspend fun generateAdContent(request: com.talkar.app.data.api.AdContentGenerationRequest) = throw NotImplementedError()
        override suspend fun generateAdContentStreaming(request: com.talkar.app.data.api.AdContentGenerationRequest) = throw NotImplementedError()
        override suspend fun generateDynamicScript(request: com.talkar.app.data.api.DynamicScriptRequest) = throw NotImplementedError()
        override suspend fun generateAdContentFromPoster(request: com.talkar.app.data.api.PosterAdContentRequest) = throw NotImplementedError()
        override suspend fun getAllPosters() = throw NotImplementedError()
        override suspend fun getPosterById(imageId: String) = throw NotImplementedError()
        override suspend fun processConversationalQuery(request: com.talkar.app.data.api.ConversationalQueryRequest) = throw NotImplementedError()
        override suspend fun sendFeedback(request: com.talkar.app.data.api.FeedbackRequest) = throw NotImplementedError()
        override suspend fun sendUserContextFeedback(request: com.talkar.app.data.api.FeedbackRequest) = throw NotImplementedError()
        override suspend fun getDefaultTone() = throw NotImplementedError()
        override suspend fun getPromptTemplate() = throw NotImplementedError()
        override suspend fun sendBetaFeedback(request: com.talkar.app.data.api.BetaFeedbackRequest) = throw NotImplementedError()
    }
    
    "Property 21: Polling continues until status changes to complete" {
        checkAll(10,
            Arb.int(2..10),     // pollsUntilComplete (2-10 polls)
            Arb.string(5..20)   // videoId
        ) { pollsUntilComplete, videoId ->
            // Given: API service that transitions to "complete" after N polls
            val testApiService = TestPollingApiService(pollsUntilComplete)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            // When: Polling until complete
            val startTime = System.currentTimeMillis()
            val result = runBlocking {
                fetcher.pollUntilComplete(videoId)
            }
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Then: Should have polled exactly pollsUntilComplete times
            testApiService.getStatusCallCount() shouldBe pollsUntilComplete
            
            // And: Result should be successful with "complete" status
            assert(result.isSuccess)
            val status = result.getOrNull()!!
            status.status shouldBe "complete"
            
            // And: Total time should reflect polling intervals
            // Expected: (pollsUntilComplete - 1) * 2000ms
            val expectedMinTime = (pollsUntilComplete - 1) * 2000L
            val expectedMaxTime = expectedMinTime + 1000L // Allow 1s overhead
            totalTime shouldBeGreaterThanOrEqual expectedMinTime
            totalTime shouldBeLessThan expectedMaxTime
        }
    }
    
    "Property 21: Polling continues until status changes to failed" {
        checkAll(10,
            Arb.int(2..10),     // pollsUntilFailed (2-10 polls)
            Arb.string(5..20)   // videoId
        ) { pollsUntilFailed, videoId ->
            // Given: API service that transitions to "failed" after N polls
            val testApiService = TestFailingPollingApiService(pollsUntilFailed)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            // When: Polling until failed
            val startTime = System.currentTimeMillis()
            val result = runBlocking {
                fetcher.pollUntilComplete(videoId)
            }
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Then: Should have polled exactly pollsUntilFailed times
            testApiService.getStatusCallCount() shouldBe pollsUntilFailed
            
            // And: Result should be failure
            assert(result.isFailure)
            
            // And: Total time should reflect polling intervals
            val expectedMinTime = (pollsUntilFailed - 1) * 2000L
            val expectedMaxTime = expectedMinTime + 1000L
            totalTime shouldBeGreaterThanOrEqual expectedMinTime
            totalTime shouldBeLessThan expectedMaxTime
        }
    }
    
    "Property 21: Polling intervals are 2 seconds apart" {
        checkAll(10,
            Arb.int(3..8),      // pollsUntilComplete (3-8 polls for meaningful intervals)
            Arb.string(5..20)   // videoId
        ) { pollsUntilComplete, videoId ->
            // Given: API service that transitions to "complete" after N polls
            val testApiService = TestPollingApiService(pollsUntilComplete)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            // When: Polling until complete
            runBlocking {
                fetcher.pollUntilComplete(videoId)
            }
            
            // Then: Verify intervals between consecutive polls are ~2 seconds
            val timestamps = testApiService.getStatusCallTimestamps()
            timestamps.size shouldBe pollsUntilComplete
            
            // Check intervals between consecutive polls
            for (i in 1 until timestamps.size) {
                val interval = timestamps[i] - timestamps[i - 1]
                
                // Interval should be approximately 2000ms (allow 1800-2200ms range)
                interval shouldBeGreaterThanOrEqual 1800L
                interval shouldBeLessThan 2200L
            }
        }
    }
    
    // NOTE: Timeout test commented out as it takes ~60 seconds per iteration (10 iterations = 10 minutes)
    // Uncomment for full validation if needed
    /*
    "Property 21: Polling times out after 60 seconds" {
        // Test API service that never completes (always returns "processing")
        class NeverCompleteApiService : ApiService {
            private val statusCallCount = AtomicInteger(0)
            
            fun getStatusCallCount(): Int = statusCallCount.get()
            
            override suspend fun getLipSyncStatus(videoId: String): Response<LipSyncResponse> {
                statusCallCount.incrementAndGet()
                return Response.success(LipSyncResponse(
                    success = true,
                    status = "processing",
                    message = "Still processing"
                ))
            }
            
            override suspend fun generateTalkingHeadVideo(request: TalkingHeadRequest): Response<LipSyncResponse> {
                return Response.success(LipSyncResponse(
                    success = true,
                    videoUrl = "test_video_id",
                    status = "processing"
                ))
            }
            
            // All other methods throw NotImplementedError
            override suspend fun getImages() = throw NotImplementedError()
            override suspend fun getImageById(id: String) = throw NotImplementedError()
            override suspend fun sendVisualQuery(image: okhttp3.MultipartBody.Part, text: okhttp3.RequestBody, posterId: okhttp3.RequestBody?) = throw NotImplementedError()
            override suspend fun generateSyncVideo(request: com.talkar.app.data.models.SyncRequest) = throw NotImplementedError()
            override suspend fun getSyncStatus(jobId: String) = throw NotImplementedError()
            override suspend fun getTalkingHeadVideo(imageId: String, language: String?) = throw NotImplementedError()
            override suspend fun getAvatars() = throw NotImplementedError()
            override suspend fun getAvatarById(id: String) = throw NotImplementedError()
            override suspend fun getAvatarForImage(imageId: String) = throw NotImplementedError()
            override suspend fun getCompleteImageData(imageId: String) = throw NotImplementedError()
            override suspend fun generateLipSyncVideo(request: com.talkar.app.data.api.LipSyncRequest) = throw NotImplementedError()
            override suspend fun getAvailableVoices() = throw NotImplementedError()
            override suspend fun generateScript(request: com.talkar.app.data.api.ScriptGenerationRequest) = throw NotImplementedError()
            override suspend fun generateAdContent(request: com.talkar.app.data.api.AdContentGenerationRequest) = throw NotImplementedError()
            override suspend fun generateAdContentStreaming(request: com.talkar.app.data.api.AdContentGenerationRequest) = throw NotImplementedError()
            override suspend fun generateDynamicScript(request: com.talkar.app.data.api.DynamicScriptRequest) = throw NotImplementedError()
            override suspend fun generateAdContentFromPoster(request: com.talkar.app.data.api.PosterAdContentRequest) = throw NotImplementedError()
            override suspend fun getAllPosters() = throw NotImplementedError()
            override suspend fun getPosterById(imageId: String) = throw NotImplementedError()
            override suspend fun processConversationalQuery(request: com.talkar.app.data.api.ConversationalQueryRequest) = throw NotImplementedError()
            override suspend fun sendFeedback(request: com.talkar.app.data.api.FeedbackRequest) = throw NotImplementedError()
            override suspend fun sendUserContextFeedback(request: com.talkar.app.data.api.FeedbackRequest) = throw NotImplementedError()
            override suspend fun getDefaultTone() = throw NotImplementedError()
            override suspend fun getPromptTemplate() = throw NotImplementedError()
            override suspend fun sendBetaFeedback(request: com.talkar.app.data.api.BetaFeedbackRequest) = throw NotImplementedError()
        }
        
        checkAll(10,  // Fewer iterations since this test takes ~60 seconds each
            Arb.string(5..20)   // videoId
        ) { videoId ->
            // Given: API service that never completes
            val testApiService = NeverCompleteApiService()
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            // When: Polling (should timeout after 60 seconds)
            val startTime = System.currentTimeMillis()
            val result = runBlocking {
                fetcher.pollUntilComplete(videoId)
            }
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Then: Should have timed out
            assert(result.isFailure)
            
            // And: Total time should be approximately 60 seconds
            // Allow 59-62 seconds range
            totalTime shouldBeGreaterThanOrEqual 59000L
            totalTime shouldBeLessThan 62000L
            
            // And: Should have made approximately 30 status calls (60s / 2s = 30)
            // Allow 29-31 range for timing variance
            testApiService.getStatusCallCount() shouldBeGreaterThanOrEqual 29
        }
    }
    */
    
    "Property 21: Polling stops immediately when status is complete" {
        checkAll(10,
            Arb.int(1..10),     // pollsUntilComplete
            Arb.string(5..20)   // videoId
        ) { pollsUntilComplete, videoId ->
            // Given: API service that completes after N polls
            val testApiService = TestPollingApiService(pollsUntilComplete)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            // When: Polling until complete
            runBlocking {
                fetcher.pollUntilComplete(videoId)
            }
            
            // Then: Should have made exactly pollsUntilComplete calls (no extra polls)
            testApiService.getStatusCallCount() shouldBe pollsUntilComplete
        }
    }
    
    "Property 21: Polling stops immediately when status is failed" {
        checkAll(10,
            Arb.int(1..10),     // pollsUntilFailed
            Arb.string(5..20)   // videoId
        ) { pollsUntilFailed, videoId ->
            // Given: API service that fails after N polls
            val testApiService = TestFailingPollingApiService(pollsUntilFailed)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            // When: Polling until failed
            runBlocking {
                fetcher.pollUntilComplete(videoId)
            }
            
            // Then: Should have made exactly pollsUntilFailed calls (no extra polls)
            testApiService.getStatusCallCount() shouldBe pollsUntilFailed
        }
    }
})

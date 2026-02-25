package com.talkar.app.ar.video.backend

import com.talkar.app.ar.video.errors.TalkingPhotoError
import com.talkar.app.ar.video.models.StatusResponse
import com.talkar.app.ar.video.models.TalkingPhotoRequest
import com.talkar.app.data.api.ApiService
import com.talkar.app.data.api.LipSyncResponse
import com.talkar.app.data.api.TalkingHeadRequest
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.robolectric.RobolectricTest
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

/**
 * Property-based tests for BackendVideoFetcher retry logic.
 * 
 * **Validates: Requirements 11.5, 14.3**
 * 
 * Property 22: API Retry with Exponential Backoff
 * 
 * Tests that failed API calls retry exactly 3 times with exponential delays:
 * - Initial attempt + 3 retries = 4 total attempts
 * - Delays between retries: 1s, 2s, 4s (exponential backoff)
 * - Total minimum time: 7 seconds (1s + 2s + 4s)
 * - Verifies retry count and timing across randomized inputs
 */
@RobolectricTest
class BackendVideoFetcherRetryPropertyTest : StringSpec({
    
    /**
     * Test API service that tracks call count and can be configured to fail or succeed.
     */
    class TestApiService(
        private val failureCount: Int = Int.MAX_VALUE,
        private val successVideoId: String = "test_video"
    ) : ApiService {
        private val callCount = AtomicInteger(0)
        
        fun getCallCount(): Int = callCount.get()
        
        override suspend fun generateTalkingHeadVideo(request: TalkingHeadRequest): Response<LipSyncResponse> {
            val currentCall = callCount.incrementAndGet()
            
            return if (currentCall <= failureCount) {
                // Fail
                Response.success(LipSyncResponse(
                    success = false,
                    status = "failed",
                    message = "Simulated failure"
                ))
            } else {
                // Succeed
                Response.success(LipSyncResponse(
                    success = true,
                    videoUrl = successVideoId,
                    status = "processing"
                ))
            }
        }
        
        // All other methods throw NotImplementedError as they're not needed for retry tests
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
        override suspend fun getLipSyncStatus(videoId: String): Response<LipSyncResponse> {
            throw NotImplementedError("Not needed for retry tests")
        }
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
    
    "Property 22: Failed API calls retry exactly 3 times with exponential backoff" {
        checkAll(10,
            Arb.string(5..20),  // posterId
            Arb.string(10..50), // text
            Arb.string(5..15)   // voiceId
        ) { posterId, text, voiceId ->
            // Given: API service that always fails
            val testApiService = TestApiService(failureCount = Int.MAX_VALUE)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            val request = TalkingPhotoRequest(
                posterId = posterId,
                text = text,
                voiceId = voiceId
            )
            
            // When: Calling generateLipSync (which will fail and retry)
            val startTime = System.currentTimeMillis()
            val result = runBlocking {
                fetcher.generateLipSync(request)
            }
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Then: Should have made exactly 4 attempts (initial + 3 retries)
            testApiService.getCallCount() shouldBeExactly 4
            
            // And: Total time should be at least 7 seconds (1s + 2s + 4s)
            // Allow some tolerance for execution overhead
            totalTime shouldBeGreaterThanOrEqual 7000L
            
            // And: Total time should be less than 9 seconds (reasonable upper bound)
            totalTime shouldBeLessThan 9000L
            
            // And: Result should be a failure
            assert(result.isFailure)
        }
    }
    
    "Property 22: Successful retry on 2nd attempt stops further retries" {
        checkAll(10,
            Arb.string(5..20),  // posterId
            Arb.string(10..50), // text
            Arb.int(1..3)       // successOnAttempt (1-3)
        ) { posterId, text, successOnAttempt ->
            // Given: API service that succeeds on a specific attempt
            val testApiService = TestApiService(
                failureCount = successOnAttempt - 1,
                successVideoId = "video_${posterId}"
            )
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            val request = TalkingPhotoRequest(
                posterId = posterId,
                text = text,
                voiceId = "en-US-male-1"
            )
            
            // When: Calling generateLipSync
            val startTime = System.currentTimeMillis()
            val result = runBlocking {
                fetcher.generateLipSync(request)
            }
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Then: Should have made exactly successOnAttempt calls (no more retries after success)
            testApiService.getCallCount() shouldBeExactly successOnAttempt
            
            // And: Result should be successful
            assert(result.isSuccess)
            assert(result.getOrNull() == "video_${posterId}")
            
            // And: Time should be less than if all retries were attempted
            // If succeeded on attempt 1: < 1s
            // If succeeded on attempt 2: >= 1s, < 3s
            // If succeeded on attempt 3: >= 3s, < 7s
            when (successOnAttempt) {
                1 -> totalTime shouldBeLessThan 1000L
                2 -> {
                    totalTime shouldBeGreaterThanOrEqual 1000L
                    totalTime shouldBeLessThan 3000L
                }
                3 -> {
                    totalTime shouldBeGreaterThanOrEqual 3000L
                    totalTime shouldBeLessThan 7000L
                }
            }
        }
    }
    
    "Property 22: Exponential backoff delays double each retry (1s, 2s, 4s)" {
        checkAll(10,
            Arb.string(5..20)  // posterId
        ) { posterId ->
            // Given: API service that always fails
            val testApiService = TestApiService(failureCount = Int.MAX_VALUE)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            val request = TalkingPhotoRequest(
                posterId = posterId,
                text = "Test text",
                voiceId = "test-voice"
            )
            
            // When: Calling generateLipSync and measuring time
            val startTime = System.currentTimeMillis()
            runBlocking {
                fetcher.generateLipSync(request)
            }
            val endTime = System.currentTimeMillis()
            val totalTime = endTime - startTime
            
            // Then: Total time should reflect exponential backoff pattern
            // Expected: 1000ms + 2000ms + 4000ms = 7000ms minimum
            // Allow 2000ms overhead for execution and timing variance
            totalTime shouldBeGreaterThanOrEqual 7000L
            totalTime shouldBeLessThan 9000L
            
            // Verify exactly 4 attempts were made
            testApiService.getCallCount() shouldBeExactly 4
        }
    }
    
    "Property 22: Retry count is exactly 3 for all failed requests" {
        checkAll(10,
            Arb.string(5..20),  // posterId
            Arb.string(10..100) // text (varying lengths)
        ) { posterId, text ->
            // Given: API service that always fails
            val testApiService = TestApiService(failureCount = Int.MAX_VALUE)
            val fetcher = BackendVideoFetcherImpl(testApiService, OkHttpClient())
            
            val request = TalkingPhotoRequest(
                posterId = posterId,
                text = text,
                voiceId = "en-US-female-1"
            )
            
            // When: Calling generateLipSync
            runBlocking {
                fetcher.generateLipSync(request)
            }
            
            // Then: Should have made exactly 4 attempts (initial + 3 retries)
            testApiService.getCallCount() shouldBeExactly 4
        }
    }
})

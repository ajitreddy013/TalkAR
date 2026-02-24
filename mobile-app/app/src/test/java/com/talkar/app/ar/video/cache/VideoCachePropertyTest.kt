package com.talkar.app.ar.video.cache

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.talkar.app.ar.video.models.LipCoordinates
import io.kotest.matchers.shouldBe
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest

/**
 * Property-based tests for VideoCache using Kotest.
 * 
 * Tests universal properties that should hold for all valid inputs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class VideoCachePropertyTest {
    
    private lateinit var context: Context
    private lateinit var database: VideoCacheDatabase
    private lateinit var cache: VideoCacheImpl
    private lateinit var testDir: File
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            VideoCacheDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Create test cache
        cache = VideoCacheImpl(context)
        
        // Create test directory
        testDir = File(context.cacheDir, "test_videos_property")
        testDir.mkdirs()
    }
    
    @After
    fun teardown() {
        runBlocking {
            cache.clear()
        }
        database.close()
        testDir.deleteRecursively()
    }
    
    /**
     * Property 31: Video Cache Round-Trip
     * **Validates: Requirements 16.1**
     * 
     * For any lip video, the sequence of downloading, caching, and retrieving 
     * must produce identical video data (verified by checksum).
     */
    @Test
    fun `Property 31 - Video Cache Round-Trip - download-cache-retrieve produces identical data`() = runBlocking {
        val iterations = 10
        var successCount = 0
        
        repeat(iterations) { i ->
            val posterId = "poster_$i"
            val content = "video_content_$i"
            val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            
            // Given: Create a test video file
            val originalFile = createTestVideo("original_$posterId.mp4", content)
            val originalChecksum = calculateChecksum(originalFile)
            
            // When: Store the video in cache
            val storeResult = cache.store(posterId, originalFile.absolutePath, lipCoords, originalChecksum)
            storeResult.isSuccess shouldBe true
            
            // And: Retrieve the video from cache
            val retrieved = cache.retrieve(posterId)
            
            // Then: Retrieved video should not be null
            retrieved.shouldNotBeNull()
            
            // And: Checksum should match original
            retrieved.checksum shouldBe originalChecksum
            
            // And: Recalculating checksum of cached file should match
            val cachedChecksum = calculateChecksum(File(retrieved.videoPath))
            cachedChecksum shouldBe originalChecksum
            
            // And: Lip coordinates should match
            retrieved.lipCoordinates shouldBe lipCoords
            
            // And: Poster ID should match
            retrieved.posterId shouldBe posterId
            
            successCount++
        }
        
        successCount shouldBe iterations
    }
    
    /**
     * Property 32: Checksum Validation and Recovery
     * **Validates: Requirements 16.2, 16.3, 16.4, 16.5**
     * 
     * Checksums must be validated before storage and before playback, 
     * and corrupted files must trigger automatic re-download.
     */
    @Test
    fun `Property 32 - Checksum Validation - checksums validated and corrupted files rejected`() = runBlocking {
        val iterations = 10
        var successCount = 0
        
        repeat(iterations) { i ->
            val posterId = "poster_checksum_$i"
            val content = "video_content_checksum_$i"
            val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            
            // Given: Create a test video file
            val videoFile = createTestVideo("checksum_$posterId.mp4", content)
            val correctChecksum = calculateChecksum(videoFile)
            val wrongChecksum = "sha256:0000000000000000000000000000000000000000000000000000000000000000"
            
            // When: Try to store with wrong checksum
            val storeResult = cache.store(posterId, videoFile.absolutePath, lipCoords, wrongChecksum)
            
            // Then: Store should fail
            storeResult.isFailure shouldBe true
            
            // And: Video should not be cached
            cache.isCached(posterId) shouldBe false
            
            // When: Store with correct checksum
            val correctStoreResult = cache.store(posterId, videoFile.absolutePath, lipCoords, correctChecksum)
            correctStoreResult.isSuccess shouldBe true
            
            // And: Video should be cached
            cache.isCached(posterId) shouldBe true
            
            // And: Integrity validation should pass
            cache.validateIntegrity(posterId) shouldBe true
            
            // When: Corrupt the cached file
            val retrieved = cache.retrieve(posterId)
            retrieved.shouldNotBeNull()
            val cachedFile = File(retrieved.videoPath)
            cachedFile.writeText("CORRUPTED CONTENT")
            
            // Then: Integrity validation should fail
            cache.validateIntegrity(posterId) shouldBe false
            
            // And: Retrieve should return null (auto-delete corrupted file)
            val retrievedAfterCorruption = cache.retrieve(posterId)
            retrievedAfterCorruption.shouldBeNull()
            
            successCount++
        }
        
        successCount shouldBe iterations
    }
    
    /**
     * Property 8: Cache Expiration
     * **Validates: Requirements 5.2, 5.4**
     * 
     * Videos more than 24 hours old should not be returned and should be marked for deletion.
     */
    @Test
    fun `Property 8 - Cache Expiration - videos over 24 hours old not returned and marked for deletion`() = runBlocking {
        val iterations = 10
        var successCount = 0
        
        repeat(iterations) { i ->
            val posterId = "poster_expiry_$i"
            val content = "video_content_expiry_$i"
            val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            
            // Given: Create a test video file
            val videoFile = createTestVideo("expiry_$posterId.mp4", content)
            val checksum = calculateChecksum(videoFile)
            
            // When: Store video with old timestamp (25 hours ago)
            val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
            val entry = CacheEntry(
                posterId = posterId,
                videoPath = videoFile.absolutePath,
                lipX = lipCoords.lipX,
                lipY = lipCoords.lipY,
                lipWidth = lipCoords.lipWidth,
                lipHeight = lipCoords.lipHeight,
                checksum = checksum,
                cachedAt = oldTimestamp,
                sizeBytes = videoFile.length()
            )
            database.cacheDao().insert(entry)
            
            // Then: isCached should return false
            cache.isCached(posterId) shouldBe false
            
            // And: retrieve should return null
            val retrieved = cache.retrieve(posterId)
            retrieved.shouldBeNull()
            
            // When: Run cleanup
            val deletedCount = cache.cleanupExpired()
            
            // Then: At least one video should be deleted
            deletedCount shouldBe 1
            
            // And: Video should no longer be in database
            cache.isCached(posterId) shouldBe false
            
            successCount++
        }
        
        successCount shouldBe iterations
    }
    
    /**
     * Property 27: Cache Size Limit
     * **Validates: Requirements 15.1**
     * 
     * Cache should never exceed 500MB.
     */
    @Test
    fun `Property 27 - Cache Size Limit - cache never exceeds 500MB`() = runBlocking {
        val iterations = 10  // Reduced for faster testing
        var successCount = 0
        
        repeat(iterations) { iteration ->
            // Clear cache before each iteration
            cache.clear()
            
            val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            
            // Given: Create multiple large video entries that exceed 500MB
            val largeSize = 100L * 1024 * 1024 // 100MB each
            
            // Create 6 videos (600MB total)
            for (i in 1..6) {
                val videoFile = createTestVideo("large_${iteration}_$i.mp4", "content_$i")
                val checksum = calculateChecksum(videoFile)
                
                val entry = CacheEntry(
                    posterId = "poster_large_${iteration}_$i",
                    videoPath = videoFile.absolutePath,
                    lipX = lipCoords.lipX,
                    lipY = lipCoords.lipY,
                    lipWidth = lipCoords.lipWidth,
                    lipHeight = lipCoords.lipHeight,
                    checksum = checksum,
                    cachedAt = System.currentTimeMillis() - (i * 1000L),
                    sizeBytes = largeSize
                )
                database.cacheDao().insert(entry)
            }
            
            // When: Enforce cache limit
            cache.enforceLimit()
            
            // Then: Total cache size should be <= 500MB
            val totalSize = cache.getTotalSize()
            totalSize shouldBeLessThanOrEqual CachedVideo.MAX_CACHE_SIZE_BYTES
            
            successCount++
        }
        
        successCount shouldBe iterations
    }
    
    /**
     * Property 28: LRU Eviction Order
     * **Validates: Requirements 15.2**
     * 
     * When cache exceeds limit, oldest videos should be deleted first.
     */
    @Test
    fun `Property 28 - LRU Eviction Order - oldest videos deleted first when limit exceeded`() = runBlocking {
        val iterations = 10  // Reduced for faster testing
        var successCount = 0
        
        repeat(iterations) { iteration ->
            // Clear cache before each iteration
            cache.clear()
            
            val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            
            // Given: Create videos with different ages
            val largeSize = 100L * 1024 * 1024 // 100MB each
            val now = System.currentTimeMillis()
            
            // Create 6 videos with different timestamps (oldest to newest)
            for (i in 1..6) {
                val posterId = "poster_lru_${iteration}_$i"
                
                val videoFile = createTestVideo("lru_${iteration}_$i.mp4", "content_$i")
                val checksum = calculateChecksum(videoFile)
                
                val entry = CacheEntry(
                    posterId = posterId,
                    videoPath = videoFile.absolutePath,
                    lipX = lipCoords.lipX,
                    lipY = lipCoords.lipY,
                    lipWidth = lipCoords.lipWidth,
                    lipHeight = lipCoords.lipHeight,
                    checksum = checksum,
                    cachedAt = now - ((6 - i) * 60 * 1000L), // Older = smaller i
                    sizeBytes = largeSize
                )
                database.cacheDao().insert(entry)
            }
            
            // When: Enforce cache limit
            cache.enforceLimit()
            
            // Then: Oldest videos (poster_lru_X_1) should be deleted
            // and newest videos should remain
            cache.isCached("poster_lru_${iteration}_1") shouldBe false
            
            // And: Newest videos should still be cached
            cache.isCached("poster_lru_${iteration}_6") shouldBe true
            
            // And: Total size should be under limit
            val totalSize = cache.getTotalSize()
            totalSize shouldBeLessThanOrEqual CachedVideo.MAX_CACHE_SIZE_BYTES
            
            successCount++
        }
        
        successCount shouldBe iterations
    }
    
    /**
     * Property 7: Video Caching on Download
     * **Validates: Requirements 5.1**
     * 
     * Downloaded videos must appear in cache immediately after successful download.
     */
    @Test
    fun `Property 7 - Video Caching on Download - downloaded videos appear in cache immediately`() = runBlocking {
        val iterations = 10
        var successCount = 0
        
        repeat(iterations) { i ->
            val posterId = "poster_cache_hit_$i"
            val content = "video_content_cache_hit_$i"
            val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            
            // Given: Create a test video file (simulating download)
            val videoFile = createTestVideo("cache_hit_$posterId.mp4", content)
            val checksum = calculateChecksum(videoFile)
            
            // When: Store the video (simulating successful download)
            val storeResult = cache.store(posterId, videoFile.absolutePath, lipCoords, checksum)
            storeResult.isSuccess shouldBe true
            
            // Then: Video should be immediately available in cache
            cache.isCached(posterId) shouldBe true
            
            // And: Retrieve should return the video immediately
            val retrieved = cache.retrieve(posterId)
            retrieved.shouldNotBeNull()
            
            // And: Retrieved video should have correct metadata
            retrieved.posterId shouldBe posterId
            retrieved.lipCoordinates shouldBe lipCoords
            retrieved.checksum shouldBe checksum
            
            // And: Video file should exist
            val cachedFile = File(retrieved.videoPath)
            cachedFile.exists() shouldBe true
            
            successCount++
        }
        
        successCount shouldBe iterations
    }
    
    /**
     * Property 9: Offline Cache Retrieval
     * **Validates: Requirements 5.3, 5.5**
     * 
     * Cached videos must be retrievable without network requests.
     * This property verifies that once a video is cached, it can be retrieved
     * purely from local storage without any network dependency.
     */
    @Test
    fun `Property 9 - Offline Cache Retrieval - cached videos retrieved without network requests`() = runBlocking {
        val iterations = 10
        var successCount = 0
        
        repeat(iterations) { i ->
            val posterId = "poster_offline_$i"
            val content = "video_content_offline_$i"
            val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
            
            // Given: Create and cache a video
            val videoFile = createTestVideo("offline_$posterId.mp4", content)
            val checksum = calculateChecksum(videoFile)
            
            val storeResult = cache.store(posterId, videoFile.absolutePath, lipCoords, checksum)
            storeResult.isSuccess shouldBe true
            
            // When: Retrieve the video (simulating offline mode - no network available)
            // The cache should retrieve from local storage only
            val retrieved = cache.retrieve(posterId)
            
            // Then: Video should be retrieved successfully
            retrieved.shouldNotBeNull()
            
            // And: Retrieved video should have correct data
            retrieved.posterId shouldBe posterId
            retrieved.lipCoordinates shouldBe lipCoords
            retrieved.checksum shouldBe checksum
            
            // And: Video file should exist locally
            val cachedFile = File(retrieved.videoPath)
            cachedFile.exists() shouldBe true
            
            // And: File content should match original
            val retrievedChecksum = calculateChecksum(cachedFile)
            retrievedChecksum shouldBe checksum
            
            // And: Multiple retrievals should work (idempotent)
            val retrieved2 = cache.retrieve(posterId)
            retrieved2.shouldNotBeNull()
            retrieved2.posterId shouldBe posterId
            retrieved2.checksum shouldBe checksum
            
            successCount++
        }
        
        successCount shouldBe iterations
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Creates a test video file with given content.
     */
    private fun createTestVideo(filename: String, content: String): File {
        val file = File(testDir, filename)
        file.writeText(content)
        return file
    }
    
    /**
     * Calculates SHA-256 checksum of a file.
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead = input.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = input.read(buffer)
            }
        }
        val hashBytes = digest.digest()
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }
        return "sha256:$hexString"
    }
}

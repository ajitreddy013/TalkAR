package com.talkar.app.ar.video.cache

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.talkar.app.ar.video.models.LipCoordinates
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class VideoCacheTest {
    
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
        
        // Create test cache with in-memory database
        cache = VideoCacheImpl(context)
        
        // Create test directory
        testDir = File(context.cacheDir, "test_videos")
        testDir.mkdirs()
    }
    
    @After
    fun teardown() {
        database.close()
        testDir.deleteRecursively()
    }
    
    @Test
    fun `store saves video with metadata`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test1.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        // When
        val result = cache.store("poster1", testVideo.absolutePath, lipCoords, checksum)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(cache.isCached("poster1"))
    }
    
    @Test
    fun `store validates checksum before storing`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test2.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val wrongChecksum = "sha256:wrong"
        
        // When
        val result = cache.store("poster2", testVideo.absolutePath, lipCoords, wrongChecksum)
        
        // Then
        assertTrue(result.isFailure)
        assertFalse(cache.isCached("poster2"))
    }
    
    @Test
    fun `retrieve returns cached video if not expired`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test3.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        cache.store("poster3", testVideo.absolutePath, lipCoords, checksum)
        
        // When
        val retrieved = cache.retrieve("poster3")
        
        // Then
        assertNotNull(retrieved)
        assertEquals("poster3", retrieved!!.posterId)
        assertEquals(lipCoords, retrieved.lipCoordinates)
        assertEquals(checksum, retrieved.checksum)
    }
    
    @Test
    fun `retrieve returns null for expired video`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test4.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        // Store with old timestamp (25 hours ago)
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        val entry = CacheEntry(
            posterId = "poster4",
            videoPath = testVideo.absolutePath,
            lipX = lipCoords.lipX,
            lipY = lipCoords.lipY,
            lipWidth = lipCoords.lipWidth,
            lipHeight = lipCoords.lipHeight,
            checksum = checksum,
            cachedAt = oldTimestamp,
            sizeBytes = testVideo.length()
        )
        database.cacheDao().insert(entry)
        
        // When
        val retrieved = cache.retrieve("poster4")
        
        // Then
        assertNull(retrieved)
    }

    
    @Test
    fun `retrieve returns null for corrupted video`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test5.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        cache.store("poster5", testVideo.absolutePath, lipCoords, checksum)
        
        // Corrupt the video file
        val cachedEntry = database.cacheDao().get("poster5")!!
        File(cachedEntry.videoPath).writeText("corrupted content")
        
        // When
        val retrieved = cache.retrieve("poster5")
        
        // Then
        assertNull(retrieved)
    }
    
    @Test
    fun `isCached returns true for valid cached video`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test6.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        cache.store("poster6", testVideo.absolutePath, lipCoords, checksum)
        
        // When
        val isCached = cache.isCached("poster6")
        
        // Then
        assertTrue(isCached)
    }
    
    @Test
    fun `isCached returns false for expired video`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test7.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        // Store with old timestamp
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        val entry = CacheEntry(
            posterId = "poster7",
            videoPath = testVideo.absolutePath,
            lipX = lipCoords.lipX,
            lipY = lipCoords.lipY,
            lipWidth = lipCoords.lipWidth,
            lipHeight = lipCoords.lipHeight,
            checksum = checksum,
            cachedAt = oldTimestamp,
            sizeBytes = testVideo.length()
        )
        database.cacheDao().insert(entry)
        
        // When
        val isCached = cache.isCached("poster7")
        
        // Then
        assertFalse(isCached)
    }
    
    @Test
    fun `validateIntegrity returns true for valid video`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test8.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        cache.store("poster8", testVideo.absolutePath, lipCoords, checksum)
        
        // When
        val isValid = cache.validateIntegrity("poster8")
        
        // Then
        assertTrue(isValid)
    }
    
    @Test
    fun `validateIntegrity returns false for corrupted video`() = runBlocking {
        // Given
        val testVideo = createTestVideo("test9.mp4", "test content")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum = calculateTestChecksum(testVideo)
        
        cache.store("poster9", testVideo.absolutePath, lipCoords, checksum)
        
        // Corrupt the video
        val cachedEntry = database.cacheDao().get("poster9")!!
        File(cachedEntry.videoPath).writeText("corrupted")
        
        // When
        val isValid = cache.validateIntegrity("poster9")
        
        // Then
        assertFalse(isValid)
    }
    
    @Test
    fun `cleanupExpired deletes old videos`() = runBlocking {
        // Given
        val testVideo1 = createTestVideo("test10.mp4", "content1")
        val testVideo2 = createTestVideo("test11.mp4", "content2")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum1 = calculateTestChecksum(testVideo1)
        val checksum2 = calculateTestChecksum(testVideo2)
        
        // Store one old video and one recent video
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        val entry1 = CacheEntry(
            posterId = "poster10",
            videoPath = testVideo1.absolutePath,
            lipX = lipCoords.lipX,
            lipY = lipCoords.lipY,
            lipWidth = lipCoords.lipWidth,
            lipHeight = lipCoords.lipHeight,
            checksum = checksum1,
            cachedAt = oldTimestamp,
            sizeBytes = testVideo1.length()
        )
        database.cacheDao().insert(entry1)
        
        cache.store("poster11", testVideo2.absolutePath, lipCoords, checksum2)
        
        // When
        val deletedCount = cache.cleanupExpired()
        
        // Then
        assertEquals(1, deletedCount)
        assertFalse(cache.isCached("poster10"))
        assertTrue(cache.isCached("poster11"))
    }
    
    @Test
    fun `enforceLimit deletes oldest videos when over 500MB`() = runBlocking {
        // Given - Create multiple large entries that exceed 500MB
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        
        // Create 3 videos with fake large sizes (100MB each)
        val largeSize = 100L * 1024 * 1024 // 100MB
        
        for (i in 1..6) {
            val testVideo = createTestVideo("large$i.mp4", "content$i")
            val checksum = calculateTestChecksum(testVideo)
            
            val entry = CacheEntry(
                posterId = "poster_large_$i",
                videoPath = testVideo.absolutePath,
                lipX = lipCoords.lipX,
                lipY = lipCoords.lipY,
                lipWidth = lipCoords.lipWidth,
                lipHeight = lipCoords.lipHeight,
                checksum = checksum,
                cachedAt = System.currentTimeMillis() - (i * 1000L), // Older = smaller i
                sizeBytes = largeSize
            )
            database.cacheDao().insert(entry)
        }
        
        // When
        val deletedCount = cache.enforceLimit()
        
        // Then
        assertTrue(deletedCount > 0)
        val totalSize = cache.getTotalSize()
        assertTrue(totalSize <= CachedVideo.MAX_CACHE_SIZE_BYTES)
    }
    
    @Test
    fun `clear removes all cached videos`() = runBlocking {
        // Given
        val testVideo1 = createTestVideo("test12.mp4", "content1")
        val testVideo2 = createTestVideo("test13.mp4", "content2")
        val lipCoords = LipCoordinates(0.4f, 0.5f, 0.2f, 0.1f)
        val checksum1 = calculateTestChecksum(testVideo1)
        val checksum2 = calculateTestChecksum(testVideo2)
        
        cache.store("poster12", testVideo1.absolutePath, lipCoords, checksum1)
        cache.store("poster13", testVideo2.absolutePath, lipCoords, checksum2)
        
        // When
        cache.clear()
        
        // Then
        assertFalse(cache.isCached("poster12"))
        assertFalse(cache.isCached("poster13"))
        assertEquals(0L, cache.getTotalSize())
    }
    
    // Helper methods
    
    private fun createTestVideo(filename: String, content: String): File {
        val file = File(testDir, filename)
        file.writeText(content)
        return file
    }
    
    private fun calculateTestChecksum(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
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

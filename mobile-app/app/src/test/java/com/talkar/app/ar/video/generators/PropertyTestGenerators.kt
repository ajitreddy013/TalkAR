package com.talkar.app.ar.video.generators

import com.talkar.app.ar.video.models.LipCoordinates
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.string

/**
 * Custom Kotest property test generators for talking photo domain types.
 * 
 * These generators create random but valid test data for property-based testing.
 */
object PropertyTestGenerators {
    
    /**
     * Generates valid normalized coordinates in 0-1 range.
     * 
     * Used for testing lip coordinates and other normalized values.
     */
    fun normalizedFloat(): Arb<Float> = Arb.float(min = 0f, max = 1f)
    
    /**
     * Generates valid LipCoordinates with all values in 0-1 range.
     * 
     * Property: All coordinate values must be in 0-1 range
     */
    fun lipCoordinates(): Arb<LipCoordinates> = arbitrary {
        LipCoordinates(
            lipX = normalizedFloat().bind(),
            lipY = normalizedFloat().bind(),
            lipWidth = normalizedFloat().bind(),
            lipHeight = normalizedFloat().bind()
        )
    }
    
    /**
     * Generates invalid LipCoordinates with at least one value outside 0-1 range.
     * 
     * Used for testing validation logic.
     */
    fun invalidLipCoordinates(): Arb<Map<String, Float>> = arbitrary {
        val invalidValue = Arb.float(min = -10f, max = 10f)
            .filter { it < 0f || it > 1f }
            .bind()
        
        mapOf(
            "lipX" to invalidValue,
            "lipY" to normalizedFloat().bind(),
            "lipWidth" to normalizedFloat().bind(),
            "lipHeight" to normalizedFloat().bind()
        )
    }
    
    /**
     * Generates poster dimensions in pixels.
     * 
     * Typical poster sizes: 1080x1920, 720x1280, etc.
     */
    fun posterDimensions(): Arb<Pair<Int, Int>> = arbitrary {
        val width = Arb.int(min = 480, max = 2160).bind()
        val height = Arb.int(min = 640, max = 3840).bind()
        Pair(width, height)
    }
    
    /**
     * Generates video dimensions in pixels.
     * 
     * Typical video sizes: 1920x1080, 1280x720, etc.
     */
    fun videoDimensions(): Arb<Pair<Int, Int>> = arbitrary {
        val width = Arb.int(min = 320, max = 1920).bind()
        val height = Arb.int(min = 240, max = 1080).bind()
        Pair(width, height)
    }
    
    /**
     * Generates frame rates in fps.
     * 
     * Valid range: 24-60 fps
     */
    fun frameRate(): Arb<Int> = Arb.int(min = 24, max = 60)
    
    /**
     * Generates video duration in milliseconds.
     * 
     * Range: 1-30 seconds
     */
    fun videoDuration(): Arb<Long> = Arb.long(min = 1000L, max = 30000L)
    
    /**
     * Generates file size in bytes.
     * 
     * Range: 100KB - 10MB
     */
    fun fileSize(): Arb<Long> = Arb.long(min = 100_000L, max = 10_000_000L)
    
    /**
     * Generates cache size in bytes.
     * 
     * Range: 0 - 500MB (cache limit)
     */
    fun cacheSize(): Arb<Long> = Arb.long(min = 0L, max = 500_000_000L)
    
    /**
     * Generates timestamp in milliseconds.
     * 
     * Range: Current time ± 30 days
     */
    fun timestamp(): Arb<Long> = arbitrary {
        val now = System.currentTimeMillis()
        val offset = Arb.long(min = -30 * 24 * 60 * 60 * 1000L, max = 30 * 24 * 60 * 60 * 1000L).bind()
        now + offset
    }
    
    /**
     * Generates poster ID.
     * 
     * Format: "poster-{random}"
     */
    fun posterId(): Arb<String> = arbitrary {
        "poster-${Arb.string(minSize = 6, maxSize = 12).bind()}"
    }
    
    /**
     * Generates video ID.
     * 
     * Format: "video-{random}"
     */
    fun videoId(): Arb<String> = arbitrary {
        "video-${Arb.string(minSize = 6, maxSize = 12).bind()}"
    }
    
    /**
     * Generates SHA-256 checksum.
     * 
     * Format: 64 hex characters
     */
    fun checksum(): Arb<String> = arbitrary {
        val hexChars = "0123456789abcdef"
        (1..64).map { hexChars.random() }.joinToString("")
    }
    
    /**
     * Generates feather radius in pixels.
     * 
     * Valid range: 5-10 pixels (as per requirements)
     */
    fun featherRadius(): Arb<Int> = Arb.int(min = 5, max = 10)
    
    /**
     * Generates alpha value for blending.
     * 
     * Range: 0.0 - 1.0
     */
    fun alphaValue(): Arb<Float> = Arb.float(min = 0f, max = 1f)
    
    /**
     * Generates tracking latency in milliseconds.
     * 
     * Target: <16ms per frame (60fps)
     */
    fun trackingLatency(): Arb<Long> = Arb.long(min = 1L, max = 16L)
    
    /**
     * Generates cache retrieval time in milliseconds.
     * 
     * Target: <100ms
     */
    fun cacheRetrievalTime(): Arb<Long> = Arb.long(min = 1L, max = 100L)
    
    /**
     * Generates poster detection time in milliseconds.
     * 
     * Target: <2000ms (2 seconds)
     */
    fun detectionTime(): Arb<Long> = Arb.long(min = 100L, max = 2000L)
    
    /**
     * Generates retry delay in milliseconds.
     * 
     * Exponential backoff: 1000, 2000, 4000
     */
    fun retryDelay(): Arb<Long> = arbitrary {
        val attempt = Arb.int(min = 1, max = 3).bind()
        1000L * (1 shl (attempt - 1)) // 2^(attempt-1) * 1000
    }
    
    /**
     * Generates HTTP status code.
     * 
     * Common codes: 200, 400, 404, 429, 500, 503
     */
    fun httpStatusCode(): Arb<Int> = arbitrary {
        listOf(200, 400, 404, 429, 500, 503).random()
    }
    
    /**
     * Generates error status code.
     * 
     * Range: 400-599 (client and server errors)
     */
    fun errorStatusCode(): Arb<Int> = Arb.int(min = 400, max = 599)
    
    /**
     * Generates success status code.
     * 
     * Range: 200-299
     */
    fun successStatusCode(): Arb<Int> = Arb.int(min = 200, max = 299)
}

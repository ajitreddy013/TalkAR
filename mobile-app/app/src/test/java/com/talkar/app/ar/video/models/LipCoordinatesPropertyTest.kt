package com.talkar.app.ar.video.models

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

/**
 * Property-based tests for LipCoordinates data class.
 * 
 * **Validates: Requirements 3.3, 4.1, 4.3, 4.5**
 * 
 * Property 5: Normalized Coordinate Completeness and Range
 * - All coordinate values (lipX, lipY, lipWidth, lipHeight) must be present
 * - Each coordinate value must be in the range 0-1
 * - Invalid coordinates (outside 0-1 range) must throw IllegalArgumentException
 */
class LipCoordinatesPropertyTest : StringSpec({
    
    "Property 5: Valid normalized coordinates (0-1 range) are accepted" {
        checkAll(100, 
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f)
        ) { lipX, lipY, lipWidth, lipHeight ->
            // When creating LipCoordinates with valid values in 0-1 range
            val coordinates = LipCoordinates(lipX, lipY, lipWidth, lipHeight)
            
            // Then all values should be stored correctly
            coordinates.lipX shouldBe lipX
            coordinates.lipY shouldBe lipY
            coordinates.lipWidth shouldBe lipWidth
            coordinates.lipHeight shouldBe lipHeight
            
            // And all values should be in valid range
            (coordinates.lipX >= 0f) shouldBe true
            (coordinates.lipX <= 1f) shouldBe true
            (coordinates.lipY >= 0f) shouldBe true
            (coordinates.lipY <= 1f) shouldBe true
            (coordinates.lipWidth >= 0f) shouldBe true
            (coordinates.lipWidth <= 1f) shouldBe true
            (coordinates.lipHeight >= 0f) shouldBe true
            (coordinates.lipHeight <= 1f) shouldBe true
        }
    }
    
    "Property 5: Invalid lipX (< 0) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(-1000f, -0.001f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f)
        ) { invalidLipX, lipY, lipWidth, lipHeight ->
            // When creating LipCoordinates with lipX < 0
            val result = runCatching {
                LipCoordinates(invalidLipX, lipY, lipWidth, lipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Invalid lipX (> 1) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(1.001f, 1000f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f)
        ) { invalidLipX, lipY, lipWidth, lipHeight ->
            // When creating LipCoordinates with lipX > 1
            val result = runCatching {
                LipCoordinates(invalidLipX, lipY, lipWidth, lipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Invalid lipY (< 0) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(-1000f, -0.001f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f)
        ) { lipX, invalidLipY, lipWidth, lipHeight ->
            // When creating LipCoordinates with lipY < 0
            val result = runCatching {
                LipCoordinates(lipX, invalidLipY, lipWidth, lipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Invalid lipY (> 1) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(1.001f, 1000f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f)
        ) { lipX, invalidLipY, lipWidth, lipHeight ->
            // When creating LipCoordinates with lipY > 1
            val result = runCatching {
                LipCoordinates(lipX, invalidLipY, lipWidth, lipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Invalid lipWidth (< 0) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(-1000f, -0.001f),
            Arb.float(0f, 1f)
        ) { lipX, lipY, invalidLipWidth, lipHeight ->
            // When creating LipCoordinates with lipWidth < 0
            val result = runCatching {
                LipCoordinates(lipX, lipY, invalidLipWidth, lipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Invalid lipWidth (> 1) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(1.001f, 1000f),
            Arb.float(0f, 1f)
        ) { lipX, lipY, invalidLipWidth, lipHeight ->
            // When creating LipCoordinates with lipWidth > 1
            val result = runCatching {
                LipCoordinates(lipX, lipY, invalidLipWidth, lipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Invalid lipHeight (< 0) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(-1000f, -0.001f)
        ) { lipX, lipY, lipWidth, invalidLipHeight ->
            // When creating LipCoordinates with lipHeight < 0
            val result = runCatching {
                LipCoordinates(lipX, lipY, lipWidth, invalidLipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Invalid lipHeight (> 1) throws IllegalArgumentException" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(1.001f, 1000f)
        ) { lipX, lipY, lipWidth, invalidLipHeight ->
            // When creating LipCoordinates with lipHeight > 1
            val result = runCatching {
                LipCoordinates(lipX, lipY, lipWidth, invalidLipHeight)
            }
            
            // Then it should throw IllegalArgumentException
            result.isFailure shouldBe true
            result.exceptionOrNull().shouldBeInstanceOf<IllegalArgumentException>()
        }
    }
    
    "Property 5: Coordinate completeness - all four values must be present" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f)
        ) { lipX, lipY, lipWidth, lipHeight ->
            // When creating LipCoordinates with all four values
            val coordinates = LipCoordinates(lipX, lipY, lipWidth, lipHeight)
            
            // Then all four values must be accessible and non-null
            coordinates.lipX shouldBe lipX
            coordinates.lipY shouldBe lipY
            coordinates.lipWidth shouldBe lipWidth
            coordinates.lipHeight shouldBe lipHeight
        }
    }
    
    "Property 5: Boundary values (0.0 and 1.0) are valid" {
        // Test all combinations of boundary values
        val boundaryValues = listOf(0f, 1f)
        
        for (lipX in boundaryValues) {
            for (lipY in boundaryValues) {
                for (lipWidth in boundaryValues) {
                    for (lipHeight in boundaryValues) {
                        // When creating LipCoordinates with boundary values
                        val coordinates = LipCoordinates(lipX, lipY, lipWidth, lipHeight)
                        
                        // Then they should be accepted
                        coordinates.lipX shouldBe lipX
                        coordinates.lipY shouldBe lipY
                        coordinates.lipWidth shouldBe lipWidth
                        coordinates.lipHeight shouldBe lipHeight
                    }
                }
            }
        }
    }
    
    "Property 5: Coordinate to pixel conversion maintains range validity" {
        checkAll(100,
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.float(0f, 1f),
            Arb.int(100, 4000),
            Arb.int(100, 4000)
        ) { lipX, lipY, lipWidth, lipHeight, posterWidth, posterHeight ->
            // When converting normalized coordinates to pixel coordinates
            val coordinates = LipCoordinates(lipX, lipY, lipWidth, lipHeight)
            val pixelRect = coordinates.toPixelCoordinates(posterWidth, posterHeight)
            
            // Then pixel coordinates should be within poster bounds
            (pixelRect.left >= 0) shouldBe true
            (pixelRect.top >= 0) shouldBe true
            (pixelRect.right <= posterWidth) shouldBe true
            (pixelRect.bottom <= posterHeight) shouldBe true
        }
    }
})

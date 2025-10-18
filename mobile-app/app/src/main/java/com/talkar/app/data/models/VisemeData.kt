package com.talkar.app.data.models

/**
 * Viseme Data Model - Phoneme Timing for Lip-Sync
 * 
 * Represents phoneme timing information extracted from Sync API
 * or generated for mock/demo animations.
 */
data class VisemeData(
    /**
     * List of viseme (mouth shape) keyframes
     */
    val visemes: List<Viseme>,
    
    /**
     * Total duration of the audio in seconds
     */
    val totalDuration: Float,
    
    /**
     * Audio file URL or path
     */
    val audioUrl: String? = null,
    
    /**
     * Source of viseme data
     */
    val source: VisemeSource = VisemeSource.MOCK
)

/**
 * Individual viseme (mouth shape) keyframe
 */
data class Viseme(
    /**
     * Phoneme/viseme type (A, I, U, E, O, etc.)
     */
    val phoneme: Phoneme,
    
    /**
     * Start time in seconds
     */
    val startTime: Float,
    
    /**
     * End time in seconds
     */
    val endTime: Float,
    
    /**
     * Blend shape weight (0.0 - 1.0)
     */
    val weight: Float = 1.0f
) {
    /**
     * Duration of this viseme
     */
    val duration: Float
        get() = endTime - startTime
}

/**
 * Phoneme types for mouth shapes
 * 
 * Based on standard viseme mapping:
 * - Vowels: A, I, U, E, O
 * - Consonants: M, F, TH, S, etc.
 * - Neutral: Resting/closed mouth
 */
enum class Phoneme(
    val displayName: String,
    val blendShapeIndex: Int
) {
    // Vowels (primary mouth shapes)
    NEUTRAL("Neutral", 0),       // Resting mouth
    A("A (ah)", 1),              // Open mouth, jaw down
    E("E (eh)", 2),              // Slightly open, corners wide
    I("I (ee)", 3),              // Wide smile, teeth visible
    O("O (oh)", 4),              // Rounded lips
    U("U (oo)", 5),              // Pursed lips
    
    // Consonants (secondary shapes)
    M("M/B/P", 6),               // Lips closed
    F("F/V", 7),                 // Lower lip to upper teeth
    TH("TH", 8),                 // Tongue between teeth
    S("S/Z", 9),                 // Teeth together, lips apart
    L("L", 10),                  // Tongue to roof
    R("R", 11),                  // Lips slightly rounded
    
    // Special
    SILENCE("Silence", 0);       // Same as neutral
    
    companion object {
        /**
         * Get phoneme from character
         */
        fun fromChar(char: Char): Phoneme {
            return when (char.uppercaseChar()) {
                'A' -> A
                'E' -> E
                'I' -> I
                'O' -> O
                'U' -> U
                'M', 'B', 'P' -> M
                'F', 'V' -> F
                'S', 'Z' -> S
                'L' -> L
                'R' -> R
                else -> NEUTRAL
            }
        }
        
        /**
         * Get phoneme from API viseme ID
         */
        fun fromVisemeId(id: Int): Phoneme {
            return entries.find { it.blendShapeIndex == id } ?: NEUTRAL
        }
    }
}

/**
 * Source of viseme data
 */
enum class VisemeSource {
    /**
     * Real viseme data from Sync API
     */
    API,
    
    /**
     * Mock/generated viseme data for testing
     */
    MOCK,
    
    /**
     * Auto-generated from text analysis
     */
    GENERATED
}

/**
 * Viseme Data Builder - Generate mock viseme data
 */
object VisemeDataBuilder {
    
    /**
     * Generate mock viseme data based on audio duration
     * Creates simple open-close animation synced to audio
     */
    fun generateMockVisemes(
        duration: Float,
        text: String? = null,
        audioUrl: String? = null
    ): VisemeData {
        val visemes = mutableListOf<Viseme>()
        
        if (text != null && text.isNotEmpty()) {
            // Generate visemes from text
            visemes.addAll(generateFromText(text, duration))
        } else {
            // Generate simple open-close pattern
            visemes.addAll(generateSimplePattern(duration))
        }
        
        return VisemeData(
            visemes = visemes,
            totalDuration = duration,
            audioUrl = audioUrl,
            source = VisemeSource.MOCK
        )
    }
    
    /**
     * Generate visemes from text
     * Simple phoneme extraction from vowels
     */
    private fun generateFromText(text: String, totalDuration: Float): List<Viseme> {
        val words = text.split(" ")
        val visemes = mutableListOf<Viseme>()
        
        // Estimate time per word
        val timePerWord = totalDuration / words.size.toFloat()
        
        words.forEachIndexed { index, word ->
            val startTime = index * timePerWord
            val endTime = startTime + timePerWord
            
            // Extract vowels from word
            val vowels = word.filter { it.lowercaseChar() in "aeiou" }
            
            if (vowels.isNotEmpty()) {
                // Time per vowel
                val timePerVowel = timePerWord / vowels.length
                
                vowels.forEachIndexed { vIndex, vowel ->
                    val vStart = startTime + (vIndex * timePerVowel)
                    val vEnd = vStart + timePerVowel
                    
                    visemes.add(
                        Viseme(
                            phoneme = Phoneme.fromChar(vowel),
                            startTime = vStart,
                            endTime = vEnd,
                            weight = 0.8f
                        )
                    )
                }
            } else {
                // No vowels, use neutral
                visemes.add(
                    Viseme(
                        phoneme = Phoneme.NEUTRAL,
                        startTime = startTime,
                        endTime = endTime,
                        weight = 0.3f
                    )
                )
            }
        }
        
        return visemes
    }
    
    /**
     * Generate simple open-close pattern
     * Alternates between open (A) and closed (neutral)
     */
    private fun generateSimplePattern(duration: Float): List<Viseme> {
        val visemes = mutableListOf<Viseme>()
        val syllableRate = 4.0f // 4 syllables per second (natural speech rate)
        val syllableDuration = 1.0f / syllableRate
        
        var currentTime = 0f
        var isOpen = false
        
        while (currentTime < duration) {
            val endTime = (currentTime + syllableDuration).coerceAtMost(duration)
            
            visemes.add(
                Viseme(
                    phoneme = if (isOpen) Phoneme.A else Phoneme.NEUTRAL,
                    startTime = currentTime,
                    endTime = endTime,
                    weight = if (isOpen) 0.7f else 0.2f
                )
            )
            
            currentTime = endTime
            isOpen = !isOpen
        }
        
        return visemes
    }
    
    /**
     * Parse viseme data from Sync API response
     * Format: { "visemes": [{"phoneme": "A", "start": 0.0, "end": 0.5}], "duration": 3.0 }
     */
    fun parseFromApi(jsonResponse: String, audioUrl: String?): VisemeData? {
        try {
            // TODO: Implement actual JSON parsing when API format is available
            // For now, return null (will fallback to mock data)
            return null
        } catch (e: Exception) {
            android.util.Log.e("VisemeDataBuilder", "Failed to parse API visemes", e)
            return null
        }
    }
}

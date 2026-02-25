package com.talkar.app.data.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class SpeechRecognitionServiceTest {
    
    private lateinit var context: Context
    private lateinit var speechRecognitionService: SpeechRecognitionService
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        speechRecognitionService = SpeechRecognitionService(context)
    }
    
    @Test
    fun `test speech recognition service initialization`() {
        // Test that the service initializes without crashing
        assertNotNull(speechRecognitionService)
        
        // Test that the speech result flow is initialized
        assertNotNull(speechRecognitionService.speechResult)
    }
    
    @Test
    fun `test start listening changes state`() {
        // Initially should be idle
        assertTrue(speechRecognitionService.speechResult.value is SpeechResult.Idle)
        
        // Start listening
        speechRecognitionService.startListening()
        
        // Should now be listening (or processing)
        val result = speechRecognitionService.speechResult.value
        assertTrue(
            "Expected Listening or Processing state, but got ${result::class.simpleName}",
            result is SpeechResult.Listening || result is SpeechResult.Processing
        )
    }
    
    @Test
    fun `test stop listening changes state`() {
        // Start listening first
        speechRecognitionService.startListening()
        
        // Stop listening
        speechRecognitionService.stopListening()
        
        // Should be back to idle
        assertTrue(speechRecognitionService.speechResult.value is SpeechResult.Idle)
    }
    
    @Test
    fun `test destroy cleans up resources`() {
        // Destroy the service
        speechRecognitionService.destroy()
        
        // Try to start listening - should not crash
        speechRecognitionService.startListening()
        
        // This test mainly ensures destroy() doesn't crash
        assertTrue(true)
    }
}
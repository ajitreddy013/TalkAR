package com.talkar.app.data.services

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.talkar.app.data.models.ConversationalResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class ConversationalContextServiceTest {
    
    private lateinit var context: Context
    private lateinit var conversationalContextService: ConversationalContextService
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        conversationalContextService = ConversationalContextService(context)
    }
    
    @Test
    fun `test conversational context service initialization`() {
        // Test that the service initializes without crashing
        assertNotNull(conversationalContextService)
    }
    
    @Test
    fun `test process query with simple input`() = runBlocking {
        // Test processing a simple query
        val response = conversationalContextService.processQuery("Hello", "test-image-id")
        
        // Should return a response
        assertNotNull("Response should not be null", response)
        assertTrue("Response should be successful", response?.success ?: false)
        assertNotNull("Response text should not be null", response?.text)
    }
    
    @Test
    fun `test process query with what is this question`() = runBlocking {
        // Test processing a "what is this" query
        val response = conversationalContextService.processQuery("What is this?", "test-image-id")
        
        // Should return a response
        assertNotNull("Response should not be null", response)
        assertTrue("Response should be successful", response?.success ?: false)
        assertNotNull("Response text should not be null", response?.text)
        assertTrue("Response should mention the image", response?.text?.contains("image") ?: false)
    }
    
    @Test
    fun `test process query with thanks`() = runBlocking {
        // Test processing a "thank you" query
        val response = conversationalContextService.processQuery("Thank you", null)
        
        // Should return a response
        assertNotNull("Response should not be null", response)
        assertTrue("Response should be successful", response?.success ?: false)
        assertNotNull("Response text should not be null", response?.text)
        assertTrue("Response should be polite", response?.text?.contains("welcome") ?: false)
    }
    
    @Test
    fun `test process voice input`() {
        // Test processing voice input
        val result = conversationalContextService.processVoiceInput()
        
        // Should return a speech result (initially idle)
        assertNotNull("Speech result should not be null", result)
        assertTrue("Should be idle initially", result is SpeechResult.Idle)
    }
    
    @Test
    fun `test start and stop voice listening`() {
        // Test starting voice listening
        conversationalContextService.startVoiceListening()
        
        // Test stopping voice listening
        conversationalContextService.stopVoiceListening()
        
        // These methods should not crash
        assertTrue(true)
    }
    
    @Test
    fun `test destroy cleans up resources`() {
        // Destroy the service
        conversationalContextService.destroy()
        
        // This test mainly ensures destroy() doesn't crash
        assertTrue(true)
    }
}
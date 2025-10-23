package com.talkar.app.data.services

import android.util.Log
import com.talkar.app.TalkARApplication
import kotlinx.coroutines.runBlocking

class AdContentGenerationTest {
    
    private val tag = "AdContentGenerationTest"
    
    fun testAdContentGeneration() {
        runBlocking {
            try {
                Log.d(tag, "Testing ad content generation...")
                
                val service = AdContentGenerationService.getInstance()
                val result = service.generateAdContent("iPhone")
                
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    Log.d(tag, "Ad content generation successful:")
                    Log.d(tag, "Success: ${response?.success}")
                    Log.d(tag, "Script: ${response?.script}")
                    Log.d(tag, "Audio URL: ${response?.audio_url}")
                    Log.d(tag, "Video URL: ${response?.video_url}")
                } else {
                    Log.e(tag, "Ad content generation failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in ad content generation test", e)
            }
        }
    }
}
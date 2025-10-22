const axios = require("axios");

// Comprehensive test suite for TTS functionality
async function testTTSComprehensive() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Running comprehensive TTS tests...\n");

    // Test 1: Basic audio generation
    console.log("1. Testing basic audio generation...");
    const audioResponse1 = await axios.post(`${baseUrl}/generate_audio`, {
      text: "Welcome to our product showcase.",
      language: "en",
      emotion: "neutral",
    });
    console.log("✓ Basic audio generation successful");
    console.log("  Audio URL:", audioResponse1.data.audioUrl);
    console.log("  Duration:", audioResponse1.data.duration, "seconds\n");

    // Test 2: Audio generation with happy emotion
    console.log("2. Testing audio generation with happy emotion...");
    const audioResponse2 = await axios.post(`${baseUrl}/generate_audio`, {
      text: "I'm so excited to show you our amazing products!",
      language: "en",
      emotion: "happy",
    });
    console.log("✓ Happy emotion audio generation successful");
    console.log("  Audio URL:", audioResponse2.data.audioUrl);
    console.log("  Duration:", audioResponse2.data.duration, "seconds\n");

    // Test 3: Audio generation with serious emotion
    console.log("3. Testing audio generation with serious emotion...");
    const audioResponse3 = await axios.post(`${baseUrl}/generate_audio`, {
      text: "Please pay attention to this important information.",
      language: "en",
      emotion: "serious",
    });
    console.log("✓ Serious emotion audio generation successful");
    console.log("  Audio URL:", audioResponse3.data.audioUrl);
    console.log("  Duration:", audioResponse3.data.duration, "seconds\n");

    // Test 4: Audio generation with Spanish language
    console.log("4. Testing audio generation with Spanish language...");
    const audioResponse4 = await axios.post(`${baseUrl}/generate_audio`, {
      text: "Bienvenido a nuestra exhibición de productos.",
      language: "es",
      emotion: "neutral",
    });
    console.log("✓ Spanish language audio generation successful");
    console.log("  Audio URL:", audioResponse4.data.audioUrl);
    console.log("  Duration:", audioResponse4.data.duration, "seconds\n");

    // Test 5: Audio generation with long text
    console.log("5. Testing audio generation with longer text...");
    const audioResponse5 = await axios.post(`${baseUrl}/generate_audio`, {
      text: "This is a longer text to test the audio generation capabilities of our system. It should generate a longer audio file with appropriate duration estimation.",
      language: "en",
      emotion: "neutral",
    });
    console.log("✓ Long text audio generation successful");
    console.log("  Audio URL:", audioResponse5.data.audioUrl);
    console.log("  Duration:", audioResponse5.data.duration, "seconds\n");

    // Test 6: Error handling - missing text
    console.log("6. Testing error handling for missing text...");
    try {
      await axios.post(`${baseUrl}/generate_audio`, {
        language: "en",
        emotion: "neutral",
      });
      console.log("✗ Expected error but request succeeded");
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log("✓ Error handling for missing text works correctly");
        console.log("  Error message:", error.response.data.error, "\n");
      } else {
        console.log("✗ Unexpected error:", error.message);
      }
    }

    // Test 7: Error handling - empty text
    console.log("7. Testing error handling for empty text...");
    try {
      await axios.post(`${baseUrl}/generate_audio`, {
        text: "",
        language: "en",
        emotion: "neutral",
      });
      console.log("✗ Expected error but request succeeded");
    } catch (error) {
      if (error.response && error.response.status === 400) {
        console.log("✓ Error handling for empty text works correctly");
        console.log("  Error message:", error.response.data.error, "\n");
      } else {
        console.log("✗ Unexpected error:", error.message);
      }
    }

    // Test 8: Caching test - same request should be faster
    console.log("8. Testing caching mechanism...");
    const startTime = Date.now();
    const audioResponse8a = await axios.post(`${baseUrl}/generate_audio`, {
      text: "This is a test for caching.",
      language: "en",
      emotion: "neutral",
    });
    const firstRequestTime = Date.now() - startTime;

    const startTime2 = Date.now();
    const audioResponse8b = await axios.post(`${baseUrl}/generate_audio`, {
      text: "This is a test for caching.",
      language: "en",
      emotion: "neutral",
    });
    const secondRequestTime = Date.now() - startTime2;

    console.log("✓ Caching test completed");
    console.log("  First request time:", firstRequestTime, "ms");
    console.log("  Second request time:", secondRequestTime, "ms");
    console.log(
      "  Audio URLs match:",
      audioResponse8a.data.audioUrl === audioResponse8b.data.audioUrl,
      "\n"
    );

    console.log("All comprehensive TTS tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the comprehensive test
testTTSComprehensive();

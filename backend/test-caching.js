const axios = require("axios");

// Test caching in the AI pipeline
async function testCaching() {
  const baseUrl = "http://localhost:3000/api/v1/ai-pipeline";

  try {
    console.log("Testing caching in AI Pipeline...\n");

    // First request - should not be cached
    console.log("1. First request for script generation...");
    const firstResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("First response:", firstResponse.data.script);

    // Second request with same parameters - should be cached
    console.log(
      "\n2. Second request with same parameters (should be cached)..."
    );
    const secondResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "happy",
    });
    console.log("Second response:", secondResponse.data.script);

    // Third request with different parameters - should not be cached
    console.log(
      "\n3. Third request with different parameters (should not be cached)..."
    );
    const thirdResponse = await axios.post(`${baseUrl}/generate_script`, {
      imageId: "test-image-123",
      language: "en",
      emotion: "serious",
    });
    console.log("Third response:", thirdResponse.data.script);

    // Test audio generation caching
    console.log("\n4. Testing audio generation caching...");
    const audioText = "Welcome to our exhibition!";

    // First audio request
    console.log("First audio request...");
    const firstAudioResponse = await axios.post(`${baseUrl}/generate_audio`, {
      text: audioText,
      language: "en",
      emotion: "happy",
    });
    console.log("First audio response:", firstAudioResponse.data.audioUrl);

    // Second audio request with same text
    console.log("Second audio request with same text (should be cached)...");
    const secondAudioResponse = await axios.post(`${baseUrl}/generate_audio`, {
      text: audioText,
      language: "en",
      emotion: "happy",
    });
    console.log("Second audio response:", secondAudioResponse.data.audioUrl);

    console.log("\nAll caching tests completed successfully!");
  } catch (error) {
    console.error("Test failed:", error.response?.data || error.message);
  }
}

// Run the test
testCaching();
